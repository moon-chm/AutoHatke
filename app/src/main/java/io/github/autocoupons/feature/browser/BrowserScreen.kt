package io.github.autocoupons.feature.browser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.components.StoreLogo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.SystemBarIcons
import io.github.autocoupons.core.designsystem.components.AcPrimaryButton
import io.github.autocoupons.core.designsystem.components.AcSecondaryButton
import io.github.autocoupons.core.designsystem.components.StateMessage
import io.github.autocoupons.core.web.NavigationDecision
import io.github.autocoupons.core.web.NavigationPolicy
import io.github.autocoupons.core.web.SecureWebView
import io.github.autocoupons.core.web.ShoppingWebViewListener
import io.github.autocoupons.core.web.WebPageBridge
import io.github.autocoupons.data.checkout.DemoStoreCheckoutAdapter
import io.github.autocoupons.data.checkout.RetailerConfig
import io.github.autocoupons.data.checkout.RetailerRegistry
import io.github.autocoupons.data.coupon.DemoCouponProvider
import io.github.autocoupons.domain.coupon.SessionPhase

/** WebView plus the objects whose lifetime is tied to it. */
private class BrowserHandles(
    val webView: WebView,
    val bridge: WebPageBridge?,
    val adapter: DemoStoreCheckoutAdapter?,
    val bridgeAttached: Boolean,
)

/** Forwards WebView events to the ViewModel and invalidates bridge calls when a new page starts. */
private class BrowserWebViewListener(
    private val viewModel: BrowserViewModel,
    private val bridge: WebPageBridge?,
) : ShoppingWebViewListener {
    var adapter: DemoStoreCheckoutAdapter? = null

    override fun onPageStarted(url: String) {
        bridge?.onNavigation()
        viewModel.onPageStarted(url)
    }

    override fun onPageFinished(url: String, canGoBack: Boolean, canGoForward: Boolean) =
        viewModel.onPageFinished(url, canGoBack, canGoForward, adapter)

    override fun onUrlChanged(url: String, canGoBack: Boolean, canGoForward: Boolean) =
        viewModel.onUrlChanged(url, canGoBack, canGoForward)

    override fun onProgress(progress: Int) = viewModel.onProgress(progress)
    override fun onTitle(title: String?) = viewModel.onTitle(title)
    override fun onMainFrameError(title: String, detail: String) = viewModel.onMainFrameError(title, detail)
    override fun onNavigationRefused(decision: NavigationDecision) = viewModel.onNavigationRefused(decision)
    override fun onDownloadBlocked() = viewModel.onDownloadBlocked()
    override fun onRendererGone() = viewModel.onRendererGone()
}

@Composable
fun BrowserScreen(
    onClose: () -> Unit,
    viewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val retailer = state.retailer
    val snackbar = remember { SnackbarHostState() }
    SystemBarIcons(onDarkBackground = false)
    // A new generation is created only after the renderer process died and the user chose to reload.
    val handles = remember(state.webViewGeneration) {
        val bridge = if (retailer.id == DemoCouponProvider.DEMO_STORE_ID) WebPageBridge(RetailerRegistry.DEMO_ORIGIN) else null
        val listener = BrowserWebViewListener(viewModel, bridge)
        val webView = SecureWebView.create(context, retailer.allowedHosts, listener)
        val attached = bridge?.attach(webView) ?: false
        val adapter = if (bridge != null && attached) DemoStoreCheckoutAdapter(bridge) else null
        listener.adapter = adapter
        BrowserHandles(webView, bridge, adapter, attached)
    }

    LaunchedEffect(handles) {
        if (handles.bridge != null) viewModel.onBridgeSupport(handles.bridgeAttached)
        if (handles.webView.url == null) handles.webView.loadUrl(viewModel.initialUrl())
    }
    DisposableEffect(handles) {
        onDispose {
            handles.bridge?.detach()
            SecureWebView.destroy(handles.webView)
        }
    }

    val notice = state.notice
    val latestContext by rememberUpdatedState(context)
    LaunchedEffect(notice) {
        if (notice == null) return@LaunchedEffect
        viewModel.consumeNotice()
        val result = snackbar.showSnackbar(
            message = notice.message,
            actionLabel = if (notice.externalUrl != null) "Open in browser" else null,
            withDismissAction = notice.externalUrl == null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed && notice.externalUrl != null) {
            try {
                latestContext.startActivity(Intent(Intent.ACTION_VIEW, notice.externalUrl.toUri()))
            } catch (e: ActivityNotFoundException) {
                snackbar.showSnackbar("No browser app is available to open this link.")
            }
        }
    }

    BackHandler {
        when {
            state.sheetMode != null -> viewModel.dismissSheet()
            !state.sessionRunning && handles.webView.canGoBack() -> handles.webView.goBack()
            else -> {
                viewModel.cancelSession()
                onClose()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BrowserTopBar(
                retailer = retailer,
                url = state.url,
                onClose = {
                    viewModel.cancelSession()
                    onClose()
                },
            )
        },
        bottomBar = {
            BrowserBottomBar(
                state = state,
                onBack = { handles.webView.goBack() },
                onForward = { handles.webView.goForward() },
                onReload = { handles.webView.reload() },
                onFindCoupons = { viewModel.onAssistantRequested(handles.adapter) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = { handles.webView }, modifier = Modifier.fillMaxSize().testTag("shopping_webview"))

            if (state.loading) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    drawStopIndicator = {},
                )
            }

            if (state.assistantVisible) {
                val session = state.session
                FloatingAssistant(
                    mode = when {
                        state.sessionRunning -> AssistantMode.RUNNING
                        session?.phase == SessionPhase.COMPLETED -> AssistantMode.DONE
                        else -> AssistantMode.IDLE
                    },
                    progress = session?.progress ?: 0f,
                    completed = session?.completed ?: 0,
                    total = session?.total ?: 0,
                    onClick = {
                        if (session?.phase == SessionPhase.COMPLETED && state.sheetMode == null) viewModel.showSheet(SheetMode.SESSION)
                        else viewModel.onAssistantRequested(handles.adapter)
                    },
                )
            }

            state.pageError?.let { error ->
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StateMessage(
                        icon = Icons.Filled.Warning,
                        title = error.title,
                        body = error.detail,
                        iconTint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.xxl),
                    ) {
                        AcPrimaryButton("Try again", onClick = {
                            if (viewModel.retry()) handles.webView.reload()
                        })
                        AcSecondaryButton("Close browser", onClick = onClose)
                    }
                }
            }
        }
    }

    if (state.sheetMode != null) {
        CouponSheet(
            state = state,
            onDismiss = viewModel::dismissSheet,
            onCancel = viewModel::cancelSession,
            onRetry = { handles.adapter?.let(viewModel::startSession) },
            onRetryAssisted = viewModel::loadAssistedCoupons,
            onOpenCheckout = {
                viewModel.dismissSheet()
                val cartUrl = when (retailer.id) {
                    DemoCouponProvider.DEMO_STORE_ID -> RetailerRegistry.DEMO_ORIGIN + RetailerRegistry.DEMO_CART_PATH
                    else -> "https://www.myntra.com/checkout/cart"
                }
                handles.webView.loadUrl(cartUrl)
            },
        )
    }
}

@Composable
private fun BrowserTopBar(retailer: RetailerConfig, url: String?, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(60.dp)
                .padding(end = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close store") }
            StoreLogo(retailer.id, size = 36.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(retailer.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Secure connection",
                        modifier = Modifier.size(11.dp),
                        tint = AcTheme.colors.success,
                    )
                    Text(
                        " " + (if (retailer.isDemo) "Secure checkout" else NavigationPolicy.hostOf(url) ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = AcTheme.colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserBottomBar(
    state: BrowserUiState,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onFindCoupons: () -> Unit,
) {
    val navEnabled = !state.sessionRunning
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = navEnabled && state.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = onForward, enabled = navEnabled && state.canGoForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
            }
            IconButton(onClick = onReload, enabled = navEnabled) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reload")
            }
            Box(Modifier.weight(1f))
            // The floating bubble is the entry point on a detected checkout; this pill covers other pages.
            if (!state.assistantVisible || state.sessionRunning) Row(
                Modifier
                    .padding(end = Spacing.sm)
                    .clip(CircleShape)
                    .background(AcBrushes.Primary)
                    .clickable(role = Role.Button, onClick = onFindCoupons)
                    .padding(horizontal = Spacing.lg, vertical = 10.dp)
                    .testTag("find_coupons"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(AcIcons.LocalOffer, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(if (state.sessionRunning) "Checking…" else "Coupons", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}
