package io.github.autocoupons.domain.coupon

/** A theoretical discount computed from structured offer metadata, for display only. */
sealed interface AdvertisedEstimate {
    data class Discount(val amount: Money) : AdvertisedEstimate
    data class BelowMinimumSpend(val minimumSpend: Money) : AdvertisedEstimate
    data object FreeShipping : AdvertisedEstimate
    data object Unknown : AdvertisedEstimate
}

/**
 * Estimates what a coupon *claims* it would save on a cart subtotal. Results are labelled
 * "advertised" in the UI and are never used for ranking: the retailer's checkout is the only
 * source of truth for real savings.
 */
object AdvertisedDiscountEstimator {

    fun estimate(offer: AdvertisedOffer?, subtotal: Money): AdvertisedEstimate {
        if (offer == null) return AdvertisedEstimate.Unknown
        val minimum = offer.minimumSpend
        if (minimum != null) {
            if (!minimum.sameCurrency(subtotal)) return AdvertisedEstimate.Unknown
            if (subtotal < minimum) return AdvertisedEstimate.BelowMinimumSpend(minimum)
        }
        return when (offer.type) {
            CouponType.FLAT -> {
                val flat = offer.flatAmount ?: return AdvertisedEstimate.Unknown
                if (!flat.sameCurrency(subtotal) || !flat.isPositive()) return AdvertisedEstimate.Unknown
                AdvertisedEstimate.Discount(Money(minOf(flat.minor, subtotal.minor), subtotal.currency))
            }
            CouponType.PERCENTAGE -> {
                val percent = offer.percent ?: return AdvertisedEstimate.Unknown
                if (percent !in 1..100) return AdvertisedEstimate.Unknown
                var minor = subtotal.minor * percent / 100
                val cap = offer.maximumDiscount
                if (cap != null) {
                    if (!cap.sameCurrency(subtotal)) return AdvertisedEstimate.Unknown
                    minor = minOf(minor, cap.minor)
                }
                AdvertisedEstimate.Discount(Money(minor, subtotal.currency))
            }
            CouponType.FREE_SHIPPING -> AdvertisedEstimate.FreeShipping
            CouponType.UNKNOWN -> AdvertisedEstimate.Unknown
        }
    }
}
