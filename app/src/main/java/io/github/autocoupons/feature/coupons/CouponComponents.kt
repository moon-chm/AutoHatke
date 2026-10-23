package io.github.autocoupons.feature.coupons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.CouponCodeStyle
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.domain.coupon.Coupon
import io.github.autocoupons.domain.coupon.CouponTestResult
import io.github.autocoupons.domain.coupon.CouponTestStatus

private val StubWidth = 88.dp
private val NotchRadius = 9.dp

/** Rounded ticket with semicircular bites where the stub meets the body. */
class TicketShape(private val stubWidth: Dp = StubWidth, private val notch: Dp = NotchRadius, private val corner: Dp = 16.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val stub = with(density) { stubWidth.toPx() }
        val r = with(density) { notch.toPx() }
        val c = with(density) { corner.toPx() }
        val body = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(c))) }
        val bites = Path().apply {
            addOval(Rect(Offset(stub, 0f), r))
            addOval(Rect(Offset(stub, size.height), r))
        }
        return Outline.Generic(Path().apply { op(body, bites, PathOperation.Difference) })
    }
}

/** A coupon whose saving was observed on the bag. [best] marks the one that was applied. */
@Composable
fun WorkingCouponCard(result: CouponTestResult, best: Boolean, modifier: Modifier = Modifier) {
    val coupon = result.coupon
    val shape = remember { TicketShape() }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (best) 2.dp else 1.dp, if (best) MaterialTheme.colorScheme.primary else AcTheme.colors.border),
        shadowElevation = if (best) 4.dp else 0.dp,
        modifier = modifier.fillMaxWidth().testTag("coupon_${coupon.code}"),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Column(
                Modifier
                    .width(StubWidth)
                    .fillMaxHeight()
                    .background(if (best) AcBrushes.Primary else SolidColor(MaterialTheme.colorScheme.primaryContainer))
                    .padding(vertical = Spacing.lg, horizontal = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val stubText = if (best) Color.White else MaterialTheme.colorScheme.primary
                Text(result.observedSavings?.format() ?: "—", style = MaterialTheme.typography.titleMedium, color = stubText, textAlign = TextAlign.Center)
                Text("OFF", style = MaterialTheme.typography.labelSmall, color = stubText.copy(alpha = 0.8f))
            }
            DashedDivider()
            Column(
                Modifier.weight(1f).padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(coupon.code, style = CouponCodeStyle, modifier = Modifier.weight(1f))
                    if (best) {
                        Text(
                            "BEST",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandColors.IndigoDarkest,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BrandColors.Highlight).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                (coupon.title ?: coupon.description)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textSecondary, maxLines = 2)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(AcIcons.Verified, contentDescription = null, tint = AcTheme.colors.success, modifier = Modifier.size(14.dp))
                    Text(
                        result.observedTotal?.let { "Bag total ${it.format()}" } ?: "Works on this bag",
                        style = MaterialTheme.typography.labelMedium,
                        color = AcTheme.colors.success,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashedDivider() {
    val color = AcTheme.colors.border
    Canvas(Modifier.fillMaxHeight().width(1.dp).padding(vertical = NotchRadius + 2.dp)) {
        drawLine(
            color,
            Offset(0f, 0f),
            Offset(0f, size.height),
            strokeWidth = size.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}

/** Compact row for a coupon in progress or one that did not produce a confirmed saving. */
@Composable
fun CouponStatusRow(result: CouponTestResult, modifier: Modifier = Modifier) {
    val coupon = result.coupon
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(horizontal = Spacing.md, vertical = Spacing.md)
            .testTag("coupon_${coupon.code}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusGlyph(result.status)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(coupon.code, style = CouponCodeStyle.copy(fontSize = CouponCodeStyle.fontSize * 0.9f))
            val detail = when (result.status) {
                CouponTestStatus.REJECTED, CouponTestStatus.FAILED -> result.retailerMessage ?: result.failureReason?.userMessage
                CouponTestStatus.UNVERIFIED -> result.failureReason?.userMessage ?: "Saving couldn't be confirmed"
                else -> coupon.title ?: coupon.description
            }
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textSecondary, maxLines = 2) }
        }
        Spacer(Modifier.width(Spacing.sm))
        if (result.status == CouponTestStatus.APPLIED_AND_VERIFIED && result.observedSavings != null) {
            Text("−${result.observedSavings.format()}", style = MaterialTheme.typography.labelLarge, color = AcTheme.colors.success)
        } else {
            Text(CouponMessages.statusLabel(result.status), style = MaterialTheme.typography.labelMedium, color = AcTheme.colors.textTertiary)
        }
    }
}

@Composable
private fun StatusGlyph(status: CouponTestStatus) {
    val (bg, fg) = when (status) {
        CouponTestStatus.APPLIED_AND_VERIFIED -> AcTheme.colors.successContainer to AcTheme.colors.success
        CouponTestStatus.REJECTED, CouponTestStatus.CANCELLED, CouponTestStatus.DISCOVERED ->
            MaterialTheme.colorScheme.surface to AcTheme.colors.textTertiary
        CouponTestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        CouponTestStatus.UNVERIFIED -> AcTheme.colors.warningContainer to AcTheme.colors.warning
        CouponTestStatus.TESTING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }
    Box(Modifier.size(32.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        when (status) {
            CouponTestStatus.TESTING -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = fg)
            CouponTestStatus.APPLIED_AND_VERIFIED -> Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            CouponTestStatus.REJECTED, CouponTestStatus.FAILED -> Icon(Icons.Filled.Close, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            else -> Icon(AcIcons.LocalOffer, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
        }
    }
}

/** Coupon to copy, for stores where the shopper applies codes themselves. */
@Composable
fun AssistedCouponCard(coupon: Coupon, onCopy: (String) -> Unit) {
    var copied by remember(coupon.id) { mutableStateOf(false) }
    val shape = remember { TicketShape(stubWidth = 64.dp) }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AcTheme.colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(64.dp).fillMaxHeight().background(AcBrushes.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AcIcons.LocalOffer, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(coupon.code, style = CouponCodeStyle)
                (coupon.title ?: coupon.description)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textSecondary, maxLines = 2)
                }
                coupon.expiry?.let { Text("Valid till $it", style = MaterialTheme.typography.labelSmall, color = AcTheme.colors.textTertiary) }
            }
            Text(
                if (copied) "COPIED" else "COPY",
                style = MaterialTheme.typography.labelMedium,
                color = if (copied) AcTheme.colors.success else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = Spacing.md)
                    .clip(CircleShape)
                    .background(if (copied) AcTheme.colors.successContainer else MaterialTheme.colorScheme.primaryContainer)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Copy ${coupon.code}"
                    }
                    .clickable {
                        onCopy(coupon.code)
                        copied = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
