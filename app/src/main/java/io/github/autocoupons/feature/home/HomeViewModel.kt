package io.github.autocoupons.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.autocoupons.appContainer
import io.github.autocoupons.core.web.NavigationDecision
import io.github.autocoupons.core.web.NavigationPolicy
import io.github.autocoupons.data.checkout.RetailerConfig
import io.github.autocoupons.data.checkout.RetailerRegistry
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponRepository
import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.Money
import io.github.autocoupons.feature.auth.DemoSession
import io.github.autocoupons.feature.auth.DemoSessionStore
import io.github.autocoupons.feature.coupons.CouponMessages
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LiveApiStatus {
    data object NotChecked : LiveApiStatus
    data object Checking : LiveApiStatus
    data class Available(val couponCount: Int, val checkedAtMillis: Long) : LiveApiStatus
    data class Unavailable(val title: String, val detail: String, val checkedAtMillis: Long) : LiveApiStatus
}

data class HomeUiState(
    val session: DemoSession? = null,
    val retailers: List<RetailerConfig> = emptyList(),
    val history: List<LastSessionSummary> = emptyList(),
    val liveApi: LiveApiStatus = LiveApiStatus.NotChecked,
    val welcome: String? = null,
) {
    val lastSession: LastSessionSummary? get() = history.firstOrNull()

    /** Sum of savings that were re-confirmed at checkout; null until there is at least one. */
    val totalSaved: Money? get() = history.mapNotNull { it.confirmedSavings }.reduceOrNull { a, b -> a + b }
}

/** Where a typed or pasted link should open, or why it cannot. */
sealed interface LinkResolution {
    data class Open(val retailerId: String, val url: String) : LinkResolution
    data class Rejected(val message: String) : LinkResolution
}

class HomeViewModel(
    private val sessions: DemoSessionStore,
    private val registry: RetailerRegistry,
    lastSessions: LastSessionStore,
    private val repository: CouponRepository,
) : ViewModel() {

    private val liveApi = MutableStateFlow<LiveApiStatus>(LiveApiStatus.NotChecked)
    private var liveCheck: Job? = null

    val state: StateFlow<HomeUiState> = combine(
        sessions.session, sessions.welcome, lastSessions.history, liveApi,
    ) { session, welcome, history, api ->
        HomeUiState(session, registry.retailers, history, api, welcome)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(retailers = registry.retailers),
    )

    fun consumeWelcome() = sessions.consumeWelcome()

    fun signOut() = sessions.end()

    /** Matches a pasted product or cart link to a supported store, using the same policy as the browser. */
    fun resolveLink(input: String): LinkResolution {
        val text = input.trim().let { if ("://" !in it && '.' in it && ' ' !in it) "https://$it" else it }
        val host = NavigationPolicy.hostOf(text)
            ?: return LinkResolution.Rejected("Paste a full store link, like https://www.myntra.com/…")
        val retailer = registry.retailers.firstOrNull { NavigationPolicy.isAllowedHost(host, it.allowedHosts) }
            ?: return LinkResolution.Rejected("$host isn't supported yet")
        return when (NavigationPolicy.evaluate(text, retailer.allowedHosts)) {
            NavigationDecision.Allow -> LinkResolution.Open(retailer.id, text)
            else -> LinkResolution.Rejected("That link can't be opened securely")
        }
    }

    /** One request on demand (never polled) that reports exactly what the coupon service returned. */
    fun checkLiveApi() {
        if (liveCheck?.isActive == true) return
        liveApi.value = LiveApiStatus.Checking
        liveCheck = viewModelScope.launch {
            liveApi.value = when (val result = repository.fetchCoupons("myntra", CouponSource.LIVE_API)) {
                is CouponFetchResult.Success -> LiveApiStatus.Available(result.coupons.size, System.currentTimeMillis())
                is CouponFetchResult.Failure -> {
                    val (title, detail) = CouponMessages.fetchError(result.error)
                    LiveApiStatus.Unavailable(title, detail, System.currentTimeMillis())
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                HomeViewModel(c.demoSessionStore, c.retailerRegistry, c.lastSessionStore, c.couponRepository)
            }
        }
    }
}
