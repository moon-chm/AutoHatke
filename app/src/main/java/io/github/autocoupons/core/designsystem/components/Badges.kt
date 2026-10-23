package io.github.autocoupons.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.Spacing

/** Original AutoCoupons mark: coupon tag on the brand indigo with the brand-yellow accent. */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = BrandColors.Indigo,
    glyph: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(container)
            .semantics { contentDescription = "AutoCoupons" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(AcIcons.LocalOffer, contentDescription = null, tint = glyph, modifier = Modifier.size(size * 0.56f))
        Canvas(Modifier.size(size * 0.56f)) {
            drawCircle(BrandColors.Highlight, radius = this.size.minDimension * 0.085f, center = Offset(this.size.width * 0.23f, this.size.height * 0.23f))
        }
    }
}

/** Empty / error / unsupported state with optional actions. */
@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xl, horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = AcTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Column(
            Modifier.fillMaxWidth().padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { actions() }
    }
}
