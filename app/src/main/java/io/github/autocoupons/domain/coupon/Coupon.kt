package io.github.autocoupons.domain.coupon

/** Where a coupon record came from. Shown to the user on every list so origins are never mixed. */
enum class CouponSource(val label: String) {
    LIVE_API("Live API"),
    DEMO_FIXTURE("Demo data"),
}

enum class CouponType { FLAT, PERCENTAGE, FREE_SHIPPING, UNKNOWN }

/**
 * What a coupon claims to offer, built only from structured fields in the source data.
 * It is never parsed out of free text and never used to pick a winner.
 */
data class AdvertisedOffer(
    val type: CouponType,
    val flatAmount: Money? = null,
    val percent: Int? = null,
    val maximumDiscount: Money? = null,
    val minimumSpend: Money? = null,
)

data class Coupon(
    val id: String,
    val code: String,
    val retailerId: String,
    val source: CouponSource,
    val title: String? = null,
    val description: String? = null,
    val advertised: AdvertisedOffer? = null,
    /** Raw expiry text exactly as supplied; not interpreted because no format is documented. */
    val expiry: String? = null,
)
