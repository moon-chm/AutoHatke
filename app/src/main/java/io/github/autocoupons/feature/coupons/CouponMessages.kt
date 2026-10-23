package io.github.autocoupons.feature.coupons

import io.github.autocoupons.domain.coupon.CouponFetchError
import io.github.autocoupons.domain.coupon.CouponTestStatus
import io.github.autocoupons.domain.coupon.SessionError

/** User-facing wording for domain errors, kept in one place so screens stay consistent. */
object CouponMessages {

    fun fetchError(error: CouponFetchError): Pair<String, String> = when (error) {
        is CouponFetchError.Http -> if (error.code == 404) {
            "Coupons aren't available right now" to
                "We couldn't load coupons for this store (error 404). Please try again later."
        } else {
            "Something went wrong" to "The coupon service returned an error (HTTP ${error.code}). Please try again."
        }
        CouponFetchError.Timeout -> "Taking too long" to "The coupon service didn't respond in time. Check your connection and try again."
        is CouponFetchError.Network -> "You're offline" to "Connect to the internet and try again."
        is CouponFetchError.Malformed -> "Couldn't read coupons" to "The coupon service sent something unexpected. Please try again later."
        is CouponFetchError.UnsupportedRetailer -> "No coupons for this store yet" to "We don't have a coupon source for this store yet."
    }

    fun sessionError(error: SessionError): Pair<String, String> = when (error) {
        is SessionError.Fetch -> fetchError(error.error)
        SessionError.NoCoupons -> "No coupons right now" to "There are no coupons for this store at the moment."
        is SessionError.CheckoutUnavailable -> "Open your bag to continue" to "${error.detail}. Go back to your bag and try again."
        SessionError.CartChanged -> "Your bag changed" to "Items changed while we were testing, so we stopped. Tap Try again to re-check."
        SessionError.CleanupFailed -> "We couldn't remove a test coupon" to "Testing stopped to keep your bag safe. Please check the coupon on your bag."
    }

    fun statusLabel(status: CouponTestStatus): String = when (status) {
        CouponTestStatus.DISCOVERED -> "Waiting"
        CouponTestStatus.TESTING -> "Trying"
        CouponTestStatus.APPLIED_AND_VERIFIED -> "Works"
        CouponTestStatus.REJECTED -> "Not applicable"
        CouponTestStatus.FAILED -> "Couldn't check"
        CouponTestStatus.UNVERIFIED -> "Unconfirmed"
        CouponTestStatus.CANCELLED -> "Skipped"
    }
}
