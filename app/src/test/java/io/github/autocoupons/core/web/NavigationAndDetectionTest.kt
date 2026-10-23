package io.github.autocoupons.core.web

import io.github.autocoupons.data.checkout.RetailerRegistry
import io.github.autocoupons.domain.coupon.PageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    private val myntra = setOf("myntra.com")

    @Test fun `https on allowlisted host and subdomains is allowed`() {
        assertEquals(NavigationDecision.Allow, NavigationPolicy.evaluate("https://www.myntra.com/checkout/cart", myntra))
        assertEquals(NavigationDecision.Allow, NavigationPolicy.evaluate("https://myntra.com/", myntra))
    }

    @Test fun `look-alike hosts are not treated as the retailer`() {
        assertTrue(NavigationPolicy.evaluate("https://evilmyntra.com/", myntra) is NavigationDecision.External)
        assertTrue(NavigationPolicy.evaluate("https://myntra.com.evil.io/", myntra) is NavigationDecision.External)
    }

    @Test fun `non-https schemes and credentials are blocked`() {
        listOf(
            "http://www.myntra.com/",
            "intent://scan/#Intent;scheme=zxing;end",
            "javascript:alert(1)",
            "file:///sdcard/x.html",
            "tel:12345",
            "https://user:pass@www.myntra.com/",
            "not a url",
        ).forEach { assertTrue(it, NavigationPolicy.evaluate(it, myntra) is NavigationDecision.Block) }
    }
}

class CheckoutDetectorTest {
    @Test fun `demo store bag page is checkout, product page is not`() {
        val d = RetailerRegistry.DemoStoreDetector
        assertTrue(d.detect("https://appassets.androidplatform.net/assets/demo-store/cart.html").isCheckout)
        assertEquals(PageKind.OTHER, d.detect("https://appassets.androidplatform.net/assets/demo-store/index.html").pageKind)
        assertFalse(d.detect("https://appassets.androidplatform.net/assets/other/cart.html").isCheckout)
        assertFalse(d.detect("http://appassets.androidplatform.net/assets/demo-store/cart.html").isCheckout)
    }

    @Test fun `myntra cart url is checkout but the word checkout alone is not enough`() {
        val d = RetailerRegistry.MyntraDetector
        assertTrue(d.detect("https://www.myntra.com/checkout/cart").isCheckout)
        assertTrue(d.detect("https://www.myntra.com/checkout/cart/").isCheckout)
        assertFalse(d.detect("https://www.myntra.com/checkout/payment").isCheckout)
        assertFalse(d.detect("https://www.myntra.com/search?q=checkout").isCheckout)
        assertFalse(d.detect("https://example.com/checkout/cart").isCheckout)
    }
}
