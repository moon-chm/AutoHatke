package io.github.autocoupons.domain.coupon

/**
 * Lifecycle of a single coupon inside a session:
 *
 * DISCOVERED → TESTING → APPLIED_AND_VERIFIED | REJECTED | FAILED | UNVERIFIED
 * DISCOVERED | TESTING → CANCELLED
 *
 * Only APPLIED_AND_VERIFIED coupons are eligible to win.
 */
enum class CouponTestStatus {
    DISCOVERED,
    TESTING,
    APPLIED_AND_VERIFIED,
    REJECTED,
    FAILED,
    UNVERIFIED,
    CANCELLED;

    val isTerminal: Boolean get() = this != DISCOVERED && this != TESTING

    fun canTransitionTo(next: CouponTestStatus): Boolean = when (this) {
        DISCOVERED -> next == TESTING || next == CANCELLED
        TESTING -> next.isTerminal
        else -> false
    }
}

class IllegalCouponTransition(from: CouponTestStatus, to: CouponTestStatus) :
    IllegalStateException("Coupon cannot move from $from to $to")

/** Why a coupon did not produce a verified saving. */
enum class FailureReason(val userMessage: String) {
    REJECTED_BY_RETAILER("Not accepted by the store"),
    TIMED_OUT("The store did not respond in time"),
    NO_OBSERVABLE_DISCOUNT("Accepted, but no discount was visible in the cart"),
    INCONSISTENT_TOTALS("Cart totals did not add up after applying"),
    CART_CHANGED("The cart changed during testing"),
    PAGE_UNAVAILABLE("The checkout page was not available"),
    CANCELLED("Testing was cancelled"),
}

/** The current, immutable record for one coupon in a session. */
data class CouponTestResult(
    val coupon: Coupon,
    val status: CouponTestStatus = CouponTestStatus.DISCOVERED,
    /** Savings observed in the cart. Set only for APPLIED_AND_VERIFIED or UNVERIFIED. */
    val observedSavings: Money? = null,
    val observedTotal: Money? = null,
    /** Message the retailer displayed, if any (e.g. "Minimum order ₹5,000"). */
    val retailerMessage: String? = null,
    val failureReason: FailureReason? = null,
) {
    fun transitionTo(
        next: CouponTestStatus,
        observedSavings: Money? = this.observedSavings,
        observedTotal: Money? = this.observedTotal,
        retailerMessage: String? = this.retailerMessage,
        failureReason: FailureReason? = this.failureReason,
    ): CouponTestResult {
        if (!status.canTransitionTo(next)) throw IllegalCouponTransition(status, next)
        return copy(
            status = next,
            observedSavings = observedSavings,
            observedTotal = observedTotal,
            retailerMessage = retailerMessage,
            failureReason = failureReason,
        )
    }
}
