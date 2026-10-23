package io.github.autocoupons.core.web

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.github.autocoupons.domain.coupon.CheckoutUnavailableException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * Request/response channel between Kotlin and one trusted origin's page script.
 *
 * Page → app messages use AndroidX [WebViewCompat.addWebMessageListener], which only injects the
 * `AutoCouponsBridge` object into main frames whose origin exactly matches [allowedOrigin]. No
 * `@JavascriptInterface` is exposed, so other sites (and iframes) cannot reach native code.
 * The WebView is held weakly and every pending call fails fast when the page navigates away.
 */
class WebPageBridge(private val allowedOrigin: String) {

    private var webViewRef: WeakReference<WebView>? = null
    private val pending = HashMap<String, CompletableDeferred<JsonObject>>()
    private val json = Json { ignoreUnknownKeys = true }

    val isSupported: Boolean get() = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)

    /** Must be called on the main thread before the first page load. Returns false if unsupported. */
    @SuppressLint("RequiresFeature")
    fun attach(webView: WebView): Boolean {
        if (!isSupported) return false
        WebViewCompat.addWebMessageListener(webView, BRIDGE_NAME, setOf(allowedOrigin)) { _, message, origin, isMainFrame, _ ->
            if (isMainFrame && origin == allowedOrigin.toUri()) onMessage(message.data)
        }
        webViewRef = WeakReference(webView)
        return true
    }

    @SuppressLint("RequiresFeature")
    fun detach() {
        webViewRef?.get()?.let { if (isSupported) WebViewCompat.removeWebMessageListener(it, BRIDGE_NAME) }
        webViewRef = null
        failPending("The browser was closed")
    }

    /** Called when the main frame starts loading a new page: scripts on the old page can no longer reply. */
    fun onNavigation() = failPending("The page changed while a coupon was being tested")

    /**
     * Runs [script] (which receives a request id and must eventually post `{id, ...}` back) and
     * suspends until the page answers. Callers bound the wait with a timeout.
     */
    suspend fun call(script: (requestId: String) -> String): JsonObject = withContext(Dispatchers.Main.immediate) {
        val webView = webViewRef?.get() ?: throw CheckoutUnavailableException("The browser is not open")
        val currentOrigin = webView.url?.toUri()?.let { "${it.scheme}://${it.host}" }
        if (currentOrigin != allowedOrigin) throw CheckoutUnavailableException("The checkout page is no longer open")

        val id = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<JsonObject>()
        pending[id] = deferred
        try {
            webView.evaluateJavascript(script(id), null)
            deferred.await()
        } finally {
            pending.remove(id)
        }
    }

    private fun onMessage(data: String?) {
        val obj = try {
            data?.let { json.parseToJsonElement(it).jsonObject }
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } ?: return
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
        pending.remove(id)?.complete(obj)
    }

    private fun failPending(reason: String) {
        val calls = pending.values.toList()
        pending.clear()
        calls.forEach { it.completeExceptionally(CheckoutUnavailableException(reason)) }
    }

    companion object {
        const val BRIDGE_NAME = "AutoCouponsBridge"
    }
}
