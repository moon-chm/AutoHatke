package io.github.autocoupons.domain.coupon

import io.github.autocoupons.testing.coupon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CouponRankingEngineTest {

    private val engine = DefaultCouponRankingEngine()
    private val baseline = CartSnapshot(Money.inr(3_298), Money.zero("INR"), Money.inr(3_298), null, "fp")

    private fun verified(code: String, savingsRupees: Long, total: Money? = Money.inr(3_298 - savingsRupees)) =
        CouponTestResult(
            coupon(code),
            CouponTestStatus.APPLIED_AND_VERIFIED,
            observedSavings = Money.inr(savingsRupees),
            observedTotal = total,
        )

    private fun withStatus(code: String, status: CouponTestStatus, savingsRupees: Long? = null) =
        CouponTestResult(coupon(code), status, observedSavings = savingsRupees?.let(Money::inr))

    @Test
    fun `250 rupee verified saving beats 100 rupee verified saving`() {
        val decision = engine.rank(listOf(verified("A", 100), verified("B", 250)), baseline)
        assertEquals("B", decision.winner?.coupon?.code)
        assertEquals(listOf("B", "A"), decision.ranked.map { it.coupon.code })
    }

    @Test
    fun `rejected failed unverified and cancelled coupons never win even with larger claimed savings`() {
        val results = listOf(
            verified("SMALL", 10),
            withStatus("REJ", CouponTestStatus.REJECTED, 900),
            withStatus("FAIL", CouponTestStatus.FAILED, 900),
            withStatus("UNV", CouponTestStatus.UNVERIFIED, 900),
            withStatus("CAN", CouponTestStatus.CANCELLED, 900),
            withStatus("DISC", CouponTestStatus.DISCOVERED, 900),
        )
        val decision = engine.rank(results, baseline)
        assertEquals("SMALL", decision.winner?.coupon?.code)
        assertEquals(ExclusionReason.NOT_VERIFIED, decision.excluded["test:UNV"])
    }

    @Test
    fun `no eligible coupons means no winner`() {
        val decision = engine.rank(listOf(withStatus("REJ", CouponTestStatus.REJECTED)), baseline)
        assertNull(decision.winner)
    }

    @Test
    fun `ties break by source order then code, deterministically`() {
        val first = engine.rank(listOf(verified("ZETA", 200), verified("ALPHA", 200)), baseline)
        assertEquals("ZETA", first.winner?.coupon?.code)
        val reversed = engine.rank(listOf(verified("ALPHA", 200), verified("ZETA", 200)), baseline)
        assertEquals("ALPHA", reversed.winner?.coupon?.code)
    }

    @Test
    fun `different currency is excluded rather than compared`() {
        val usd = CouponTestResult(coupon("USD"), CouponTestStatus.APPLIED_AND_VERIFIED, Money(90_000, "USD"), Money(1, "USD"))
        val decision = engine.rank(listOf(usd, verified("INR", 50)), baseline)
        assertEquals("INR", decision.winner?.coupon?.code)
        assertEquals(ExclusionReason.CURRENCY_MISMATCH, decision.excluded["test:USD"])
    }

    @Test
    fun `zero negative and impossible savings are excluded`() {
        val results = listOf(
            verified("ZERO", 0),
            CouponTestResult(coupon("NEG"), CouponTestStatus.APPLIED_AND_VERIFIED, Money(-100, "INR"), null),
            verified("HUGE", 5_000, total = null),
            CouponTestResult(coupon("NONE"), CouponTestStatus.APPLIED_AND_VERIFIED),
        )
        val decision = engine.rank(results, baseline)
        assertNull(decision.winner)
        assertEquals(ExclusionReason.NON_POSITIVE_SAVINGS, decision.excluded["test:ZERO"])
        assertEquals(ExclusionReason.NON_POSITIVE_SAVINGS, decision.excluded["test:NEG"])
        assertEquals(ExclusionReason.SAVINGS_EXCEED_SUBTOTAL, decision.excluded["test:HUGE"])
        assertEquals(ExclusionReason.MISSING_SAVINGS, decision.excluded["test:NONE"])
    }

    @Test
    fun `savings that do not match the observed total are excluded`() {
        val decision = engine.rank(listOf(verified("ODD", 250, total = Money.inr(3_200))), baseline)
        assertEquals(ExclusionReason.TOTAL_INCONSISTENT, decision.excluded["test:ODD"])
    }
}
