package io.github.autocoupons.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.SystemBarIcons
import kotlinx.coroutines.launch

enum class MainTab(val label: String) { HOME("Home"), SAVINGS("Savings"), PROFILE("Profile") }

/** Signed-in shell: three tabs under a floating pill navigation bar. */
@Composable
fun HomeScreen(
    onOpenBrowser: (retailerId: String, url: String?) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var sheet by rememberSaveable { mutableStateOf<InfoTopic?>(null) }
    SystemBarIcons(onDarkBackground = tab != MainTab.PROFILE)

    LaunchedEffect(state.welcome) {
        state.welcome?.let {
            viewModel.consumeWelcome()
            snackbar.showSnackbar(it)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
    ) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
        ) { current ->
            when (current) {
                MainTab.HOME -> HomeTab(
                    state = state,
                    onOpenStore = { onOpenBrowser(it, null) },
                    onLink = { input ->
                        when (val result = viewModel.resolveLink(input)) {
                            is LinkResolution.Open -> onOpenBrowser(result.retailerId, result.url)
                            is LinkResolution.Rejected -> scope.launch { snackbar.showSnackbar(result.message) }
                        }
                    },
                    onShowSavings = { tab = MainTab.SAVINGS },
                    onShowHowItWorks = { sheet = InfoTopic.HOW_IT_WORKS },
                )
                MainTab.SAVINGS -> SavingsTab(
                    state = state,
                    onStartShopping = { onOpenBrowser(state.retailers.first().id, null) },
                )
                MainTab.PROFILE -> ProfileTab(
                    state = state,
                    onCheckService = viewModel::checkLiveApi,
                    onOpenInfo = { sheet = it },
                    onSignOut = {
                        viewModel.signOut()
                        onSignedOut()
                    },
                )
            }
        }

        // Status-bar scrim so scrolled content never runs under the clock. On the gradient tabs it
        // matches the hero's top edge, so it is invisible until content scrolls beneath it.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(
                    if (tab == MainTab.PROFILE) SolidColor(MaterialTheme.colorScheme.background)
                    else Brush.horizontalGradient(listOf(BrandColors.IndigoDeep, BrandColors.Indigo)),
                ),
        )

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnackbarHost(snackbar)
            FloatingNavBar(selected = tab, onSelect = { tab = it })
        }
    }

    sheet?.let { InfoSheet(it, onDismiss = { sheet = null }) }
}

@Composable
private fun FloatingNavBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MainTab.entries.forEach { item ->
                NavItem(
                    tab = item,
                    icon = when (item) {
                        MainTab.HOME -> Icons.Filled.Home
                        MainTab.SAVINGS -> AcIcons.Wallet
                        MainTab.PROFILE -> Icons.Filled.Person
                    },
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: MainTab, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else AcTheme.colors.textTertiary
    Column(
        modifier
            .height(56.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                CircleShape,
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag("tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
