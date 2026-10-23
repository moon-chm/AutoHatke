package io.github.autocoupons.testing

import io.github.autocoupons.domain.coupon.ApplyObservation
import io.github.autocoupons.domain.coupon.CartSnapshot
import io.github.autocoupons.domain.coupon.CheckoutAdapter
import io.github.autocoupons.domain.coupon.Coupon
import io.github.autocoupons.domain.coupon.CouponFetchError
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponRepository
import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.Money
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

fun coupon(code: String, source: CouponSource = CouponSource.DEMO_FIXTURE) =
    Coupon(id = "test:$code", code = code, retailerId = "test", source = source)

class FakeCouponRepository(private val result: CouponFetchResult) : CouponRepository {
    var calls = 0
    override suspend fun fetchCoupons(retailerId: String, source: CouponSource): CouponFetchResult {
        calls++
        return result
    }

    companion object {
        fun of(vararg codes: String) =
            FakeCouponRepository(CouponFetchResult.Success(codes.map { coupon(it) }, CouponSource.DEMO_FIXTURE))

        fun failing(error: CouponFetchError) =
            FakeCouponRepository(CouponFetchResult.Failure(error, CouponSource.LIVE_API))
    }
}

/**
 * In-memory checkout mirroring the Demo Store rules (assets/demo-store/store.js) for a
 * ₹3,298 bag with one footwear item. Hooks let tests inject retailer misbehaviour.
 */
class FakeCheckout(
    var subtotalMinor: Long = 329_800,
    private val footwearMinor: Long = 249_900,
    initialCode: String? = null,
) : CheckoutAdapter {
    override val retailerId = "test"

    var appliedCode: String? = initialCode
    var fingerprint = "sneakersx1|teex1"
    val applyLog = mutableListOf<String>()
    var removeCount = 0

    /** Codes for which apply() suspends forever (simulates an unresponsive store). */
    val hangingCodes = mutableSetOf<String>()
    /** Codes the store applies but never confirms, leaving them on the cart. */
    val applyThenHangCodes = mutableSetOf<String>()
    /** Codes the store accepts but whose totals don't add up. */
    val inconsistentCodes = mutableSetOf<String>()
    /** Called before each apply with the number of applies so far. */
    var beforeApply: (Int) -> Unit = {}
    /** Overrides the discount on the N-th apply of a code (1-based), for re-apply mismatch tests. */
    var discountOverride: (code: String, attempt: Int) -> Long? = { _, _ -> null }
    var refuseRemoval = false

    private fun rule(code: String): Pair<Long?, String> {
        val sub = subtotalMinor
        return when (code) {
            "FLAT100" -> if (sub >= 99_900) 10_000L to "FLAT100 applied" else null to "minimum not met"
            "SAVE10" -> minOf(sub * 10 / 100, 20_000) to "SAVE10 applied"
            "STYLE250" -> if (sub >= 250_000) 25_000L to "STYLE250 applied" else null to "Minimum ₹2,500"
            "MEGA500" -> if (sub >= 500_000) 50_000L to "MEGA500 applied" else null to "Add items worth ₹1,702 more"
            "SHOES15" -> minOf(footwearMinor * 15 / 100, 15_000) to "SHOES15 applied"
            "FESTIVE50" -> null to "This coupon expired on 31 Mar 2026"
            "TIE200" -> 20_000L to "TIE200 applied"
            else -> null to "Coupon code $code is not valid"
        }
    }

    private fun discountMinor(): Long = appliedCode?.let { currentDiscount[it] } ?: 0
    private val currentDiscount = mutableMapOf<String, Long>()

    private fun snapshot(): CartSnapshot {
        val discount = discountMinor()
        val inconsistent = appliedCode in inconsistentCodes
        return CartSnapshot(
            subtotal = Money(subtotalMinor, Money.INR),
            discount = Money(discount, Money.INR),
            total = Money(subtotalMinor - discount + if (inconsistent) 5_000 else 0, Money.INR),
            appliedCode = appliedCode,
            fingerprint = fingerprint,
        )
    }

    override suspend fun readCart(): CartSnapshot = snapshot()

    override suspend fun applyCoupon(code: String): ApplyObservation {
        beforeApply(applyLog.size)
        applyLog += code
        if (code in hangingCodes) awaitCancellation()
        delay(10)
        if (appliedCode != null) return ApplyObservation.Rejected(snapshot(), "Remove $appliedCode first")
        val (discount, message) = rule(code)
        if (discount == null) return ApplyObservation.Rejected(snapshot(), message)
        val attempt = applyLog.count { it == code }
        currentDiscount[code] = discountOverride(code, attempt) ?: discount
        appliedCode = code
        if (code in applyThenHangCodes) awaitCancellation()
        return ApplyObservation.Accepted(snapshot(), message)
    }

    override suspend fun removeCoupon(): CartSnapshot {
        removeCount++
        delay(5)
        if (!refuseRemoval) appliedCode = null
        return snapshot()
    }
}
