package io.github.autocoupons.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.autocoupons.R

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Plus Jakarta Sans (SIL OFL 1.1), bundled as one variable font. */
val Jakarta = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Float = 0f) = TextStyle(
    fontFamily = Jakarta,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

val AcTypography = Typography(
    displaySmall = style(34, 40, FontWeight.ExtraBold, -0.8f),
    headlineLarge = style(30, 36, FontWeight.ExtraBold, -0.6f),
    headlineMedium = style(26, 32, FontWeight.ExtraBold, -0.5f),
    headlineSmall = style(22, 28, FontWeight.Bold, -0.3f),
    titleLarge = style(20, 26, FontWeight.Bold, -0.2f),
    titleMedium = style(16, 22, FontWeight.Bold),
    titleSmall = style(14, 20, FontWeight.Bold),
    bodyLarge = style(16, 24, FontWeight.Medium),
    bodyMedium = style(14, 20, FontWeight.Medium),
    bodySmall = style(12, 16, FontWeight.Medium),
    labelLarge = style(15, 20, FontWeight.Bold),
    labelMedium = style(13, 16, FontWeight.SemiBold),
    labelSmall = style(11, 14, FontWeight.Bold, 0.3f),
)

/** Coupon codes: bold and tracked out so similar characters (0/O, 1/I) stay distinguishable. */
val CouponCodeStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    letterSpacing = 1.sp,
)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screen = 20.dp
}

object Sizes {
    val buttonHeight = 54.dp
    val minTouch = 48.dp
    val assistantBubble = 62.dp
}
