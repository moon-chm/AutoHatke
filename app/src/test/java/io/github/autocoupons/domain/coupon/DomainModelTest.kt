package io.github.autocoupons.domain.coupon

import io.github.autocoupons.testing.coupon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test fun `formats rupees with Indian grouping`() {
        assertEquals("₹0", Money.inr(0).format())
        assertEquals("₹999", Money.inr(999).format())
        assertEquals("₹3,298", Money.inr(3_298).format())
        assertEquals("₹1,23,456", Money.inr(123_456).format())
        assertEquals("₹1,00,00,000", Money.inr(10_000_000).format())
        assertEquals("₹2,499.50", Money(249_950, "INR").format())
        assertEquals("−₹50", Money.inr(-50).format())
    }

    @Test fun `arithmetic in minor units`() {
        assertEquals(Money.inr(3_048), Money.inr(3_298) - Money.inr(250))
        assertTrue(Money.inr(250) > Money.inr(100))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cross-currency arithmetic is rejected`() {
        Money.inr(1) + Money(1, "USD")
    }
}

class CouponStateMachineTest {
    @Test fun `allowed transitions`() {
        assertTrue(CouponTestStatus.DISCOVERED.canTransitionTo(CouponTestStatus.TESTING))
        assertTrue(CouponTestStatus.DISCOVERED.canTransitionTo(CouponTestStatus.CANCELLED))
        listOf(
            CouponTestStatus.APPLIED_AND_VERIFIED, CouponTestStatus.REJECTED, CouponTestStatus.FAILED,
            CouponTestStatus.UNVERIFIED, CouponTestStatus.CANCELLED,
        ).forEach { assertTrue(CouponTestStatus.TESTING.canTransitionTo(it)) }
    }

    @Test fun `forbidden transitions`() {
        assertFalse(CouponTestStatus.DISCOVERED.canTransitionTo(CouponTestStatus.APPLIED_AND_VERIFIED))
        assertFalse(CouponTestStatus.REJECTED.canTransitionTo(CouponTestStatus.APPLIED_AND_VERIFIED))
        assertFalse(CouponTestStatus.APPLIED_AND_VERIFIED.canTransitionTo(CouponTestStatus.TESTING))
        assertFalse(CouponTestStatus.TESTING.canTransitionTo(CouponTestStatus.DISCOVERED))
    }

    @Test(expected = IllegalCouponTransition::class)
    fun `a discovered coupon cannot jump straight to verified`() {
        CouponTestResult(coupon("X")).transitionTo(CouponTestStatus.APPLIED_AND_VERIFIED)
    }

    @Test fun `cancelling a session marks only unfinished coupons`() {
        val state = CouponSessionState(
            "test", CouponSource.DEMO_FIXTURE, SessionPhase.TESTING,
            results = listOf(
                CouponTestResult(coupon("DONE"), CouponTestStatus.REJECTED),
                CouponTestResult(coupon("NOW"), CouponTestStatus.TESTING),
                CouponTestResult(coupon("LATER")),
            ),
        )
        val cancelled = state.cancelled()
        assertEquals(
            listOf(CouponTestStatus.REJECTED, CouponTestStatus.CANCELLED, CouponTestStatus.CANCELLED),
            cancelled.results.map { it.status },
        )
        assertEquals(1, cancelled.completed)
    }
}

class AdvertisedDiscountEstimatorTest {
    private val cart = Money.inr(3_298)

    @Test fun `flat amount`() = assertEquals(
        AdvertisedEstimate.Discount(Money.inr(100)),
        AdvertisedDiscountEstimator.estimate(AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(100)), cart),
    )

    @Test fun `flat amount never exceeds the subtotal`() = assertEquals(
        AdvertisedEstimate.Discount(Money.inr(500)),
        AdvertisedDiscountEstimator.estimate(AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(900)), Money.inr(500)),
    )

    @Test fun `percentage without cap uses integer paise`() = assertEquals(
        AdvertisedEstimate.Discount(Money(32_980, "INR")),
        AdvertisedDiscountEstimator.estimate(AdvertisedOffer(CouponType.PERCENTAGE, percent = 10), cart),
    )

    @Test fun `percentage is capped`() = assertEquals(
        AdvertisedEstimate.Discount(Money.inr(200)),
        AdvertisedDiscountEstimator.estimate(
            AdvertisedOffer(CouponType.PERCENTAGE, percent = 10, maximumDiscount = Money.inr(200)), cart,
        ),
    )

    @Test fun `minimum spend not met`() = assertEquals(
        AdvertisedEstimate.BelowMinimumSpend(Money.inr(5_000)),
        AdvertisedDiscountEstimator.estimate(
            AdvertisedOffer(CouponType.FLAT, flatAmount = Money.inr(500), minimumSpend = Money.inr(5_000)), cart,
        ),
    )

    @Test fun `unknown and invalid offers produce no estimate`() {
        assertEquals(AdvertisedEstimate.Unknown, AdvertisedDiscountEstimator.estimate(null, cart))
        assertEquals(AdvertisedEstimate.Unknown, AdvertisedDiscountEstimator.estimate(AdvertisedOffer(CouponType.PERCENTAGE, percent = 0), cart))
        assertEquals(
            AdvertisedEstimate.Unknown,
            AdvertisedDiscountEstimator.estimate(AdvertisedOffer(CouponType.FLAT, flatAmount = Money(100, "USD")), cart),
        )
        assertEquals(AdvertisedEstimate.FreeShipping, AdvertisedDiscountEstimator.estimate(AdvertisedOffer(CouponType.FREE_SHIPPING), cart))
    }
}
