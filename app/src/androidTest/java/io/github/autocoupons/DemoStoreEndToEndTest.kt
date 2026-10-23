package io.github.autocoupons

import android.webkit.WebStorage
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.autocoupons.core.web.NavigationDecision
import io.github.autocoupons.core.web.SecureWebView
import io.github.autocoupons.core.web.ShoppingWebViewListener
import io.github.autocoupons.core.web.WebPageBridge
import io.github.autocoupons.data.checkout.DemoStoreCheckoutAdapter
import io.github.autocoupons.data.checkout.RetailerRegistry
import io.github.autocoupons.data.coupon.DefaultCouponRepository
import io.github.autocoupons.data.coupon.DemoCouponProvider
import io.github.autocoupons.domain.coupon.CouponApplicationCoordinator
import io.github.autocoupons.domain.coupon.CouponSessionState
import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.CouponTestStatus
import io.github.autocoupons.domain.coupon.DefaultCouponRankingEngine
import io.github.autocoupons.domain.coupon.Money
import io.github.autocoupons.domain.coupon.SessionOutcome
import io.github.autocoupons.domain.coupon.SessionPhase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the production coordinator and Demo Store adapter against the bundled checkout page in a
 * real WebView. Nothing here is faked: discounts come from the page's own coupon engine.
 */
@RunWith(AndroidJUnit4::class)
class DemoStoreEndToEndTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var webView: WebView
    private lateinit var bridge: WebPageBridge
    private lateinit var adapter: DemoStoreCheckoutAdapter
    private var pageFinished = CompletableDeferred<String>()

    private val coordinator = CouponApplicationCoordinator(
        DefaultCouponRepository(listOf(DemoCouponProvider())),
        DefaultCouponRankingEngine(),
    )

    private val listener = object : ShoppingWebViewListener {
        override fun onPageStarted(url: String) = bridge.onNavigation()
        override fun onPageFinished(url: String, canGoBack: Boolean, canGoForward: Boolean) {
            pageFinished.complete(url)
        }
        override fun onUrlChanged(url: String, canGoBack: Boolean, canGoForward: Boolean) = Unit
        override fun onProgress(progress: Int) = Unit
        override fun onTitle(title: String?) = Unit
        override fun onMainFrameError(title: String, detail: String) {
            pageFinished.completeExceptionally(AssertionError("$title: $detail"))
        }
        override fun onNavigationRefused(decision: NavigationDecision) = Unit
        override fun onDownloadBlocked() = Unit
        override fun onRendererGone() = Unit
    }

    @Before fun setUp() {
        instrumentation.runOnMainSync {
            WebStorage.getInstance().deleteAllData() // fresh default bag: sneakers + tee = ₹3,298
            bridge = WebPageBridge(RetailerRegistry.DEMO_ORIGIN)
            webView = SecureWebView.create(instrumentation.targetContext, setOf(RetailerRegistry.DEMO_HOST), listener)
            assertTrue("WebMessageListener unsupported", bridge.attach(webView))
            adapter = DemoStoreCheckoutAdapter(bridge)
        }
        loadCart()
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync {
            bridge.detach()
            SecureWebView.destroy(webView)
        }
    }

    private fun loadCart() = runBlocking {
        pageFinished = CompletableDeferred()
        instrumentation.runOnMainSync { webView.loadUrl(RetailerRegistry.DEMO_ORIGIN + RetailerRegistry.DEMO_CART_PATH) }
        withTimeout(15_000) { pageFinished.await() }
    }

    private fun runSession(): List<CouponSessionState> = runBlocking {
        withTimeout(60_000) { coordinator.run("demo-store", CouponSource.DEMO_FIXTURE, adapter).toList() }
    }

    @Test fun cartPageExposesCouponEntryAndDefaultBag() = runBlocking {
        assertTrue(adapter.hasCouponEntry())
        val cart = adapter.readCart()
        assertEquals(Money.inr(3_298), cart.subtotal)
        assertEquals(Money.inr(3_298), cart.total)
        assertNull(cart.appliedCode)
        assertEquals("sneakersx1|teex1", cart.fingerprint)
    }

    @Test fun happyPathSelectsStyle250ReappliesAndVerifiesTotal() {
        val states = runSession()
        val final = states.last()
        assertEquals(SessionPhase.COMPLETED, final.phase)

        val byCode = final.results.associateBy { it.coupon.code }
        assertEquals(Money.inr(100), byCode.getValue("FLAT100").observedSavings)
        assertEquals(Money.inr(200), byCode.getValue("SAVE10").observedSavings)
        assertEquals(Money.inr(250), byCode.getValue("STYLE250").observedSavings)
        assertEquals(Money.inr(150), byCode.getValue("SHOES15").observedSavings)
        assertEquals(CouponTestStatus.REJECTED, byCode.getValue("MEGA500").status)
        assertTrue(byCode.getValue("MEGA500").retailerMessage!!.contains("minimum ₹5,000"))
        assertEquals("This coupon expired on 31 Mar 2026", byCode.getValue("FESTIVE50").retailerMessage)
        assertEquals("Coupon code WELCOME75 is not valid", byCode.getValue("WELCOME75").retailerMessage)

        val outcome = final.outcome as SessionOutcome.WinnerVerified
        assertEquals("STYLE250", outcome.winner.coupon.code)
        assertEquals(Money.inr(3_048), outcome.finalCart.total)

        val cart = runBlocking { adapter.readCart() }
        assertEquals("STYLE250", cart.appliedCode)
        assertEquals(Money.inr(3_048), cart.total)
        assertEquals(Money.inr(250), cart.discount)

        // Progress reported real completed tests: 0..7, monotonically.
        val counts = states.map { it.completed }
        assertEquals(counts.sorted(), counts)
        assertEquals(7, counts.last())
    }

    @Test fun biggerBagMakesMega500TheWinner() {
        instrumentation.runOnMainSync { webView.evaluateJavascript("DemoStore.setQty('bag', 1)", null) }
        loadCart()
        val outcome = runSession().last().outcome as SessionOutcome.WinnerVerified
        assertEquals("MEGA500", outcome.winner.coupon.code)
        assertEquals(Money.inr(5_197 - 500), outcome.finalCart.total)
    }

    @Test fun repeatedSessionsAreStableAndDeterministic() {
        repeat(3) {
            val outcome = runSession().last().outcome as SessionOutcome.WinnerVerified
            assertEquals("STYLE250", outcome.winner.coupon.code)
            assertEquals(Money.inr(3_048), outcome.finalCart.total)
        }
    }

    @Test fun cancellingMidSessionRestoresTheBag() = runBlocking {
        val states = mutableListOf<CouponSessionState>()
        val job = launch { coordinator.run("demo-store", CouponSource.DEMO_FIXTURE, adapter).collect { states += it } }
        withTimeout(30_000) { while (states.lastOrNull()?.completed?.let { it < 2 } != false) delay(50) }
        job.cancel()
        job.join()
        val cart = adapter.readCart()
        assertNull(cart.appliedCode)
        assertEquals(Money.inr(3_298), cart.total)
    }

    @Test fun navigatingAwayFailsTheSessionInsteadOfHanging() = runBlocking {
        val states = mutableListOf<CouponSessionState>()
        val job = launch { coordinator.run("demo-store", CouponSource.DEMO_FIXTURE, adapter).collect { states += it } }
        withTimeout(30_000) { while (states.lastOrNull()?.completed?.let { it < 1 } != false) delay(50) }
        instrumentation.runOnMainSync { webView.loadUrl(RetailerRegistry.DEMO_ORIGIN + "/assets/demo-store/index.html") }
        withTimeout(30_000) { job.join() }
        assertEquals(SessionPhase.FAILED, states.last().phase)
    }
}
