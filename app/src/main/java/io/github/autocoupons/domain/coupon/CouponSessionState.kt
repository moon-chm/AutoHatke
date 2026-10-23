package io.github.autocoupons.domain.coupon

enum class SessionPhase {
    FETCHING,
    READING_CART,
    TESTING,
    REAPPLYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isRunning: Boolean get() = this == FETCHING || this == READING_CART || this == TESTING || this == REAPPLYING
}

sealed interface SessionError {
    data class Fetch(val error: CouponFetchError) : SessionError
    data object NoCoupons : SessionError
    data class CheckoutUnavailable(val detail: String) : SessionError
    data object CartChanged : SessionError
    /** A test coupon could not be removed, so further results would not be comparable. */
    data object CleanupFailed : SessionError
}

sealed interface SessionOutcome {
    /** The winner was applied again and the cart showed the same saving. */
    data class WinnerVerified(val winner: CouponTestResult, val finalCart: CartSnapshot) : SessionOutcome

    /** A winner was found, but re-applying it did not reproduce the saving. Not a confirmed result. */
    data class WinnerNotConfirmed(val winner: CouponTestResult, val reason: String, val finalCart: CartSnapshot?) :
        SessionOutcome

    data object NoVerifiedSavings : SessionOutcome
}

/** Immutable snapshot of a coupon session, emitted after every real step. */
data class CouponSessionState(
    val retailerId: String,
    val source: CouponSource,
    val phase: SessionPhase,
    val results: List<CouponTestResult> = emptyList(),
    val currentIndex: Int? = null,
    val baseline: CartSnapshot? = null,
    /** A code the shopper had applied before the session; it is restored if nothing better is found. */
    val originalCode: String? = null,
    val lastVerified: CouponTestResult? = null,
    val bestSoFar: CouponTestResult? = null,
    val outcome: SessionOutcome? = null,
    val error: SessionError? = null,
) {
    val total: Int get() = results.size

    /** Number of coupons whose test has actually finished. Drives the progress bar. */
    val completed: Int get() = results.count { it.status.isTerminal && it.status != CouponTestStatus.CANCELLED }

    val progress: Float get() = if (total == 0) 0f else completed.toFloat() / total

    val current: CouponTestResult? get() = currentIndex?.let { results.getOrNull(it) }

    /** Marks every unfinished coupon as cancelled; used when the user stops the session. */
    fun cancelled(): CouponSessionState = copy(
        phase = SessionPhase.CANCELLED,
        currentIndex = null,
        results = results.map {
            if (it.status.isTerminal) it
            else it.transitionTo(CouponTestStatus.CANCELLED, failureReason = FailureReason.CANCELLED)
        },
    )
}
