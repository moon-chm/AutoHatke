package io.github.autocoupons.core.web

import java.net.URI
import java.net.URISyntaxException

sealed interface NavigationDecision {
    /** Load inside the shopping browser. */
    data object Allow : NavigationDecision

    /** A valid https page outside the retailer integration; offer to open it in the user's browser. */
    data class External(val url: String) : NavigationDecision

    /** Anything else (intent:, tel:, javascript:, http:, file:, malformed). Never acted on. */
    data class Block(val reason: String) : NavigationDecision
}

/** Decides which main-frame navigations the in-app browser may perform. Pure JVM for unit tests. */
object NavigationPolicy {

    fun evaluate(url: String, allowedHosts: Set<String>): NavigationDecision {
        val uri = try {
            URI(url.trim())
        } catch (e: URISyntaxException) {
            return NavigationDecision.Block("Malformed address")
        }
        val scheme = uri.scheme?.lowercase() ?: return NavigationDecision.Block("Missing scheme")
        if (scheme != "https") {
            return NavigationDecision.Block(
                if (scheme == "http") "Insecure (http) pages are not opened" else "Links of type \"$scheme:\" are not opened",
            )
        }
        val host = uri.host?.lowercase() ?: return NavigationDecision.Block("Missing host")
        if (uri.userInfo != null) return NavigationDecision.Block("Addresses containing credentials are not opened")
        return if (isAllowedHost(host, allowedHosts)) NavigationDecision.Allow else NavigationDecision.External(url)
    }

    fun isAllowedHost(host: String, allowedHosts: Set<String>): Boolean {
        val normalized = host.lowercase().trimEnd('.')
        return allowedHosts.any { allowed -> normalized == allowed || normalized.endsWith(".$allowed") }
    }

    fun hostOf(url: String?): String? = try {
        url?.let { URI(it).host?.lowercase() }
    } catch (e: URISyntaxException) {
        null
    }
}
