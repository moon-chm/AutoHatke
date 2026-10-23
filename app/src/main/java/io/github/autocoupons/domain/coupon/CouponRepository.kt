package io.github.autocoupons.domain.coupon

sealed interface CouponFetchError {
    /** The server answered with a non-success status (e.g. 404 for the retired legacy API). */
    data class Http(val code: Int) : CouponFetchError
    data object Timeout : CouponFetchError
    data class Network(val detail: String) : CouponFetchError
    data class Malformed(val detail: String) : CouponFetchError
    /** No coupon feed exists for this retailer in the selected source. */
    data class UnsupportedRetailer(val retailerId: String) : CouponFetchError
}

sealed interface CouponFetchResult {
    data class Success(val coupons: List<Coupon>, val source: CouponSource) : CouponFetchResult
    data class Failure(val error: CouponFetchError, val source: CouponSource) : CouponFetchResult
}

/** One coupon feed (live API or demo fixtures). */
interface CouponProvider {
    val source: CouponSource
    suspend fun fetchCoupons(retailerId: String): CouponFetchResult
}

/** Chooses the feed for a retailer; the UI never talks to providers directly. */
interface CouponRepository {
    suspend fun fetchCoupons(retailerId: String, source: CouponSource): CouponFetchResult
}
