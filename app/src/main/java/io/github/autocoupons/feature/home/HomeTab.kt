package io.github.autocoupons.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.CouponTicketsArt
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.components.StoreLogo
import io.github.autocoupons.data.checkout.RetailerConfig
import io.github.autocoupons.domain.coupon.AutomationSupport
import kotlinx.coroutines.launch

private val HeroBottomRadius = 32.dp
private val SearchHeight = 56.dp

@Composable
fun HomeTab(
    state: HomeUiState,
    onOpenStore: (String) -> Unit,
    onLink: (String) -> Unit,
    onShowSavings: () -> Unit,
    onShowHowItWorks: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val stores = state.retailers.filter { query.isBlank() || looksLikeLink(query) || it.displayName.contains(query.trim(), ignoreCase = true) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Hero(name = state.session?.displayName, query = query, onQuery = { query = it }, onSubmit = onLink) }
        item { StoreShelf(stores = stores, filtered = query.isNotBlank() && !looksLikeLink(query), onOpenStore = onOpenStore) }
        item {
            val total = state.totalSaved
            if (total != null) {
                PromoStrip(
                    icon = AcIcons.Wallet,
                    title = "You've saved ${total.format()}",
                    body = "Across ${state.history.count { it.confirmedSavings != null }} checkout" +
                        if (state.history.count { it.confirmedSavings != null } == 1) "" else "s",
                    action = "View",
                    onClick = onShowSavings,
                )
            } else {
                PromoStrip(
                    icon = AcIcons.LocalOffer,
                    title = "Best coupon, auto-applied",
                    body = "We test every code so you don't have to",
                    action = "See how",
                    onClick = onShowHowItWorks,
                )
            }
        }
        item { HowItWorksRow() }
    }
}

@Composable
private fun Hero(name: String?, query: String, onQuery: (String) -> Unit, onSubmit: (String) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = SearchHeight / 2)
                .clip(RoundedCornerShape(bottomStart = HeroBottomRadius, bottomEnd = HeroBottomRadius))
                .background(AcBrushes.Hero),
        ) {
            CouponTicketsArt(
                Modifier
                    .size(150.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = (-34).dp),
            )
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.md, bottom = SearchHeight / 2 + Spacing.xl),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initials(name), style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Column {
                        Text("Hi, ${name ?: "there"}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Ready to save today?", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    "Shop smart.\nPay less.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "At checkout, tap the coupon bubble. We test every code and apply the one that saves you most.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.widthIn(max = 220.dp),
                )
            }
        }
        SearchPill(
            query = query,
            onQuery = onQuery,
            onSubmit = onSubmit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Spacing.screen)
                .widthIn(max = 600.dp),
        )
    }
}

@Composable
private fun SearchPill(query: String, onQuery: (String) -> Unit, onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        modifier = modifier.fillMaxWidth().height(SearchHeight),
    ) {
        Row(Modifier.padding(start = Spacing.lg, end = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.md))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        "Search a store or paste a link",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcTheme.colors.textTertiary,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onGo = {
                        focus.clearFocus()
                        if (looksLikeLink(query)) onSubmit(query)
                    }),
                    modifier = Modifier.fillMaxWidth().testTag("home_search"),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
            } else {
                IconButton(onClick = {
                    scope.launch {
                        val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) {
                            onQuery(text)
                            if (looksLikeLink(text)) onSubmit(text)
                        }
                    }
                }) {
                    Icon(AcIcons.ContentPaste, contentDescription = "Paste link", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Dark shelf of supported stores, styled like a launcher folder. */
@Composable
private fun StoreShelf(stores: List<RetailerConfig>, filtered: Boolean, onOpenStore: (String) -> Unit) {
    Box(Modifier.padding(horizontal = Spacing.screen).widthIn(max = 600.dp)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = BrandColors.Shelf,
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        ) {
            Column(
                Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl, bottom = Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Shop & save", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.semantics { heading() })
                Text(
                    "Coupons tested for you at checkout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))
                if (stores.isEmpty()) {
                    Text(
                        "No matching stores yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = Spacing.lg),
                    )
                } else {
                    val tiles: List<RetailerConfig?> = if (filtered) stores else stores + null
                    tiles.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
                            row.forEach { retailer ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                    if (retailer != null) StoreTile(retailer, onClick = { onOpenStore(retailer.id) })
                                    else ComingSoonTile()
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = AcTheme.colors.successContainer,
            border = BorderStroke(1.dp, AcTheme.colors.success.copy(alpha = 0.3f)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(AcIcons.Verified, contentDescription = null, tint = AcTheme.colors.success, modifier = Modifier.size(18.dp))
                Text("Savings checked at checkout", style = MaterialTheme.typography.labelMedium, color = AcTheme.colors.success)
            }
        }
    }
}

@Composable
private fun StoreTile(retailer: RetailerConfig, onClick: () -> Unit) {
    val automated = retailer.automation == AutomationSupport.AUTOMATED
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClickLabel = "Open ${retailer.displayName}", onClick = onClick)
            .padding(Spacing.xs)
            .testTag("store_${retailer.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            StoreLogo(retailer.id, size = 68.dp, modifier = Modifier.padding(top = 9.dp))
            Text(
                if (automated) "AUTO-APPLY" else "CODES",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (automated) BrandColors.Emerald else BrandColors.Indigo)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(retailer.displayName, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun ComingSoonTile() {
    Column(Modifier.padding(Spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(top = 9.dp)
                .size(68.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(BrandColors.ShelfHigh)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(Spacing.sm))
        Text("More soon", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun PromoStrip(icon: ImageVector, title: String, body: String, action: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = AcTheme.colors.successContainer,
        modifier = Modifier
            .padding(horizontal = Spacing.screen)
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(AcBrushes.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textSecondary)
            }
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

private data class Step(val title: String, val body: String, val icon: ImageVector)

private val Steps = listOf(
    Step("Shop as usual", "Open a store and add what you love to your bag.", AcIcons.Bag),
    Step("Tap the bubble", "On the bag page, tap the coupon bubble. We try every code.", AcIcons.LocalOffer),
    Step("Pay less", "The biggest saving is applied and double-checked for you.", AcIcons.Verified),
)

@Composable
private fun HowItWorksRow() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            "How it works",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.screen).semantics { heading() },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            itemsIndexed(Steps) { index, step ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, AcTheme.colors.border.copy(alpha = 0.6f)),
                    modifier = Modifier.width(210.dp).height(178.dp),
                ) {
                    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(step.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "0${index + 1}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            )
                        }
                        Text(step.title, style = MaterialTheme.typography.titleSmall)
                        Text(step.body, style = MaterialTheme.typography.bodySmall, color = AcTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

internal fun initials(name: String?): String =
    name?.split(' ')?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.take(1).uppercase() }?.ifEmpty { null } ?: "AC"

private fun looksLikeLink(text: String): Boolean {
    val t = text.trim()
    return t.startsWith("http://", true) || t.startsWith("https://", true) || (t.contains('.') && !t.contains(' ') && t.contains('/'))
}
