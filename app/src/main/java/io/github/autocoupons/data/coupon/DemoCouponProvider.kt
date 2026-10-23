package io.github.autocoupons.data.coupon

import io.github.autocoupons.domain.coupon.AdvertisedOffer
import io.github.autocoupons.domain.coupon.Coupon
import io.github.autocoupons.domain.coupon.CouponFetchError
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponProvider
import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.CouponType
import io.github.autocoupons.domain.coupon.Money

/**
 * Coupon feed for Modara, the fictional store bundled with the app. These are not Buyhatke offers.
 * Their real outcomes are decided by the store page (assets/demo-store/store.js), not by this
 * list, so the coupon session observes them exactly as it would a real checkout.
 */
class DemoCouponProvider : CouponProvider {

    override val source: CouponSource = CouponSource.DEMO_FIXTURE

    override suspend fun fetchCoupons(retailerId: String): CouponFetchResult =
        if (retailerId == DEMO_STORE_ID) CouponFetchResult.Success(FIXTURES, source)
        else CouponFetchResult.Failure(CouponFetchError.UnsupportedRetailer(retailerId), source)

    companion object {
        const val DEMO_STORE_ID = "demo-store"

        private fun demo(
            code: String,
            title: String,
            description: String,
            offer: AdvertisedOffer?,
            expiry: String? = null,
        ) = Coupon(
            id = "$DEMO_STORE_ID:$code",
            code = code,
            retailerId = DEMO_STORE_ID,
            source = CouponSource.DEMO_FIXTURE,
            title = title,
            description = description,
            advertised = offer,
            expiry = expiry,
        )

        /** Expected outcomes with the default demo cart (₹3,298) are documented in docs/TESTING.md. */
        val FIXTURES: List<Coupon> = listOf(
            demo(
                "FLAT100", "₹100 off", "Flat ₹100 off on orders of ₹999 or more",
                AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(100), minimumSpend = Money.inr(999)),
            ),
            demo(
                "SAVE10", "10% off, up to ₹200", "10% off your order, capped at ₹200",
                AdvertisedOffer(CouponType.PERCENTAGE, percent = 10, maximumDiscount = Money.inr(200)),
            ),
            demo(
                "STYLE250", "₹250 off", "Flat ₹250 off on orders of ₹2,500 or more",
                AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(250), minimumSpend = Money.inr(2500)),
            ),
            demo(
                "MEGA500", "₹500 off", "Flat ₹500 off on orders of ₹5,000 or more",
                AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(500), minimumSpend = Money.inr(5000)),
            ),
            demo(
                "SHOES15", "15% off footwear", "15% off footwear items, capped at ₹150",
                AdvertisedOffer(CouponType.PERCENTAGE, percent = 15, maximumDiscount = Money.inr(150)),
            ),
            demo(
                "FESTIVE50", "₹50 off", "Flat ₹50 off any order",
                AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(50)),
                expiry = "31 Mar 2026",
            ),
            demo("WELCOME75", "₹75 off first order", "New customers only", offer = null),
        )
    }
}
