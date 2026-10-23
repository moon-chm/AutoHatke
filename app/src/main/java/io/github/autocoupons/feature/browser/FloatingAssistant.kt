package io.github.autocoupons.feature.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.Sizes
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class AssistantMode { IDLE, RUNNING, DONE }

/**
 * In-app floating coupon bubble drawn above the WebView (no system overlay permission).
 * It sits on the right edge, can be dragged vertically so it never has to cover a checkout
 * control, pulses gently while it waits, and shows a progress ring while a session runs.
 */
@Composable
fun FloatingAssistant(
    mode: AssistantMode,
    progress: Float,
    completed: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var yFraction by rememberSaveable { mutableFloatStateOf(0.18f) }
    var dragging by remember { mutableStateOf(false) }
    var hintShown by rememberSaveable { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f)) }
    LaunchedEffect(mode) {
        if (mode == AssistantMode.IDLE && !hintShown) {
            hintShown = true
            delay(600)
            showHint = true
            delay(4_500)
        }
        showHint = false
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val halo by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "halo",
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bubble = Sizes.assistantBubble
        val margin = 14.dp
        val maxY = with(density) { (maxHeight - bubble - margin * 2).toPx() }.coerceAtLeast(0f)
        val x = with(density) { (maxWidth - bubble - margin).toPx() }
        val y = with(density) { margin.toPx() } + maxY * yFraction
        var hintWidth by remember { mutableFloatStateOf(0f) }

        AnimatedVisibility(
            visible = showHint && mode == AssistantMode.IDLE,
            enter = fadeIn() + slideInHorizontally { it / 3 },
            exit = fadeOut(),
            modifier = Modifier.offset {
                IntOffset(
                    (x - hintWidth - with(density) { 10.dp.toPx() }).roundToInt(),
                    (y + with(density) { (bubble / 2 - 20.dp).toPx() }).roundToInt(),
                )
            },
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandColors.Shelf,
                shadowElevation = 6.dp,
                modifier = Modifier.widthIn(max = 220.dp).onSizeChanged { hintWidth = it.width.toFloat() },
            ) {
                Text(
                    "Tap to apply the best coupon",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }

        val description = when (mode) {
            AssistantMode.IDLE -> "AutoCoupons: find and apply the best coupon"
            AssistantMode.RUNNING -> "AutoCoupons: trying coupons, $completed of $total. Open progress"
            AssistantMode.DONE -> "AutoCoupons: results ready. Open results"
        }

        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(bubble)
                .scale(entrance.value * if (dragging) 1.08f else 1f)
                .testTag("assistant_bubble")
                .semantics {
                    contentDescription = description
                    role = Role.Button
                    onClick { onClick(); true }
                }
                .pointerInput(maxY) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, drag ->
                        change.consume()
                        if (maxY > 0f) yFraction = (yFraction + drag.y / maxY).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(onClick) {
                    detectTapGestures(onTap = { onClick() })
                },
            contentAlignment = Alignment.Center,
        ) {
            if (mode == AssistantMode.IDLE) {
                Box(
                    Modifier
                        .size(bubble)
                        .graphicsLayer {
                            val s = 1f + halo * 0.45f
                            scaleX = s
                            scaleY = s
                            alpha = (1f - halo) * 0.45f
                        }
                        .clip(CircleShape)
                        .background(AcBrushes.Bubble),
                )
            }
            Box(
                Modifier
                    .size(bubble)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AcBrushes.Bubble),
            )
            when (mode) {
                AssistantMode.RUNNING -> {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0.02f, 1f) },
                        modifier = Modifier.size(bubble - 6.dp),
                        color = BrandColors.Highlight,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        strokeWidth = 3.dp,
                        gapSize = 0.dp,
                    )
                    Text("$completed/$total", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                else -> Icon(AcIcons.LocalOffer, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            if (mode == AssistantMode.DONE) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(BrandColors.Highlight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = BrandColors.IndigoDarkest, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
