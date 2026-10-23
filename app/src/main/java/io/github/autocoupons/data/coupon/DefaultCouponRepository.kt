package io.github.autocoupons.data.coupon

import io.github.autocoupons.domain.coupon.CouponFetchError
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponProvider
import io.github.autocoupons.domain.coupon.CouponRepository
import io.github.autocoupons.domain.coupon.CouponSource

/** Routes each request to exactly one provider so live and demo records never mix. */
class DefaultCouponRepository(providers: List<CouponProvider>) : CouponRepository {

    private val bySource = providers.associateBy { it.source }

    override suspend fun fetchCoupons(retailerId: String, source: CouponSource): CouponFetchResult {
        val provider = bySource[source]
            ?: return CouponFetchResult.Failure(CouponFetchError.UnsupportedRetailer(retailerId), source)
        return provider.fetchCoupons(retailerId)
    }
}
