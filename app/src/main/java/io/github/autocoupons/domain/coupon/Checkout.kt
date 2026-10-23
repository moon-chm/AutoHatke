package io.github.autocoupons.domain.coupon

/** Cart amounts as read from the checkout page. */
data class CartSnapshot(
    val subtotal: Money,
    val discount: Money,
    val total: Money,
    val appliedCode: String?,
    /** Stable description of line items + quantities, used to detect cart edits mid-session. */
    val fingerprint: String,
) {
    val currency: String get() = subtotal.currency

    /** True when the page's own numbers agree with each other (total = subtotal − discount). */
    fun isInternallyConsistent(): Boolean =
        subtotal.sameCurrency(discount) && subtotal.sameCurrency(total) &&
            discount.minor >= 0 && total.minor >= 0 &&
            subtotal.minor - discount.minor == total.minor
}

/** What the retailer page did after a coupon code was submitted. */
sealed interface ApplyObservation {
    data class Accepted(val cart: CartSnapshot, val message: String?) : ApplyObservation
    data class Rejected(val cart: CartSnapshot, val message: String?) : ApplyObservation
    data object TimedOut : ApplyObservation
}

enum class AutomationSupport {
    /** The adapter can apply/remove codes itself and observe the result. */
    AUTOMATED,

    /** Codes can be listed, but the shopper must apply them on the retailer page. */
    USER_ASSISTED,
}

class CheckoutUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Retailer-specific checkout automation. Implementations must never click order-placement or
 * payment controls; they only touch the coupon field and read cart totals.
 */
interface CheckoutAdapter {
    val retailerId: String

    /** @throws CheckoutUnavailableException when the cart cannot be read. */
    suspend fun readCart(): CartSnapshot

    /** Submits [code] and waits (bounded) for the page to accept or reject it. */
    suspend fun applyCoupon(code: String): ApplyObservation

    /** Removes the currently applied coupon and returns the resulting cart. */
    suspend fun removeCoupon(): CartSnapshot
}

enum class PageKind { CHECKOUT, OTHER }

/** Result of matching a URL against a retailer integration. */
data class CheckoutDetection(
    val retailerId: String?,
    val pageKind: PageKind,
) {
    val isCheckout: Boolean get() = retailerId != null && pageKind == PageKind.CHECKOUT

    companion object {
        val Unsupported = CheckoutDetection(retailerId = null, pageKind = PageKind.OTHER)
    }
}

/** Retailer-specific checkout recognition based on verified URL patterns. */
fun interface CheckoutDetector {
    fun detect(url: String): CheckoutDetection
}
