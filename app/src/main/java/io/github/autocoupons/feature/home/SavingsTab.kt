package io.github.autocoupons.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BagArt
import io.github.autocoupons.core.designsystem.ConfettiBackdrop
import io.github.autocoupons.core.designsystem.CouponCodeStyle
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.components.AcPrimaryButton
import io.github.autocoupons.core.designsystem.components.StoreLogo
import java.text.DateFormat
import java.util.Date

@Composable
fun SavingsTab(state: HomeUiState, onStartShopping: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SavingsHeader(state) }
        if (state.history.isEmpty()) {
            item { EmptySavings(onStartShopping) }
        } else {
            item {
                Text(
                    "Recent checks",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen, vertical = Spacing.sm)
                        .semantics { heading() },
                )
            }
            items(state.history, key = { it.finishedAtMillis }) { SessionRow(it) }
        }
    }
}

@Composable
private fun SavingsHeader(state: HomeUiState) {
    val total = state.totalSaved
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(AcBrushes.Hero),
    ) {
        if (total != null) ConfettiBackdrop(Modifier.matchParentSize())
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        ) {
            Text("Your savings", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(Spacing.sm))
            Text(
                total?.format() ?: "₹0",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.semantics { heading() },
            )
            val checks = state.history.size
            Text(
                when (checks) {
                    0 -> "Your first saving is one checkout away"
                    1 -> "From 1 coupon check"
                    else -> "From $checks coupon checks"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun EmptySavings(onStartShopping: () -> Unit) {
    Column(
        Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xl).widthIn(max = 420.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        BagArt(Modifier.size(150.dp))
        Text("No savings yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Open a store, add a few things to your bag and tap the coupon bubble at checkout.",
            style = MaterialTheme.typography.bodyMedium,
            color = AcTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        AcPrimaryButton("Start shopping", onClick = onStartShopping)
    }
}

@Composable
private fun SessionRow(summary: LastSessionSummary) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AcTheme.colors.border.copy(alpha = 0.6f)),
        modifier = Modifier.padding(horizontal = Spacing.screen).widthIn(max = 600.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            StoreLogo(summary.retailerId, size = 46.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(summary.retailerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(summary.finishedAtMillis))} · ${summary.tested} coupons tried",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcTheme.colors.textTertiary,
                )
                summary.winnerCode?.let {
                    Text(it, style = CouponCodeStyle.copy(fontSize = CouponCodeStyle.fontSize * 0.85f), color = MaterialTheme.colorScheme.primary)
                }
            }
            val saved = summary.confirmedSavings
            Column(horizontalAlignment = Alignment.End) {
                if (saved != null) {
                    Text("−${saved.format()}", style = MaterialTheme.typography.titleMedium, color = AcTheme.colors.success)
                    Text("saved", style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textTertiary)
                } else {
                    Text(
                        if (summary.savings != null) "Unconfirmed" else "No saving",
                        style = MaterialTheme.typography.labelMedium,
                        color = AcTheme.colors.textTertiary,
                    )
                }
            }
        }
    }
}
