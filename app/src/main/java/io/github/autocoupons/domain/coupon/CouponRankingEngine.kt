package io.github.autocoupons.domain.coupon

enum class ExclusionReason {
    NOT_VERIFIED,
    MISSING_SAVINGS,
    NON_POSITIVE_SAVINGS,
    CURRENCY_MISMATCH,
    SAVINGS_EXCEED_SUBTOTAL,
    TOTAL_INCONSISTENT,
}

data class RankingDecision(
    /** Highest verified saving, or null when nothing qualified. */
    val winner: CouponTestResult?,
    /** Eligible coupons, best first. */
    val ranked: List<CouponTestResult>,
    val excluded: Map<String, ExclusionReason>,
)

fun interface CouponRankingEngine {
    fun rank(results: List<CouponTestResult>, baseline: CartSnapshot): RankingDecision
}

/**
 * Picks the coupon with the largest *observed* saving.
 *
 * Eligibility: status APPLIED_AND_VERIFIED, savings in the baseline currency, 0 < savings ≤
 * subtotal, and (when an observed total exists) total == subtotal − savings.
 *
 * Ordering, fully deterministic:
 *  1. larger saving
 *  2. lower observed total
 *  3. earlier position in the source list (the order the coupon feed returned)
 *  4. coupon code, lexicographically
 */
class DefaultCouponRankingEngine : CouponRankingEngine {

    override fun rank(results: List<CouponTestResult>, baseline: CartSnapshot): RankingDecision {
        val excluded = linkedMapOf<String, ExclusionReason>()
        val eligible = mutableListOf<IndexedValue<CouponTestResult>>()

        results.forEachIndexed { index, result ->
            val reason = exclusionReason(result, baseline)
            if (reason == null) eligible += IndexedValue(index, result) else excluded[result.coupon.id] = reason
        }

        val ranked = eligible.sortedWith(
            compareByDescending<IndexedValue<CouponTestResult>> { it.value.observedSavings!!.minor }
                .thenBy { it.value.observedTotal?.minor ?: Long.MAX_VALUE }
                .thenBy { it.index }
                .thenBy { it.value.coupon.code },
        ).map { it.value }

        return RankingDecision(winner = ranked.firstOrNull(), ranked = ranked, excluded = excluded)
    }

    private fun exclusionReason(result: CouponTestResult, baseline: CartSnapshot): ExclusionReason? {
        if (result.status != CouponTestStatus.APPLIED_AND_VERIFIED) return ExclusionReason.NOT_VERIFIED
        val savings = result.observedSavings ?: return ExclusionReason.MISSING_SAVINGS
        if (savings.currency != baseline.currency) return ExclusionReason.CURRENCY_MISMATCH
        if (!savings.isPositive()) return ExclusionReason.NON_POSITIVE_SAVINGS
        if (savings.minor > baseline.subtotal.minor) return ExclusionReason.SAVINGS_EXCEED_SUBTOTAL
        val total = result.observedTotal
        if (total != null) {
            if (total.currency != baseline.currency) return ExclusionReason.CURRENCY_MISMATCH
            if (total.minor != baseline.subtotal.minor - savings.minor) return ExclusionReason.TOTAL_INCONSISTENT
        }
        return null
    }
}
