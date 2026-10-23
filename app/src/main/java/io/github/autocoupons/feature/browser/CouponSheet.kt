package io.github.autocoupons.feature.browser

import android.content.ClipData
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BagArt
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.ConfettiBackdrop
import io.github.autocoupons.core.designsystem.CouponCodeStyle
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.components.AcPrimaryButton
import io.github.autocoupons.core.designsystem.components.AcPrimaryButtonFull
import io.github.autocoupons.core.designsystem.components.AcSecondaryButton
import io.github.autocoupons.core.designsystem.components.AcTextButton
import io.github.autocoupons.core.designsystem.components.StoreLogo
import io.github.autocoupons.domain.coupon.CouponSessionState
import io.github.autocoupons.domain.coupon.CouponTestResult
import io.github.autocoupons.domain.coupon.CouponTestStatus
import io.github.autocoupons.domain.coupon.SessionOutcome
import io.github.autocoupons.domain.coupon.SessionPhase
import io.github.autocoupons.feature.coupons.AssistedCouponCard
import io.github.autocoupons.feature.coupons.CouponMessages
import io.github.autocoupons.feature.coupons.CouponStatusRow
import io.github.autocoupons.feature.coupons.WorkingCouponCard
import kotlinx.coroutines.launch
import io.github.autocoupons.core.designsystem.components.AcSheetDragHandle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponSheet(
    state: BrowserUiState,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRetryAssisted: () -> Unit,
    onOpenCheckout: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { AcSheetDragHandle() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().testTag("coupon_sheet"),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { SheetHeader(state) }
            when (state.sheetMode) {
                SheetMode.SESSION -> state.session?.let { sessionContent(it, state.restoring, onCancel, onRetry, onDismiss) }
                SheetMode.ASSISTED -> assistedContent(state, onRetryAssisted, onOpenCheckout)
                SheetMode.NOT_ON_CHECKOUT -> item {
                    Illustrated(
                        title = "Head to your bag",
                        body = "Coupons are tested on your bag page, where the coupon box and total are shown.",
                    ) { AcPrimaryButton("Go to bag", onClick = onOpenCheckout, icon = AcIcons.Bag) }
                }
                SheetMode.UNSUPPORTED_WEBVIEW -> item {
                    Illustrated(
                        title = "Update Android System WebView",
                        body = "Auto-apply needs a newer version of Android System WebView. Update it from the Play Store and try again.",
                    )
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun SheetHeader(state: BrowserUiState) {
    val subtitle = when {
        state.sheetMode == SheetMode.ASSISTED -> "Copy a code and apply it at checkout"
        state.session?.phase == SessionPhase.COMPLETED -> "All coupons checked"
        state.session?.phase?.isRunning == true -> "Finding your best deal"
        else -> state.retailer.summary
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StoreLogo(state.retailer.id, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                "Coupons for ${state.retailer.displayName}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textTertiary)
        }
    }
}

private fun LazyListScope.sessionContent(
    session: CouponSessionState,
    restoring: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    when (session.phase) {
        SessionPhase.FETCHING -> item { Working("Finding coupons for this store…") }
        SessionPhase.READING_CART -> item { Working("Found ${session.total} coupons. Reading your bag…") }
        SessionPhase.TESTING, SessionPhase.REAPPLYING -> {
            item { ProgressCard(session, onCancel) }
            items(session.results, key = { it.coupon.id }) { CouponStatusRow(it) }
        }
        SessionPhase.COMPLETED -> {
            item { OutcomeCard(session) }
            groupedResults(session)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Spacer(Modifier.height(Spacing.xs))
                    AcPrimaryButtonFull("Done", onClick = onDone)
                    AcTextButton("Check again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        SessionPhase.FAILED -> {
            val (title, body) = session.error?.let(CouponMessages::sessionError) ?: ("Something went wrong" to "Please try again.")
            item { Illustrated(title = title, body = body) { AcPrimaryButton("Try again", onClick = onRetry) } }
            if (session.results.any { it.status.isTerminal }) groupedResults(session)
        }
        SessionPhase.CANCELLED -> {
            item {
                Illustrated(
                    title = if (restoring) "Restoring your bag…" else "Stopped",
                    body = if (restoring) "Putting your bag back the way it was." else "Your bag is back to how it was. Results so far are below.",
                ) {
                    if (restoring) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else AcPrimaryButton("Check again", onClick = onRetry)
                }
            }
            groupedResults(session)
        }
    }
}

@Composable
private fun Working(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        BagArt(Modifier.size(120.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

/** The assignment's progress bar, with the coupon being applied and its discount below it. */
@Composable
private fun ProgressCard(session: CouponSessionState, onCancel: () -> Unit) {
    val animated by animateFloatAsState(session.progress, tween(400), label = "progress")
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (session.phase == SessionPhase.REAPPLYING) "Applying the best coupon" else "Trying coupons",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${session.completed} of ${session.total}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("progress_count"),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animated.coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(AcBrushes.Progress),
                )
            }
            val current = session.current
            val line = when {
                session.phase == SessionPhase.REAPPLYING -> "Applying ${session.bestSoFar?.coupon?.code} and checking your total…"
                current != null -> "Applying ${current.coupon.code}…"
                else -> "Getting ready…"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            session.lastVerified?.let { last ->
                Text(
                    "${last.coupon.code} gives you ${last.observedSavings?.format()} off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AcTheme.colors.success,
                )
            }
            session.bestSoFar?.let { best ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Best so far", style = MaterialTheme.typography.labelMedium, color = AcTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                    Text(best.coupon.code, style = CouponCodeStyle.copy(fontSize = CouponCodeStyle.fontSize * 0.9f))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("−${best.observedSavings?.format()}", style = MaterialTheme.typography.labelLarge, color = AcTheme.colors.success)
                }
            }
            // Stop lives inside the card so it stays on screen however long the coupon list is.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Please don't change your bag while we check.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcTheme.colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                AcTextButton("Stop", onClick = onCancel, modifier = Modifier.testTag("cancel_session"))
            }
        }
    }
}

@Composable
private fun OutcomeCard(session: CouponSessionState) {
    val baseline = session.baseline
    when (val outcome = session.outcome) {
        is SessionOutcome.WinnerVerified -> Box(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(AcBrushes.Success)
                .testTag("outcome_verified"),
        ) {
            ConfettiBackdrop(Modifier.matchParentSize())
            Column(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text("You saved", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                Text(
                    outcome.winner.observedSavings?.format().orEmpty(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                if (baseline != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            baseline.total.format(),
                            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            "${outcome.finalCart.total.format()} to pay",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = Spacing.md, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(AcIcons.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("${outcome.winner.coupon.code} applied to your bag", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
        is SessionOutcome.WinnerNotConfirmed -> NoticeCard(
            title = "Couldn't confirm the best coupon",
            body = "${outcome.winner.coupon.code} saved ${outcome.winner.observedSavings?.format()} while testing. ${outcome.reason}. Please check the total on your bag.",
            warning = true,
        )
        SessionOutcome.NoVerifiedSavings -> NoticeCard(
            title = "No coupon lowered this total",
            body = "We tried ${session.total} coupons on this bag." +
                (session.originalCode?.let { " Your code $it is back on your bag." } ?: " Your bag is unchanged."),
            warning = false,
        )
        null -> Unit
    }
}

@Composable
private fun NoticeCard(title: String, body: String, warning: Boolean) {
    Surface(
        color = if (warning) AcTheme.colors.warningContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (warning) AcTheme.colors.warning else MaterialTheme.colorScheme.onPrimaryContainer)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary)
        }
    }
}

/** Coupons that worked are never mixed with ones that didn't. */
private fun LazyListScope.groupedResults(session: CouponSessionState) {
    val winnerId = (session.outcome as? SessionOutcome.WinnerVerified)?.winner?.coupon?.id
    val working = session.results.filter { it.status == CouponTestStatus.APPLIED_AND_VERIFIED }
        .sortedByDescending { it.observedSavings?.minor ?: 0 }
    val rejected = session.results.filter { it.status == CouponTestStatus.REJECTED }
    val other = session.results.filter {
        it.status == CouponTestStatus.FAILED || it.status == CouponTestStatus.UNVERIFIED || it.status == CouponTestStatus.CANCELLED
    }
    section("Coupons that work on this bag", working) { WorkingCouponCard(it, best = it.coupon.id == winnerId) }
    section("Didn't apply", rejected) { CouponStatusRow(it) }
    section("Couldn't check", other) { CouponStatusRow(it) }
}

private fun LazyListScope.section(
    title: String,
    entries: List<CouponTestResult>,
    content: @Composable (CouponTestResult) -> Unit,
) {
    if (entries.isEmpty()) return
    item(key = "header_$title") {
        Row(Modifier.padding(top = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                "${entries.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
    items(entries, key = { "${title}_${it.coupon.id}" }) { content(it) }
}

private fun LazyListScope.assistedContent(state: BrowserUiState, onRetry: () -> Unit, onOpenCheckout: () -> Unit) {
    // The copy tip and "Go to bag" only make sense once there are codes to copy.
    val hasCodes = (state.assisted as? AssistedCoupons.Loaded)?.coupons?.isNotEmpty() == true
    if (hasCodes) item {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(BrandColors.Highlight.copy(alpha = 0.25f))
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(AcIcons.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            Text(
                "${state.retailer.displayName} needs your own login, so tap Copy and paste the code in the coupon box on your bag.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (hasCodes && !state.detection.isCheckout) {
        item { AcSecondaryButton("Go to bag", onClick = onOpenCheckout, icon = AcIcons.Bag, modifier = Modifier.fillMaxWidth()) }
    }
    when (val assisted = state.assisted) {
        null, AssistedCoupons.Loading -> item { Working("Loading coupons…") }
        is AssistedCoupons.Failed -> item {
            Illustrated(title = assisted.title, body = assisted.detail) { AcPrimaryButton("Try again", onClick = onRetry) }
        }
        is AssistedCoupons.Loaded -> if (assisted.coupons.isEmpty()) {
            item { Illustrated(title = "No coupons right now", body = "Check back later for new codes.") }
        } else {
            items(assisted.coupons, key = { it.id }) { coupon ->
                val clipboard = LocalClipboard.current
                val scope = rememberCoroutineScope()
                AssistedCouponCard(coupon) { code ->
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Coupon code", code))) }
                }
            }
        }
    }
    item { Spacer(Modifier.height(Spacing.sm)) }
}

@Composable
private fun Illustrated(title: String, body: String, actions: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Spacing.lg, horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BagArt(Modifier.size(120.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        actions()
    }
}
