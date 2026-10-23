package io.github.autocoupons.domain.coupon

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs one coupon session against a checkout:
 *
 * 1. fetch coupons → 2. read the baseline cart (removing any pre-applied code) →
 * 3. for each coupon: apply, observe, record, remove, confirm the cart is back to baseline →
 * 4. rank verified savings → 5. re-apply the winner and confirm the same saving appears.
 *
 * Every emitted state corresponds to a completed real operation; nothing is simulated here.
 * Cancelling the collector stops after the current step and restores the cart (best effort,
 * bounded by [cleanupTimeoutMs]).
 */
class CouponApplicationCoordinator(
    private val repository: CouponRepository,
    private val rankingEngine: CouponRankingEngine,
    private val applyTimeoutMs: Long = 12_000,
    private val readTimeoutMs: Long = 6_000,
    private val cleanupTimeoutMs: Long = 6_000,
) {

    fun run(retailerId: String, source: CouponSource, adapter: CheckoutAdapter): Flow<CouponSessionState> = flow {
        var state = CouponSessionState(retailerId = retailerId, source = source, phase = SessionPhase.FETCHING)
        emit(state)

        val coupons = when (val fetched = repository.fetchCoupons(retailerId, source)) {
            is CouponFetchResult.Failure -> {
                emit(state.copy(phase = SessionPhase.FAILED, error = SessionError.Fetch(fetched.error)))
                return@flow
            }
            is CouponFetchResult.Success -> fetched.coupons.distinctBy { it.code.trim().uppercase() }
        }
        if (coupons.isEmpty()) {
            emit(state.copy(phase = SessionPhase.FAILED, error = SessionError.NoCoupons))
            return@flow
        }

        state = state.copy(phase = SessionPhase.READING_CART, results = coupons.map { CouponTestResult(it) })
        emit(state)

        val session = Session(adapter)
        try {
            val baseline = session.establishBaseline()
            state = state.copy(phase = SessionPhase.TESTING, baseline = baseline, originalCode = session.originalCode)
            emit(state)

            for (index in state.results.indices) {
                currentCoroutineContext().ensureActive()
                state = state.update(index) { it.transitionTo(CouponTestStatus.TESTING) }.copy(currentIndex = index)
                emit(state)

                val tested = session.test(state.results[index], baseline)
                state = state.update(index) { tested }
                if (tested.status == CouponTestStatus.APPLIED_AND_VERIFIED) {
                    state = state.copy(
                        lastVerified = tested,
                        bestSoFar = rankingEngine.rank(state.results, baseline).winner,
                    )
                }
                emit(state)
            }

            state = state.copy(currentIndex = null)
            val decision = rankingEngine.rank(state.results, baseline)
            val winner = decision.winner
            if (winner == null) {
                session.restoreOriginalCode()
                emit(state.copy(phase = SessionPhase.COMPLETED, outcome = SessionOutcome.NoVerifiedSavings))
                return@flow
            }

            state = state.copy(phase = SessionPhase.REAPPLYING, bestSoFar = winner)
            emit(state)
            val outcome = session.reapply(winner, baseline)
            emit(state.copy(phase = SessionPhase.COMPLETED, outcome = outcome))
        } catch (e: SessionAbort) {
            session.restoreBestEffort()
            emitFailure(state, e.error)
        } finally {
            // Runs on cancellation too: never leave a half-tested coupon applied to the cart.
            if (session.cartModified) {
                withContext(NonCancellable) { session.restoreBestEffort() }
            }
        }
    }

    private suspend fun FlowCollector<CouponSessionState>.emitFailure(state: CouponSessionState, error: SessionError) {
        val results = state.results.map {
            when {
                it.status == CouponTestStatus.TESTING -> it.transitionTo(
                    CouponTestStatus.FAILED,
                    failureReason = if (error == SessionError.CartChanged) FailureReason.CART_CHANGED
                    else FailureReason.PAGE_UNAVAILABLE,
                )
                it.status.isTerminal -> it
                else -> it.transitionTo(CouponTestStatus.CANCELLED, failureReason = FailureReason.CANCELLED)
            }
        }
        emit(state.copy(phase = SessionPhase.FAILED, results = results, currentIndex = null, error = error))
    }

    private class SessionAbort(val error: SessionError) : Exception()

    /** Mutable per-run bookkeeping kept out of the emitted state. */
    private inner class Session(private val adapter: CheckoutAdapter) {
        var originalCode: String? = null
            private set

        /**
         * True while the cart differs from what the shopper left it as and the session has not
         * deliberately settled it (winner applied or original code restored).
         */
        var cartModified = false
            private set

        suspend fun establishBaseline(): CartSnapshot {
            var cart = read()
            val existing = cart.appliedCode
            if (existing != null) {
                originalCode = existing
                cartModified = true
                cart = remove()
                if (cart.appliedCode != null) throw SessionAbort(SessionError.CleanupFailed)
            }
            if (!cart.isInternallyConsistent() || cart.discount.minor != 0L || !cart.subtotal.isPositive()) {
                throw SessionAbort(SessionError.CheckoutUnavailable("Cart totals could not be read reliably"))
            }
            return cart
        }

        suspend fun test(result: CouponTestResult, baseline: CartSnapshot): CouponTestResult {
            val before = read()
            if (before.fingerprint != baseline.fingerprint || before.subtotal != baseline.subtotal) {
                throw SessionAbort(SessionError.CartChanged)
            }
            if (before.appliedCode != null) removeAndConfirm(baseline)

            val code = result.coupon.code
            cartModified = true
            val observation = apply(code)
            val tested = when (observation) {
                ApplyObservation.TimedOut -> result.transitionTo(
                    CouponTestStatus.FAILED,
                    failureReason = FailureReason.TIMED_OUT,
                )
                is ApplyObservation.Rejected -> {
                    if (observation.cart.fingerprint != baseline.fingerprint) throw SessionAbort(SessionError.CartChanged)
                    result.transitionTo(
                        CouponTestStatus.REJECTED,
                        retailerMessage = observation.message,
                        failureReason = FailureReason.REJECTED_BY_RETAILER,
                    )
                }
                is ApplyObservation.Accepted -> {
                    if (observation.cart.fingerprint != baseline.fingerprint) throw SessionAbort(SessionError.CartChanged)
                    verifyAcceptance(result, code, observation, baseline)
                }
            }

            val after = read()
            if (after.appliedCode != null) removeAndConfirm(baseline)
            else if (after.total != baseline.total) throw SessionAbort(SessionError.CleanupFailed)
            cartModified = originalCode != null
            return tested
        }

        suspend fun reapply(winner: CouponTestResult, baseline: CartSnapshot): SessionOutcome {
            val before = read()
            if (before.fingerprint != baseline.fingerprint) throw SessionAbort(SessionError.CartChanged)
            if (before.appliedCode != null) removeAndConfirm(baseline)

            cartModified = true
            val observation = apply(winner.coupon.code)
            if (observation !is ApplyObservation.Accepted) {
                val reason = when (observation) {
                    ApplyObservation.TimedOut -> "The store did not respond when re-applying ${winner.coupon.code}"
                    is ApplyObservation.Rejected ->
                        "The store rejected ${winner.coupon.code} on re-apply" +
                            (observation.message?.let { ": $it" } ?: "")
                    else -> error("unreachable")
                }
                restoreBestEffort()
                return SessionOutcome.WinnerNotConfirmed(winner, reason, finalCart = null)
            }
            val cart = observation.cart
            val savings = baseline.subtotal - cart.total
            val matches = cart.isInternallyConsistent() &&
                cart.fingerprint == baseline.fingerprint &&
                cart.appliedCode.equals(winner.coupon.code, ignoreCase = true) &&
                savings == winner.observedSavings
            // The winner is intentionally left applied: that is the purpose of the session.
            cartModified = false
            return if (matches) {
                SessionOutcome.WinnerVerified(winner, cart)
            } else {
                SessionOutcome.WinnerNotConfirmed(
                    winner,
                    "Re-applying ${winner.coupon.code} showed a saving of ${savings.format()} " +
                        "instead of ${winner.observedSavings?.format()}",
                    finalCart = cart,
                )
            }
        }

        /** Puts back the shopper's own coupon (if any) once testing found nothing better. */
        suspend fun restoreOriginalCode() {
            val code = originalCode
            if (code != null) {
                withTimeoutOrNull(applyTimeoutMs) { runCatchingCheckout { adapter.applyCoupon(code) } }
            }
            cartModified = false
        }

        suspend fun restoreBestEffort() {
            withTimeoutOrNull(cleanupTimeoutMs) {
                runCatchingCheckout {
                    val cart = adapter.readCart()
                    if (cart.appliedCode != null) adapter.removeCoupon()
                }
            }
            restoreOriginalCode()
        }

        private fun verifyAcceptance(
            result: CouponTestResult,
            code: String,
            observation: ApplyObservation.Accepted,
            baseline: CartSnapshot,
        ): CouponTestResult {
            val cart = observation.cart
            val savings = baseline.subtotal - cart.total
            val unverified = { reason: FailureReason ->
                result.transitionTo(
                    CouponTestStatus.UNVERIFIED,
                    observedSavings = savings.takeIf { it.isPositive() },
                    observedTotal = cart.total,
                    retailerMessage = observation.message,
                    failureReason = reason,
                )
            }
            return when {
                !cart.isInternallyConsistent() || cart.discount != savings -> unverified(FailureReason.INCONSISTENT_TOTALS)
                !cart.appliedCode.equals(code, ignoreCase = true) -> unverified(FailureReason.INCONSISTENT_TOTALS)
                !savings.isPositive() -> unverified(FailureReason.NO_OBSERVABLE_DISCOUNT)
                else -> result.transitionTo(
                    CouponTestStatus.APPLIED_AND_VERIFIED,
                    observedSavings = savings,
                    observedTotal = cart.total,
                    retailerMessage = observation.message,
                    failureReason = null,
                )
            }
        }

        private suspend fun removeAndConfirm(baseline: CartSnapshot) {
            val cart = remove()
            if (cart.appliedCode != null || cart.total != baseline.total) throw SessionAbort(SessionError.CleanupFailed)
        }

        private suspend fun read(): CartSnapshot =
            withTimeoutOrNull(readTimeoutMs) { checkout { adapter.readCart() } }
                ?: throw SessionAbort(SessionError.CheckoutUnavailable("Timed out reading the cart"))

        private suspend fun remove(): CartSnapshot =
            withTimeoutOrNull(applyTimeoutMs) { checkout { adapter.removeCoupon() } }
                ?: throw SessionAbort(SessionError.CleanupFailed)

        private suspend fun apply(code: String): ApplyObservation =
            withTimeoutOrNull(applyTimeoutMs) { checkout { adapter.applyCoupon(code) } } ?: ApplyObservation.TimedOut

        private inline fun <T> checkout(block: () -> T): T =
            try {
                block()
            } catch (e: CheckoutUnavailableException) {
                throw SessionAbort(SessionError.CheckoutUnavailable(e.message ?: "Checkout unavailable"))
            }

        private inline fun runCatchingCheckout(block: () -> Unit) {
            try {
                block()
            } catch (_: CheckoutUnavailableException) {
                // Restoration is best effort; the session result already reports the failure.
            }
        }
    }
}

private inline fun CouponSessionState.update(
    index: Int,
    transform: (CouponTestResult) -> CouponTestResult,
): CouponSessionState = copy(results = results.toMutableList().also { it[index] = transform(it[index]) })
