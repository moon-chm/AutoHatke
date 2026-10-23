package io.github.autocoupons.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Shared gradients so heroes, buttons and the assistant bubble read as one brand. */
object AcBrushes {
    val Hero: Brush = Brush.linearGradient(
        0f to BrandColors.IndigoDeep,
        0.55f to BrandColors.Indigo,
        1f to BrandColors.Violet,
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    val Primary: Brush = Brush.horizontalGradient(listOf(BrandColors.Indigo, BrandColors.Violet))

    val Bubble: Brush = Brush.linearGradient(listOf(BrandColors.Indigo, BrandColors.Violet, BrandColors.Pink))

    val Success: Brush = Brush.linearGradient(listOf(BrandColors.SuccessText, BrandColors.Emerald))

    val Progress: Brush = Brush.horizontalGradient(listOf(BrandColors.Indigo, BrandColors.Violet, BrandColors.Pink))
}
