package io.github.autocoupons.core.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/** Events the shopping browser needs from WebView, free of WebView types where possible. */
interface ShoppingWebViewListener {
    fun onPageStarted(url: String)
    fun onPageFinished(url: String, canGoBack: Boolean, canGoForward: Boolean)
    fun onUrlChanged(url: String, canGoBack: Boolean, canGoForward: Boolean)
    fun onProgress(progress: Int)
    fun onTitle(title: String?)
    fun onMainFrameError(title: String, detail: String)
    fun onNavigationRefused(decision: NavigationDecision)
    fun onDownloadBlocked()
    fun onRendererGone()
}

/**
 * Builds a WebView with conservative settings, following the Android WebView security guidance:
 * no file/content access, no mixed content, no pop-up windows, no geolocation, Safe Browsing on,
 * https-only main-frame navigation within an allowlist, and TLS errors always cancelled.
 */
object SecureWebView {

    @SuppressLint("SetJavaScriptEnabled") // Retailer checkouts are JavaScript applications.
    fun create(context: Context, allowedHosts: Set<String>, listener: ShoppingWebViewListener): WebView {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                setGeolocationEnabled(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                safeBrowsingEnabled = true
                mediaPlaybackRequiresUserGesture = true
            }
            setDownloadListener { _, _, _, _, _ -> listener.onDownloadBlocked() }
            webViewClient = Client(assetLoader, allowedHosts, listener)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) = listener.onProgress(newProgress)
                override fun onReceivedTitle(view: WebView, title: String?) = listener.onTitle(title)
            }
        }
    }

    /** Fully releases a WebView that will not be shown again. */
    fun destroy(webView: WebView) {
        webView.stopLoading()
        webView.setDownloadListener(null)
        webView.webChromeClient = null
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeAllViews()
        webView.destroy()
    }

    private class Client(
        private val assetLoader: WebViewAssetLoader,
        private val allowedHosts: Set<String>,
        private val listener: ShoppingWebViewListener,
    ) : WebViewClient() {

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val decision = NavigationPolicy.evaluate(request.url.toString(), allowedHosts)
            if (decision == NavigationDecision.Allow) return false
            listener.onNavigationRefused(decision)
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = listener.onPageStarted(url)

        override fun onPageFinished(view: WebView, url: String) =
            listener.onPageFinished(url, view.canGoBack(), view.canGoForward())

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) =
            listener.onUrlChanged(url, view.canGoBack(), view.canGoForward())

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            val (title, detail) = when (error.errorCode) {
                ERROR_HOST_LOOKUP, ERROR_CONNECT -> "You're offline" to "Check your internet connection and try again."
                ERROR_TIMEOUT -> "The page took too long" to "The store did not respond in time."
                else -> "Couldn't load this page" to (error.description?.toString() ?: "Unknown error")
            }
            listener.onMainFrameError(title, detail)
        }

        /** Never proceed on certificate errors. */
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            listener.onMainFrameError(
                "Connection not secure",
                "The site's security certificate could not be verified, so the page was blocked.",
            )
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            listener.onRendererGone()
            return true // The host destroys this WebView and offers to create a new one.
        }
    }
}
