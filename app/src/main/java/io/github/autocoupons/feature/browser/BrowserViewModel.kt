package io.github.autocoupons.feature.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.autocoupons.appContainer
import io.github.autocoupons.core.web.NavigationDecision
import io.github.autocoupons.core.web.NavigationPolicy
import io.github.autocoupons.data.checkout.DemoStoreCheckoutAdapter
import io.github.autocoupons.data.checkout.RetailerConfig
import io.github.autocoupons.domain.coupon.AutomationSupport
import io.github.autocoupons.domain.coupon.CheckoutAdapter
import io.github.autocoupons.domain.coupon.CheckoutDetection
import io.github.autocoupons.domain.coupon.CheckoutUnavailableException
import io.github.autocoupons.domain.coupon.Coupon
import io.github.autocoupons.domain.coupon.CouponApplicationCoordinator
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponRepository
import io.github.autocoupons.domain.coupon.CouponSessionState
import io.github.autocoupons.domain.coupon.CouponTestStatus
import io.github.autocoupons.domain.coupon.SessionOutcome
import io.github.autocoupons.domain.coupon.SessionPhase
import io.github.autocoupons.feature.coupons.CouponMessages
import io.github.autocoupons.feature.home.LastSessionStore
import io.github.autocoupons.feature.home.LastSessionSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PageError(val title: String, val detail: String)

/** One-off notice for a navigation the browser refused to perform. */
data class NavigationNotice(val message: String, val externalUrl: String? = null)

enum class SheetMode { SESSION, ASSISTED, NOT_ON_CHECKOUT, UNSUPPORTED_WEBVIEW }

sealed interface AssistedCoupons {
    data object Loading : AssistedCoupons
    data class Loaded(val coupons: List<Coupon>) : AssistedCoupons
    data class Failed(val title: String, val detail: String) : AssistedCoupons
}

data class BrowserUiState(
    val retailer: RetailerConfig,
    val url: String? = null,
    val pageTitle: String? = null,
    val progress: Int = 0,
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val pageError: PageError? = null,
    val detection: CheckoutDetection = CheckoutDetection.Unsupported,
    /** For automated retailers: the page structure was probed and has a coupon field. */
    val couponEntryConfirmed: Boolean = false,
    val bridgeSupported: Boolean = true,
    val notice: NavigationNotice? = null,
    val sheetMode: SheetMode? = null,
    val session: CouponSessionState? = null,
    /** True while a cancelled session is still putting the cart back. */
    val restoring: Boolean = false,
    val assisted: AssistedCoupons? = null,
    /** The WebView renderer died; a fresh WebView must be created before anything else works. */
    val rendererGone: Boolean = false,
    val webViewGeneration: Int = 0,
) {
    val sessionRunning: Boolean get() = session?.phase?.isRunning == true || restoring

    /** The floating assistant only appears on a reliably detected checkout of this retailer. */
    val assistantVisible: Boolean
        get() = detection.isCheckout && detection.retailerId == retailer.id && pageError == null && when (retailer.automation) {
            AutomationSupport.AUTOMATED -> couponEntryConfirmed && bridgeSupported
            AutomationSupport.USER_ASSISTED -> true
        }
}

class BrowserViewModel(
    private val retailer: RetailerConfig,
    private val savedState: SavedStateHandle,
    private val repository: CouponRepository,
    private val coordinator: CouponApplicationCoordinator,
    private val lastSessions: LastSessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserUiState(retailer))
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var probeJob: Job? = null
    private var assistedJob: Job? = null

    /** URL to load when a WebView is (re)created: the last page, if still allowed, else the store start. */
    fun initialUrl(): String {
        return listOfNotNull(savedState.get<String>(KEY_URL), savedState.get<String>(ARG_URL))
            .firstOrNull { NavigationPolicy.evaluate(it, retailer.allowedHosts) == NavigationDecision.Allow }
            ?: retailer.startUrl
    }

    fun onBridgeSupport(supported: Boolean) = _state.update { it.copy(bridgeSupported = supported) }

    // --- WebView callbacks -------------------------------------------------------------------

    fun onPageStarted(url: String) {
        probeJob?.cancel()
        _state.update {
            it.copy(url = url, loading = true, progress = 0, pageError = null, couponEntryConfirmed = false, detection = retailer.detector.detect(url))
        }
    }

    fun onProgress(progress: Int) = _state.update { it.copy(progress = progress, loading = progress < 100) }

    fun onTitle(title: String?) = _state.update { it.copy(pageTitle = title?.takeIf { t -> t.isNotBlank() }) }

    /** Covers full loads and single-page-app history changes (pushState). */
    fun onUrlChanged(url: String, canGoBack: Boolean, canGoForward: Boolean) {
        savedState[KEY_URL] = url
        _state.update {
            val detection = retailer.detector.detect(url)
            it.copy(
                url = url,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                detection = detection,
                couponEntryConfirmed = it.couponEntryConfirmed && detection == it.detection,
            )
        }
    }

    fun onPageFinished(url: String, canGoBack: Boolean, canGoForward: Boolean, adapter: DemoStoreCheckoutAdapter?) {
        _state.update { it.copy(loading = false, progress = 100, canGoBack = canGoBack, canGoForward = canGoForward) }
        val detection = retailer.detector.detect(url)
        if (adapter != null && retailer.automation == AutomationSupport.AUTOMATED && detection.isCheckout) {
            probeJob?.cancel()
            probeJob = viewModelScope.launch {
                val confirmed = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                    try {
                        adapter.hasCouponEntry()
                    } catch (e: CheckoutUnavailableException) {
                        false
                    }
                } ?: false
                _state.update { if (it.url == url) it.copy(couponEntryConfirmed = confirmed) else it }
            }
        }
    }

    fun onMainFrameError(title: String, detail: String) =
        _state.update { it.copy(pageError = PageError(title, detail), loading = false) }

    fun onRendererGone() {
        cancelSession()
        _state.update {
            it.copy(
                pageError = PageError("The page stopped responding", "The browser engine closed this page to protect the app. Reload to continue."),
                rendererGone = true,
                loading = false,
            )
        }
    }

    fun onNavigationRefused(decision: NavigationDecision) {
        val notice = when (decision) {
            is NavigationDecision.External -> NavigationNotice(
                "${NavigationPolicy.hostOf(decision.url) ?: "That page"} is outside ${retailer.displayName}, so it was not opened here.",
                externalUrl = decision.url,
            )
            is NavigationDecision.Block -> NavigationNotice("${decision.reason}.")
            NavigationDecision.Allow -> return
        }
        _state.update { it.copy(notice = notice) }
    }

    fun onDownloadBlocked() = _state.update { it.copy(notice = NavigationNotice("Downloads are not supported in the shopping browser.")) }

    fun consumeNotice() = _state.update { it.copy(notice = null) }

    /** Retry after an error. Returns true when the caller can simply reload the existing WebView. */
    fun retry(): Boolean {
        val gone = _state.value.rendererGone
        _state.update {
            if (gone) it.copy(pageError = null, rendererGone = false, webViewGeneration = it.webViewGeneration + 1)
            else it.copy(pageError = null)
        }
        return !gone
    }

    // --- Assistant & sheet -------------------------------------------------------------------

    /** Tap on the floating assistant or the manual "Find coupons" action. */
    fun onAssistantRequested(adapter: CheckoutAdapter?) {
        val current = _state.value
        when {
            current.sessionRunning -> showSheet(SheetMode.SESSION)
            retailer.automation == AutomationSupport.USER_ASSISTED -> {
                showSheet(SheetMode.ASSISTED)
                if (current.assisted !is AssistedCoupons.Loaded) loadAssistedCoupons()
            }
            !current.bridgeSupported -> showSheet(SheetMode.UNSUPPORTED_WEBVIEW)
            !current.assistantVisible || adapter == null -> showSheet(SheetMode.NOT_ON_CHECKOUT)
            else -> startSession(adapter)
        }
    }

    fun startSession(adapter: CheckoutAdapter) {
        if (_state.value.sessionRunning) {
            showSheet(SheetMode.SESSION)
            return
        }
        showSheet(SheetMode.SESSION)
        sessionJob = viewModelScope.launch {
            var last: CouponSessionState? = null
            coordinator.run(retailer.id, retailer.couponSource, adapter).collect { session ->
                last = session
                _state.update { it.copy(session = session) }
            }
            last?.takeIf { it.phase == SessionPhase.COMPLETED }?.let(::recordSummary)
        }
    }

    fun cancelSession() {
        val job = sessionJob ?: return
        if (!job.isActive) return
        _state.update { it.copy(session = it.session?.cancelled(), restoring = true) }
        viewModelScope.launch {
            job.cancelAndJoin() // waits for the coordinator to put the cart back
            _state.update { it.copy(restoring = false) }
        }
    }

    fun loadAssistedCoupons() {
        if (assistedJob?.isActive == true) return
        _state.update { it.copy(assisted = AssistedCoupons.Loading) }
        assistedJob = viewModelScope.launch {
            val assisted = when (val result = repository.fetchCoupons(retailer.id, retailer.couponSource)) {
                is CouponFetchResult.Success -> AssistedCoupons.Loaded(result.coupons)
                is CouponFetchResult.Failure -> CouponMessages.fetchError(result.error).let { (t, d) -> AssistedCoupons.Failed(t, d) }
            }
            _state.update { it.copy(assisted = assisted) }
        }
    }

    fun showSheet(mode: SheetMode) = _state.update { it.copy(sheetMode = mode) }

    fun dismissSheet() = _state.update { it.copy(sheetMode = null) }

    private fun recordSummary(session: CouponSessionState) {
        val outcome = session.outcome
        val winner = when (outcome) {
            is SessionOutcome.WinnerVerified -> outcome.winner
            is SessionOutcome.WinnerNotConfirmed -> outcome.winner
            else -> null
        }
        lastSessions.record(
            LastSessionSummary(
                retailerId = retailer.id,
                retailerName = retailer.displayName,
                source = session.source,
                tested = session.completed,
                baselineTotal = session.baseline?.total,
                verified = session.results.count { it.status == CouponTestStatus.APPLIED_AND_VERIFIED },
                winnerCode = winner?.coupon?.code,
                savings = winner?.observedSavings,
                confirmedAfterReapply = outcome is SessionOutcome.WinnerVerified,
                finishedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    companion object {
        const val ARG_RETAILER = "retailerId"

        /** Optional link the shopper pasted on Home; used only if it passes the retailer's policy. */
        const val ARG_URL = "url"
        private const val KEY_URL = "lastUrl"
        private const val PROBE_TIMEOUT_MS = 3_000L

        val Factory = viewModelFactory {
            initializer {
                val container = this.appContainer
                val handle = createSavedStateHandle()
                val retailerId = handle.get<String>(ARG_RETAILER)
                val retailer = retailerId?.let(container.retailerRegistry::byId)
                    ?: container.retailerRegistry.retailers.first()
                BrowserViewModel(retailer, handle, container.couponRepository, container.coordinator, container.lastSessionStore)
            }
        }
    }
}
