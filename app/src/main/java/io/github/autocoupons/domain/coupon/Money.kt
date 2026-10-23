package io.github.autocoupons.domain.coupon

import java.util.Locale

/**
 * An amount in minor units (paise for INR). Integer arithmetic avoids floating-point rounding
 * in discount comparisons; operations across currencies are rejected instead of guessed.
 */
data class Money(val minor: Long, val currency: String) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(minor + requireSame(other).minor, currency)

    operator fun minus(other: Money): Money = Money(minor - requireSame(other).minor, currency)

    override fun compareTo(other: Money): Int = minor.compareTo(requireSame(other).minor)

    fun isPositive(): Boolean = minor > 0

    fun sameCurrency(other: Money): Boolean = currency == other.currency

    private fun requireSame(other: Money): Money {
        require(sameCurrency(other)) { "Cannot combine $currency with ${other.currency}" }
        return other
    }

    /** "₹2,499" or "₹2,499.50" using Indian digit grouping for INR. */
    fun format(): String {
        val negative = minor < 0
        val abs = kotlin.math.abs(minor)
        val major = abs / 100
        val fraction = abs % 100
        val grouped = if (currency == INR) indianGrouping(major) else westernGrouping(major)
        val symbol = if (currency == INR) "₹" else "$currency "
        val decimals = if (fraction == 0L) "" else String.format(Locale.ROOT, ".%02d", fraction)
        return (if (negative) "−" else "") + symbol + grouped + decimals
    }

    override fun toString(): String = format()

    companion object {
        const val INR = "INR"

        fun inr(rupees: Long): Money = Money(rupees * 100, INR)

        fun zero(currency: String): Money = Money(0, currency)

        private fun westernGrouping(value: Long): String = String.format(Locale.ROOT, "%,d", value)

        private fun indianGrouping(value: Long): String {
            val digits = value.toString()
            if (digits.length <= 3) return digits
            val lastThree = digits.takeLast(3)
            val rest = digits.dropLast(3)
            val groups = rest.reversed().chunked(2).joinToString(",").reversed()
            return "$groups,$lastThree"
        }
    }
}
