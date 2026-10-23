package io.github.autocoupons.data.coupon

import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.CouponType
import io.github.autocoupons.domain.coupon.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The live endpoint returned HTTP 404 during development, so these fixtures are synthetic shapes
 * (see docs/RESEARCH.md §2). The real 404 body is used verbatim in [realNotFoundBody].
 */
class CouponResponseParserTest {

    private val parser = CouponResponseParser()
    private fun parse(body: String) = parser.parse(body, "myntra", CouponSource.LIVE_API)
    private fun parsed(body: String) = parse(body) as ParseResult.Parsed

    @Test fun `bare array with alias keys`() {
        val result = parsed(
            """[{"coupon_code":"MYNTRA200","coupon_title":"Flat ₹200 off","expiry":"2026-12-31"},
                {"code":"FASHION10","description":"<b>10%</b> off","discount_type":"percent","discount_value":"10","max_discount":"300"}]""",
        )
        assertEquals(listOf("MYNTRA200", "FASHION10"), result.coupons.map { it.code })
        assertEquals("Flat ₹200 off", result.coupons[0].title)
        assertEquals("2026-12-31", result.coupons[0].expiry)
        assertEquals("10% off", result.coupons[1].description)
        val offer = result.coupons[1].advertised!!
        assertEquals(CouponType.PERCENTAGE, offer.type)
        assertEquals(10, offer.percent)
        assertEquals(Money.inr(300), offer.maximumDiscount)
        assertTrue(result.coupons.all { it.source == CouponSource.LIVE_API && it.retailerId == "myntra" })
    }

    @Test fun `array under wrapper key`() {
        val result = parsed("""{"status":"ok","data":{"coupons":[{"couponCode":"A1"},{"promo_code":"B2"}]}}""")
        assertEquals(listOf("A1", "B2"), result.coupons.map { it.code })
    }

    @Test fun `object keyed by id`() {
        val result = parsed("""{"17":{"code":"X17","id":17},"18":{"code":"X18"}}""")
        assertEquals(listOf("X17", "X18"), result.coupons.map { it.code })
        assertEquals("17", result.coupons[0].id)
        assertEquals("myntra:X18", result.coupons[1].id)
    }

    @Test fun `missing optional fields are tolerated`() {
        val coupon = parsed("""[{"code":"ONLY"}]""").coupons.single()
        assertNull(coupon.title)
        assertNull(coupon.description)
        assertNull(coupon.advertised)
        assertNull(coupon.expiry)
    }

    @Test fun `records without a usable code are skipped and counted`() {
        val result = parsed("""[{"code":"GOOD1"},{"title":"no code"},{"code":""},{"code":"bad code!"},42,null]""")
        assertEquals(listOf("GOOD1"), result.coupons.map { it.code })
        assertEquals(5, result.skipped)
    }

    @Test fun `duplicate codes are collapsed case-insensitively`() {
        assertEquals(1, parsed("""[{"code":"SAVE"},{"code":"save"}]""").coupons.size)
    }

    @Test fun `unknown discount type yields no advertised offer`() {
        assertNull(parsed("""[{"code":"X1","type":"mystery","value":5}]""").coupons.single().advertised)
    }

    @Test fun `empty array is a valid empty list`() {
        assertEquals(0, parsed("[]").coupons.size)
    }

    @Test fun `real 404 html body is rejected as html`() {
        assertEquals(ParseResult.Invalid("Received HTML instead of JSON"), parse(realNotFoundBody))
    }

    @Test fun `malformed json and odd roots are invalid`() {
        assertTrue(parse("""[{"code":"A"""") is ParseResult.Invalid)
        assertTrue(parse("") is ParseResult.Invalid)
        assertTrue(parse("\"just a string\"") is ParseResult.Invalid)
        assertTrue(parse("""{"message":"no list here"}""") is ParseResult.Invalid)
        assertTrue(parse("""[{"title":"no codes at all"}]""") is ParseResult.Invalid)
    }

    companion object {
        /** Body returned by coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php?pos=1|2 on 2026-10-24. */
        val realNotFoundBody = """
            <html>
            <head><title>404 Not Found</title></head>
            <body>
            <center><h1>404 Not Found</h1></center>
            <hr><center>nginx/1.24.0 (Ubuntu)</center>
            </body>
            </html>
        """.trimIndent()
    }
}
