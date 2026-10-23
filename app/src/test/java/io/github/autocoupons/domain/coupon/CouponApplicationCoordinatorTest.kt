package io.github.autocoupons.domain.coupon

import io.github.autocoupons.testing.FakeCheckout
import io.github.autocoupons.testing.FakeCouponRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CouponApplicationCoordinatorTest {

    private val allDemoCodes = arrayOf("FLAT100", "SAVE10", "STYLE250", "MEGA500", "SHOES15", "FESTIVE50", "WELCOME75")

    private fun coordinator(repo: CouponRepository) =
        CouponApplicationCoordinator(repo, DefaultCouponRankingEngine(), applyTimeoutMs = 1_000)

    private suspend fun run(repo: CouponRepository, checkout: FakeCheckout) =
        coordinator(repo).run("test", CouponSource.DEMO_FIXTURE, checkout).toList()

    @Test
    fun `happy path picks STYLE250, re-applies it and verifies the final total`() = runTest {
        val checkout = FakeCheckout()
        val states = run(FakeCouponRepository.of(*allDemoCodes), checkout)
        val final = states.last()

        assertEquals(SessionPhase.COMPLETED, final.phase)
        val outcome = final.outcome as SessionOutcome.WinnerVerified
        assertEquals("STYLE250", outcome.winner.coupon.code)
        assertEquals(Money.inr(250), outcome.winner.observedSavings)
        assertEquals(Money.inr(3_048), outcome.finalCart.total)
        assertEquals("STYLE250", checkout.appliedCode)
        // Every coupon tested once, then the winner applied a second time.
        assertEquals(allDemoCodes.toList() + "STYLE250", checkout.applyLog)

        val byCode = final.results.associateBy { it.coupon.code }
        assertEquals(CouponTestStatus.APPLIED_AND_VERIFIED, byCode.getValue("FLAT100").status)
        assertEquals(Money.inr(200), byCode.getValue("SAVE10").observedSavings)
        assertEquals(Money.inr(150), byCode.getValue("SHOES15").observedSavings)
        assertEquals(CouponTestStatus.REJECTED, byCode.getValue("MEGA500").status)
        assertEquals("Add items worth ₹1,702 more", byCode.getValue("MEGA500").retailerMessage)
        assertEquals(CouponTestStatus.REJECTED, byCode.getValue("FESTIVE50").status)
        assertEquals(CouponTestStatus.REJECTED, byCode.getValue("WELCOME75").status)
    }

    @Test
    fun `progress counts only finished tests and never decreases`() = runTest {
        val states = run(FakeCouponRepository.of(*allDemoCodes), FakeCheckout())
        val completed = states.map { it.completed }
        assertEquals(completed.sorted(), completed)
        assertEquals(7, states.last().completed)
        // While a coupon is being tested it is not counted as done.
        val testing = states.first { it.current?.status == CouponTestStatus.TESTING }
        assertEquals(0, testing.completed)
        assertEquals(1f, states.last().progress)
    }

    @Test
    fun `best so far tracks the highest verified saving during the run`() = runTest {
        val states = run(FakeCouponRepository.of(*allDemoCodes), FakeCheckout())
        val bests = states.mapNotNull { it.bestSoFar?.coupon?.code }.distinct()
        assertEquals(listOf("FLAT100", "SAVE10", "STYLE250"), bests)
    }

    @Test
    fun `cart is back at baseline before every test`() = runTest {
        val checkout = FakeCheckout()
        val seenCodes = mutableListOf<String?>()
        checkout.beforeApply = { seenCodes += checkout.appliedCode }
        run(FakeCouponRepository.of(*allDemoCodes), checkout)
        assertTrue(seenCodes.all { it == null })
    }

    @Test
    fun `no verified savings leaves the bag without a coupon`() = runTest {
        val checkout = FakeCheckout()
        val final = run(FakeCouponRepository.of("MEGA500", "FESTIVE50"), checkout).last()
        assertEquals(SessionOutcome.NoVerifiedSavings, final.outcome)
        assertNull(checkout.appliedCode)
    }

    @Test
    fun `shopper's pre-applied coupon is removed for testing and restored when nothing beats it`() = runTest {
        val checkout = FakeCheckout(subtotalMinor = 200_000)
        checkout.appliedCode = null
        // Pre-apply FLAT100 as the shopper would have.
        checkout.applyCoupon("FLAT100")
        checkout.applyLog.clear()

        val final = run(FakeCouponRepository.of("MEGA500", "FESTIVE50"), checkout).last()
        assertEquals("FLAT100", final.originalCode)
        assertEquals(SessionOutcome.NoVerifiedSavings, final.outcome)
        assertEquals("FLAT100", checkout.appliedCode)
    }

    @Test
    fun `fetch failure ends the session with the fetch error`() = runTest {
        val final = run(FakeCouponRepository.failing(CouponFetchError.Http(404)), FakeCheckout()).last()
        assertEquals(SessionPhase.FAILED, final.phase)
        assertEquals(SessionError.Fetch(CouponFetchError.Http(404)), final.error)
    }

    @Test
    fun `empty coupon list is reported as no coupons`() = runTest {
        val final = run(FakeCouponRepository.of(), FakeCheckout()).last()
        assertEquals(SessionError.NoCoupons, final.error)
    }

    @Test
    fun `duplicate codes from the feed are tested once`() = runTest {
        val checkout = FakeCheckout()
        val final = run(FakeCouponRepository.of("FLAT100", "flat100", "STYLE250"), checkout).last()
        assertEquals(2, final.total)
    }

    @Test
    fun `unresponsive store marks the coupon failed with a timeout and continues`() = runTest {
        val checkout = FakeCheckout().apply { hangingCodes += "SAVE10" }
        val final = run(FakeCouponRepository.of("SAVE10", "STYLE250"), checkout).last()
        val save10 = final.results.first { it.coupon.code == "SAVE10" }
        assertEquals(CouponTestStatus.FAILED, save10.status)
        assertEquals(FailureReason.TIMED_OUT, save10.failureReason)
        assertEquals("STYLE250", (final.outcome as SessionOutcome.WinnerVerified).winner.coupon.code)
    }

    @Test
    fun `accepted coupon with inconsistent totals is unverified and cannot win`() = runTest {
        val checkout = FakeCheckout().apply { inconsistentCodes += "STYLE250" }
        val final = run(FakeCouponRepository.of("FLAT100", "STYLE250"), checkout).last()
        val style250 = final.results.first { it.coupon.code == "STYLE250" }
        assertEquals(CouponTestStatus.UNVERIFIED, style250.status)
        assertEquals("FLAT100", (final.outcome as SessionOutcome.WinnerVerified).winner.coupon.code)
    }

    @Test
    fun `cart edited mid-session aborts with CartChanged`() = runTest {
        val checkout = FakeCheckout()
        checkout.beforeApply = { count -> if (count == 1) checkout.fingerprint = "sneakersx2|teex1" }
        val final = run(FakeCouponRepository.of("FLAT100", "SAVE10", "STYLE250"), checkout).last()
        assertEquals(SessionPhase.FAILED, final.phase)
        assertEquals(SessionError.CartChanged, final.error)
        assertTrue(final.results.none { it.status == CouponTestStatus.TESTING })
    }

    @Test
    fun `coupon that cannot be removed stops the session`() = runTest {
        val checkout = FakeCheckout().apply { refuseRemoval = true }
        val final = run(FakeCouponRepository.of("FLAT100", "STYLE250"), checkout).last()
        assertEquals(SessionError.CleanupFailed, final.error)
    }

    @Test
    fun `different saving on re-apply is reported as not confirmed`() = runTest {
        val checkout = FakeCheckout()
        checkout.discountOverride = { code, attempt -> if (code == "STYLE250" && attempt == 2) 10_000 else null }
        val final = run(FakeCouponRepository.of("FLAT100", "STYLE250"), checkout).last()
        val outcome = final.outcome as SessionOutcome.WinnerNotConfirmed
        assertEquals("STYLE250", outcome.winner.coupon.code)
        assertTrue(outcome.reason.contains("₹100"))
    }

    @Test
    fun `cancelling mid-test removes the half-tested coupon`() = runTest {
        val checkout = FakeCheckout().apply { applyThenHangCodes += "STYLE250" }
        val coordinator = CouponApplicationCoordinator(FakeCouponRepository.of("FLAT100", "STYLE250"), DefaultCouponRankingEngine(), applyTimeoutMs = 60_000)
        val states = mutableListOf<CouponSessionState>()
        val job = launch { coordinator.run("test", CouponSource.DEMO_FIXTURE, checkout).collect { states += it } }
        advanceTimeBy(1_000)
        assertEquals("STYLE250", states.last().current?.coupon?.code)
        assertEquals("STYLE250", checkout.appliedCode)
        job.cancel()
        job.join()
        assertNull(checkout.appliedCode)
        val cancelled = states.last().cancelled()
        assertEquals(SessionPhase.CANCELLED, cancelled.phase)
        assertEquals(CouponTestStatus.CANCELLED, cancelled.results.first { it.coupon.code == "STYLE250" }.status)
        assertEquals(CouponTestStatus.APPLIED_AND_VERIFIED, cancelled.results.first { it.coupon.code == "FLAT100" }.status)
    }
}
