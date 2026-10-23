package io.github.autocoupons.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Brand tokens. Every value is either sampled from an official Buyhatke asset or explicitly
 * marked as derived; docs/UI_DESIGN.md records the source of each one (verified 2026-10-24).
 */
object BrandColors {
    // Official logo SVG (www.buyhatke.com/_app/immutable/assets/logo.*.svg).
    val Indigo = Color(0xFF4F46E5)
    val IndigoDeep = Color(0xFF3932BF)
    val IndigoDarkest = Color(0xFF302B9E)
    val Highlight = Color(0xFFFFE372)

    // Inline styles on www.buyhatke.com.
    val IndigoTint = Color(0xFFEFEFFF)
    val IndigoSoft = Color(0xFFD0CFFF)
    val IndigoLight = Color(0xFF918FFF)
    val Background = Color(0xFFF1F5F9)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF020618)
    val TextSecondary = Color(0xFF45556C)
    val TextTertiary = Color(0xFF62748E)
    val Border = Color(0xFFCAD5E2)
    val Success = Color(0xFF008357)
    val SuccessContainer = Color(0xFFE8F8F0)
    val Warning = Color(0xFFFF6D1F)
    val WarningContainer = Color(0xFFFFF8E7)

    // Derived: no official equivalent, chosen to reach WCAG AA contrast on white.
    val WarningText = Color(0xFFA2440B)
    // Site green is 4.36:1 on SuccessContainer; this darker shade reaches 5.2:1 for small text.
    val SuccessText = Color(0xFF00754D)
    val Error = Color(0xFFC62828)
    val ErrorContainer = Color(0xFFFDECEC)

    // Derived accents for illustrations and hero gradients (indigo → violet → pink, as in the
    // Buyhatke app's campaign banners) and the dark "store shelf" card.
    val Violet = Color(0xFF7C3AED)
    val Pink = Color(0xFFEC4899)
    val Coin = Color(0xFFFFC83D)
    val CoinDeep = Color(0xFFF59E0B)
    val Shelf = Color(0xFF1C1E2B)
    val ShelfHigh = Color(0xFF2A2D3E)
    val Mint = Color(0xFF34D399)
    val Emerald = Color(0xFF10B981)

    // Derived dark-theme surfaces built around the official indigo hue.
    val DarkBackground = Color(0xFF0B0D1A)
    val DarkSurface = Color(0xFF151829)
    val DarkSurfaceHigh = Color(0xFF1E2238)
    val DarkBorder = Color(0xFF323854)
    val DarkTextPrimary = Color(0xFFF1F5F9)
    val DarkTextSecondary = Color(0xFFCAD5E2)
    val DarkTextTertiary = Color(0xFF9AA7BC)
    val DarkSuccess = Color(0xFF5BD6A6)
    val DarkSuccessContainer = Color(0xFF0E3326)
    val DarkWarning = Color(0xFFFFA36E)
    val DarkWarningContainer = Color(0xFF3A2410)
    val DarkError = Color(0xFFFF8A80)
    val DarkErrorContainer = Color(0xFF3B1414)
}
