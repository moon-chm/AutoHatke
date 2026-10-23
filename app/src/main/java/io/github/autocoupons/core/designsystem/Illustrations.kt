package io.github.autocoupons.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import kotlin.math.cos
import kotlin.math.sin

/** Coupon ticket outline: rounded rectangle with a semicircular notch on each side. */
fun ticketPath(size: Size, corner: Float = size.height * 0.14f, notch: Float = size.height * 0.13f): Path {
    val body = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner))) }
    val notches = Path().apply {
        addOval(Rect(Offset(0f, size.height / 2), notch))
        addOval(Rect(Offset(size.width, size.height / 2), notch))
    }
    return Path().apply { op(body, notches, PathOperation.Difference) }
}

/**
 * Hero artwork: two tilted coupon tickets, a couple of coins and sparkles. Drawn in code so it
 * scales crisply and costs no bitmap memory. Gently floats when [animate] is true.
 */
@Composable
fun CouponTicketsArt(modifier: Modifier = Modifier, animate: Boolean = true) {
    val measurer = rememberTextMeasurer()
    val float = if (animate) {
        val transition = rememberInfiniteTransition(label = "float")
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
            label = "bob",
        ).value
    } else 0f

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ticket = Size(w * 0.78f, h * 0.42f)

        // Back ticket.
        rotate(-14f, pivot = Offset(w * 0.5f, h * 0.42f)) {
            translate(w * 0.08f, h * 0.16f + float * 3f) {
                drawPath(ticketPath(ticket), Color.White.copy(alpha = 0.28f))
            }
        }
        // Front ticket.
        rotate(8f, pivot = Offset(w * 0.5f, h * 0.55f)) {
            translate(w * 0.14f, h * 0.36f - float * 4f) {
                drawPath(ticketPath(ticket), Color.White)
                val stub = ticket.width * 0.34f
                drawLine(
                    color = BrandColors.IndigoSoft,
                    start = Offset(stub, ticket.height * 0.16f),
                    end = Offset(stub, ticket.height * 0.84f),
                    strokeWidth = ticket.height * 0.035f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(ticket.height * 0.07f, ticket.height * 0.06f)),
                )
                centeredText(measurer, "%", Offset(stub / 2, ticket.height / 2), (ticket.height * 0.5f).toSp(), BrandColors.Indigo)
                val barX = stub + ticket.width * 0.09f
                val barW = ticket.width - barX - ticket.width * 0.12f
                drawRoundRect(BrandColors.Indigo, Offset(barX, ticket.height * 0.28f), Size(barW, ticket.height * 0.15f), CornerRadius(99f))
                drawRoundRect(BrandColors.IndigoSoft, Offset(barX, ticket.height * 0.53f), Size(barW * 0.7f, ticket.height * 0.11f), CornerRadius(99f))
                drawRoundRect(BrandColors.IndigoTint, Offset(barX, ticket.height * 0.71f), Size(barW * 0.45f, ticket.height * 0.09f), CornerRadius(99f))
            }
        }
        coin(measurer, Offset(w * 0.86f, h * 0.2f + float * 5f), w * 0.085f)
        coin(measurer, Offset(w * 0.12f, h * 0.86f - float * 4f), w * 0.06f)
        sparkle(Offset(w * 0.2f, h * 0.12f), w * 0.045f, Color.White)
        sparkle(Offset(w * 0.93f, h * 0.7f), w * 0.035f, BrandColors.Highlight)
        sparkle(Offset(w * 0.58f, h * 0.06f), w * 0.025f, Color.White.copy(alpha = 0.8f))
    }
}

/** Small celebratory confetti layer drawn behind the savings headline. */
@Composable
fun ConfettiBackdrop(modifier: Modifier = Modifier) {
    val pieces = remember {
        val colors = listOf(BrandColors.Highlight, Color.White, BrandColors.Mint, BrandColors.Pink, BrandColors.Coin)
        List(22) { i ->
            val angle = i * 137.5f
            Triple(
                // Keep pieces in the outer fifth on each side so they never cross the headline.
                Offset(
                    (((i * 37) % 100) / 100f) * 0.2f + if (i % 2 == 0) 0f else 0.8f,
                    ((i * 61 + 13) % 100) / 100f,
                ),
                colors[i % colors.size],
                angle,
            )
        }
    }
    Canvas(modifier) {
        pieces.forEachIndexed { i, (pos, color, angle) ->
            val center = Offset(pos.x * size.width, pos.y * size.height)
            rotate(angle, pivot = center) {
                if (i % 3 == 0) {
                    drawCircle(color.copy(alpha = 0.55f), radius = 3.5f * density, center = center)
                } else {
                    drawRoundRect(
                        color.copy(alpha = 0.6f),
                        topLeft = center - Offset(5f * density, 2f * density),
                        size = Size(10f * density, 4f * density),
                        cornerRadius = CornerRadius(2f * density),
                    )
                }
            }
        }
    }
}

/** Empty shopping bag with a tag, for empty and "go to bag" states. */
@Composable
fun BagArt(modifier: Modifier = Modifier, tint: Color = BrandColors.Indigo) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(tint.copy(alpha = 0.10f), radius = w * 0.48f, center = Offset(w / 2, h / 2))
        val bag = RoundRect(w * 0.27f, h * 0.34f, w * 0.73f, h * 0.8f, CornerRadius(w * 0.06f))
        drawPath(Path().apply { addRoundRect(bag) }, tint)
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.22f),
            size = Size(w * 0.24f, h * 0.24f),
            style = Stroke(width = w * 0.035f),
        )
        rotate(-18f, pivot = Offset(w * 0.68f, h * 0.66f)) {
            translate(w * 0.52f, h * 0.58f) {
                drawPath(ticketPath(Size(w * 0.3f, h * 0.16f)), BrandColors.Highlight)
            }
        }
        sparkle(Offset(w * 0.22f, h * 0.26f), w * 0.05f, BrandColors.Coin)
        sparkle(Offset(w * 0.8f, h * 0.3f), w * 0.035f, tint.copy(alpha = 0.5f))
    }
}

private fun DrawScope.coin(measurer: TextMeasurer, center: Offset, radius: Float) {
    drawCircle(BrandColors.CoinDeep, radius = radius, center = center + Offset(0f, radius * 0.12f))
    drawCircle(BrandColors.Coin, radius = radius, center = center)
    drawCircle(BrandColors.CoinDeep.copy(alpha = 0.55f), radius = radius * 0.72f, center = center, style = Stroke(radius * 0.1f))
    centeredText(measurer, "₹", center, (radius * 0.95f).toSp(), BrandColors.CoinDeep)
}

private fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 8) {
        val r = if (i % 2 == 0) radius else radius * 0.28f
        val a = Math.toRadians((i * 45 - 90).toDouble())
        val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.centeredText(measurer: TextMeasurer, text: String, center: Offset, size: TextUnit, color: Color) {
    val layout = measurer.measure(text, TextStyle(fontFamily = Jakarta, fontSize = size, fontWeight = FontWeight.ExtraBold, color = color, lineHeight = size * 1f))
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

