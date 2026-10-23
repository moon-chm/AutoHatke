package io.github.autocoupons.feature.home

import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Summary of one finished coupon session in this app run. Never pre-populated. */
data class LastSessionSummary(
    val retailerId: String,
    val retailerName: String,
    val source: CouponSource,
    val tested: Int,
    val baselineTotal: Money?,
    val verified: Int,
    val winnerCode: String?,
    val savings: Money?,
    val confirmedAfterReapply: Boolean,
    val finishedAtMillis: Long,
) {
    /** Savings that count towards totals: only winners re-confirmed at checkout. */
    val confirmedSavings: Money? get() = savings?.takeIf { confirmedAfterReapply }
}

/** In-memory history of sessions, newest first. Nothing is written to disk. */
class LastSessionStore {
    private val _history = MutableStateFlow<List<LastSessionSummary>>(emptyList())
    val history: StateFlow<List<LastSessionSummary>> = _history.asStateFlow()

    fun record(summary: LastSessionSummary) {
        _history.update { (listOf(summary) + it).take(MAX_ENTRIES) }
    }

    private companion object {
        const val MAX_ENTRIES = 50
    }
}
