package io.github.autocoupons.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sets status/navigation bar icon colour for the screen being shown. Screens with a dark hero
 * pass `onDarkBackground = true`; others follow the theme. Each screen sets its own style, so no
 * restore step is needed when navigating.
 */
@Composable
fun SystemBarIcons(onDarkBackground: Boolean) {
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !onDarkBackground && !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
