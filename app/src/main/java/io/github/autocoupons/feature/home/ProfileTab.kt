package io.github.autocoupons.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.autocoupons.BuildConfig
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.Spacing

@Composable
fun ProfileTab(
    state: HomeUiState,
    onCheckService: () -> Unit,
    onOpenInfo: (InfoTopic) -> Unit,
    onSignOut: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                Modifier.statusBarsPadding().padding(top = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(AcBrushes.Bubble), contentAlignment = Alignment.Center) {
                    Text(initials(state.session?.displayName), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(state.session?.displayName ?: "Guest", style = MaterialTheme.typography.titleLarge)
                state.session?.email?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary)
                }
            }
        }
        item {
            Group {
                val (subtitle, busy) = when (val api = state.liveApi) {
                    LiveApiStatus.NotChecked -> "Tap to check the coupon feed" to false
                    LiveApiStatus.Checking -> "Checking…" to true
                    is LiveApiStatus.Available -> "Online · ${api.couponCount} coupons" to false
                    is LiveApiStatus.Unavailable -> "Unavailable right now" to false
                }
                SettingRow(AcIcons.LocalOffer, "Coupon feed status", subtitle, onClick = onCheckService, trailing = {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else StatusDot(state.liveApi)
                })
                RowDivider()
                SettingRow(AcIcons.Verified, "How it works", "From bag to best coupon", onClick = { onOpenInfo(InfoTopic.HOW_IT_WORKS) })
                RowDivider()
                SettingRow(AcIcons.Shield, "Privacy & security", "What stays on your phone", onClick = { onOpenInfo(InfoTopic.PRIVACY) })
                RowDivider()
                SettingRow(Icons.Filled.Info, "About AutoCoupons", "Version ${BuildConfig.VERSION_NAME}", onClick = { onOpenInfo(InfoTopic.ABOUT) })
            }
        }
        item {
            Group {
                SettingRow(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    "Sign out",
                    null,
                    onClick = onSignOut,
                    tint = MaterialTheme.colorScheme.error,
                    showChevron = false,
                )
            }
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AcTheme.colors.border.copy(alpha = 0.6f)),
        modifier = Modifier.padding(horizontal = Spacing.screen).widthIn(max = 600.dp).fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(Modifier.padding(start = 68.dp), color = AcTheme.colors.border.copy(alpha = 0.6f))

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (showChevron) Color.Unspecified else tint)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textTertiary) }
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AcTheme.colors.textTertiary)
        }
    }
}

@Composable
private fun StatusDot(status: LiveApiStatus) {
    val color = when (status) {
        is LiveApiStatus.Available -> AcTheme.colors.success
        is LiveApiStatus.Unavailable -> MaterialTheme.colorScheme.error
        else -> return
    }
    Box(Modifier.padding(end = Spacing.sm).size(10.dp).clip(CircleShape).background(color))
}
