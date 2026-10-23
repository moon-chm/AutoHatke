package io.github.autocoupons.data.coupon

import io.github.autocoupons.domain.coupon.AdvertisedOffer
import io.github.autocoupons.domain.coupon.Coupon
import io.github.autocoupons.domain.coupon.CouponSource
import io.github.autocoupons.domain.coupon.CouponType
import io.github.autocoupons.domain.coupon.Money
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface ParseResult {
    data class Parsed(val coupons: List<Coupon>, val skipped: Int) : ParseResult
    data class Invalid(val reason: String) : ParseResult
}

/**
 * Tolerant parser for the legacy `getCoupons.php` feed.
 *
 * The endpoint returned HTTP 404 when this app was built (docs/RESEARCH.md), so its schema could
 * not be observed. The parser therefore accepts the common shapes (a bare array, an array under
 * a wrapper key, or an object keyed by id) and several field-name aliases. Records without a
 * usable code are skipped and counted rather than guessed.
 */
class CouponResponseParser(private val json: Json = Json { isLenient = true }) {

    fun parse(body: String, retailerId: String, source: CouponSource): ParseResult {
        val trimmed = body.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return ParseResult.Invalid("Empty response")
        if (trimmed.startsWith("<")) return ParseResult.Invalid("Received HTML instead of JSON")

        val root = try {
            json.parseToJsonElement(trimmed)
        } catch (e: SerializationException) {
            return ParseResult.Invalid("Response is not valid JSON")
        }

        val records = findRecords(root) ?: return ParseResult.Invalid("No coupon list in response")
        var skipped = 0
        val coupons = records.mapNotNull { element ->
            val obj = element as? JsonObject
            val coupon = obj?.let { toCoupon(it, retailerId, source) }
            if (coupon == null) skipped++
            coupon
        }.distinctBy { it.code.uppercase() }

        if (coupons.isEmpty() && skipped > 0) {
            return ParseResult.Invalid("None of the $skipped records contained a coupon code")
        }
        return ParseResult.Parsed(coupons, skipped)
    }

    private fun findRecords(root: JsonElement, depth: Int = 0): List<JsonElement>? {
        if (depth > 2) return null
        return when (root) {
            is JsonArray -> root
            is JsonObject -> {
                WRAPPER_KEYS.firstNotNullOfOrNull { key -> root[key]?.let { findRecords(it, depth + 1) } }
                    ?: root.values.toList().takeIf { values -> values.isNotEmpty() && values.all { it is JsonObject } }
            }
            else -> null
        }
    }

    private fun toCoupon(obj: JsonObject, retailerId: String, source: CouponSource): Coupon? {
        val code = obj.string(CODE_KEYS)?.trim()?.takeIf { CODE_PATTERN.matches(it) } ?: return null
        val id = obj.string(ID_KEYS) ?: "$retailerId:${code.uppercase()}"
        return Coupon(
            id = id,
            code = code,
            retailerId = retailerId,
            source = source,
            title = obj.string(TITLE_KEYS)?.let(::cleanText),
            description = obj.string(DESCRIPTION_KEYS)?.let(::cleanText),
            advertised = advertisedOffer(obj),
            expiry = obj.string(EXPIRY_KEYS)?.let(::cleanText),
        )
    }

    /** Only structured numeric fields are used; amounts are assumed INR because both feeds are Indian. */
    private fun advertisedOffer(obj: JsonObject): AdvertisedOffer? {
        val type = when (obj.string(TYPE_KEYS)?.lowercase()?.trim()) {
            "flat", "fixed", "amount", "rs", "inr", "absolute" -> CouponType.FLAT
            "percent", "percentage", "%", "pct" -> CouponType.PERCENTAGE
            "free_shipping", "freeshipping", "shipping" -> CouponType.FREE_SHIPPING
            else -> return null
        }
        val value = obj.number(VALUE_KEYS)
        val cap = obj.number(CAP_KEYS)?.let(::rupees)
        val minimum = obj.number(MINIMUM_KEYS)?.let(::rupees)
        return when (type) {
            CouponType.FLAT -> AdvertisedOffer(type, flatAmount = value?.let(::rupees), minimumSpend = minimum)
            CouponType.PERCENTAGE -> AdvertisedOffer(
                type,
                percent = value?.toInt()?.takeIf { it in 1..100 },
                maximumDiscount = cap,
                minimumSpend = minimum,
            )
            else -> AdvertisedOffer(type, minimumSpend = minimum)
        }
    }

    private fun rupees(value: Double): Money? =
        if (value.isFinite() && value >= 0) Money(Math.round(value * 100), Money.INR) else null

    private fun JsonObject.string(keys: List<String>): String? = keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }
            ?.content?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.number(keys: List<String>): Double? = keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)?.content?.replace(",", "")?.trim()?.toDoubleOrNull()
    }

    private fun cleanText(raw: String): String? =
        raw.replace(TAG_PATTERN, " ").replace(WHITESPACE, " ").trim().take(MAX_TEXT).ifEmpty { null }

    private companion object {
        const val MAX_TEXT = 280
        val CODE_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{1,39}$")
        val TAG_PATTERN = Regex("<[^>]*>")
        val WHITESPACE = Regex("\\s+")
        val WRAPPER_KEYS = listOf("coupons", "data", "result", "results", "offers", "items")
        val CODE_KEYS = listOf("code", "coupon_code", "couponCode", "coupon", "promo_code", "promoCode", "voucher")
        val ID_KEYS = listOf("id", "coupon_id", "couponId")
        val TITLE_KEYS = listOf("title", "coupon_title", "name", "offer")
        val DESCRIPTION_KEYS = listOf("description", "desc", "details", "coupon_description", "terms")
        val EXPIRY_KEYS = listOf("expiry", "expiry_date", "expires", "valid_till", "validity", "end_date")
        val TYPE_KEYS = listOf("discount_type", "type", "offer_type")
        val VALUE_KEYS = listOf("discount_value", "discount", "value", "amount")
        val CAP_KEYS = listOf("max_discount", "maximum_discount", "cap", "upto")
        val MINIMUM_KEYS = listOf("min_order", "min_cart_value", "minimum_spend", "min_purchase", "min_amount")
    }
}
