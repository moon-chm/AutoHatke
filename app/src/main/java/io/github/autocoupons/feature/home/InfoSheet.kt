package io.github.autocoupons.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.autocoupons.BuildConfig
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.components.BrandMark

enum class InfoTopic { HOW_IT_WORKS, PRIVACY, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(topic: InfoTopic, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (topic) {
                InfoTopic.HOW_IT_WORKS -> {
                    Title("How AutoCoupons works")
                    NumberedPoint(1, "Shop as usual", "Open a store from Home and add what you like to your bag.")
                    NumberedPoint(2, "Tap the coupon bubble", "It appears on the bag page. We fetch the store's coupons and try each one on your bag.")
                    NumberedPoint(3, "Keep the biggest saving", "Only discounts the store actually shows count. The winner is applied again and double-checked.")
                    NumberedPoint(4, "Stores without auto-apply", "Some stores need you to apply codes yourself. We list them so you can copy one in a tap.")
                }
                InfoTopic.PRIVACY -> {
                    Title("Privacy & security")
                    Point("Your password never leaves this phone. It isn't stored or sent anywhere.")
                    Point("Your account and savings history live on this device only, and clear when the app closes.")
                    Point("AutoCoupons only touches the coupon box. It never places orders or makes payments.")
                    Point("Store pages load over secure connections only. Pages with certificate problems are blocked.")
                }
                InfoTopic.ABOUT -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        BrandMark(size = 48.dp)
                        Column {
                            Title("AutoCoupons")
                            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textTertiary)
                        }
                    }
                    Point("An independent project built for the Buyhatke Android Developer assignment. It isn't affiliated with or endorsed by Buyhatke.")
                    Point("Modara is a sample store built into the app so you can watch coupons being tested and applied end to end. It doesn't sell anything.")
                    Point("Myntra coupons come from Buyhatke's public coupon feed. Myntra needs your own login, so you apply those codes yourself.")
                }
            }
        }
    }
}

@Composable
private fun Title(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })

@Composable
private fun NumberedPoint(number: Int, title: String, body: String) {
    Row {
        Box(Modifier.size(32.dp).clip(CircleShape).background(AcBrushes.Primary), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Point(text: String) {
    Row {
        Box(
            Modifier.padding(top = 7.dp).size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(2.dp))
}
