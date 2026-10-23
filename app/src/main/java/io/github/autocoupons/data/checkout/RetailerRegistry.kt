package io.github.autocoupons.data.checkout

import io.github.autocoupons.core.web.NavigationPolicy
import io.github.autocoupons.data.coupon.DemoCouponProvider
import io.github.autocoupons.domain.coupon.AutomationSupport
import io.github.autocoupons.domain.coupon.CheckoutDetection
import io.github.autocoupons.domain.coupon.CheckoutDetector
import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.PageKind
import java.net.URI
import java.net.URISyntaxException

data class RetailerConfig(
    val id: String,
    val displayName: String,
    val summary: String,
    val startUrl: String,
    val allowedHosts: Set<String>,
    val couponSource: CouponSource,
    val automation: AutomationSupport,
    val detector: CheckoutDetector,
    val isDemo: Boolean,
)

/** Retailer integrations. Each one is added independently with its own verified URL patterns. */
class RetailerRegistry(val retailers: List<RetailerConfig> = DEFAULT) {

    fun byId(id: String): RetailerConfig? = retailers.firstOrNull { it.id == id }

    companion object {
        const val DEMO_ORIGIN = "https://appassets.androidplatform.net"
        const val DEMO_HOST = "appassets.androidplatform.net"
        const val DEMO_PATH_PREFIX = "/assets/demo-store/"
        const val DEMO_CART_PATH = "/assets/demo-store/cart.html"

        /** Demo Store: the bag page is the coupon-entry page. */
        val DemoStoreDetector = CheckoutDetector { url ->
            val uri = parse(url) ?: return@CheckoutDetector CheckoutDetection.Unsupported
            if (uri.scheme != "https" || uri.host != DEMO_HOST || uri.path?.startsWith(DEMO_PATH_PREFIX) != true) {
                return@CheckoutDetector CheckoutDetection.Unsupported
            }
            CheckoutDetection(
                DemoCouponProvider.DEMO_STORE_ID,
                if (uri.path == DEMO_CART_PATH) PageKind.CHECKOUT else PageKind.OTHER,
            )
        }

        /**
         * Myntra: `/checkout/cart` is the bag page where coupons are applied (verified 2026-10-24;
         * the page title is "SHOPPING BAG"). Detection is URL-only: no script is injected into Myntra.
         */
        val MyntraDetector = CheckoutDetector { url ->
            val uri = parse(url) ?: return@CheckoutDetector CheckoutDetection.Unsupported
            val host = uri.host ?: return@CheckoutDetector CheckoutDetection.Unsupported
            if (uri.scheme != "https" || !NavigationPolicy.isAllowedHost(host, MYNTRA_HOSTS)) {
                return@CheckoutDetector CheckoutDetection.Unsupported
            }
            val path = uri.path.orEmpty().trimEnd('/')
            CheckoutDetection("myntra", if (path == "/checkout/cart") PageKind.CHECKOUT else PageKind.OTHER)
        }

        private val MYNTRA_HOSTS = setOf("myntra.com")

        val DEFAULT = listOf(
            RetailerConfig(
                id = DemoCouponProvider.DEMO_STORE_ID,
                // Fictional fashion store bundled with the app (served locally, never sells anything).
                displayName = "Modara",
                summary = "Fashion & lifestyle · coupons auto-applied",
                startUrl = "$DEMO_ORIGIN${DEMO_PATH_PREFIX}index.html",
                allowedHosts = setOf(DEMO_HOST),
                couponSource = CouponSource.DEMO_FIXTURE,
                automation = AutomationSupport.AUTOMATED,
                detector = DemoStoreDetector,
                isDemo = true,
            ),
            RetailerConfig(
                id = "myntra",
                displayName = "Myntra",
                summary = "Fashion · coupon codes to copy",
                startUrl = "https://www.myntra.com/",
                allowedHosts = MYNTRA_HOSTS,
                couponSource = CouponSource.LIVE_API,
                automation = AutomationSupport.USER_ASSISTED,
                detector = MyntraDetector,
                isDemo = false,
            ),
        )

        private fun parse(url: String): URI? = try {
            URI(url)
        } catch (e: URISyntaxException) {
            null
        }
    }
}
