package io.github.autocoupons.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Semantic colours Material 3 has no slot for. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val highlight: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
)

private val LightExtended = ExtendedColors(
    success = BrandColors.SuccessText,
    successContainer = BrandColors.SuccessContainer,
    warning = BrandColors.WarningText,
    warningContainer = BrandColors.WarningContainer,
    highlight = BrandColors.Highlight,
    textSecondary = BrandColors.TextSecondary,
    textTertiary = BrandColors.TextTertiary,
    border = BrandColors.Border,
)

private val DarkExtended = ExtendedColors(
    success = BrandColors.DarkSuccess,
    successContainer = BrandColors.DarkSuccessContainer,
    warning = BrandColors.DarkWarning,
    warningContainer = BrandColors.DarkWarningContainer,
    highlight = BrandColors.Highlight,
    textSecondary = BrandColors.DarkTextSecondary,
    textTertiary = BrandColors.DarkTextTertiary,
    border = BrandColors.DarkBorder,
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = BrandColors.Indigo,
    onPrimary = Color.White,
    primaryContainer = BrandColors.IndigoTint,
    onPrimaryContainer = BrandColors.IndigoDarkest,
    inversePrimary = BrandColors.IndigoLight,
    secondary = BrandColors.IndigoDeep,
    onSecondary = Color.White,
    secondaryContainer = BrandColors.IndigoTint,
    onSecondaryContainer = BrandColors.IndigoDarkest,
    tertiary = BrandColors.Success,
    onTertiary = Color.White,
    tertiaryContainer = BrandColors.SuccessContainer,
    onTertiaryContainer = BrandColors.SuccessText,
    background = BrandColors.Background,
    onBackground = BrandColors.TextPrimary,
    surface = BrandColors.Surface,
    onSurface = BrandColors.TextPrimary,
    surfaceVariant = BrandColors.Background,
    onSurfaceVariant = BrandColors.TextSecondary,
    surfaceContainerLowest = BrandColors.Surface,
    surfaceContainerLow = BrandColors.Surface,
    surfaceContainer = BrandColors.Surface,
    surfaceContainerHigh = BrandColors.Surface,
    surfaceContainerHighest = BrandColors.Background,
    outline = BrandColors.Border,
    outlineVariant = BrandColors.Border,
    error = BrandColors.Error,
    onError = Color.White,
    errorContainer = BrandColors.ErrorContainer,
    onErrorContainer = BrandColors.Error,
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = BrandColors.IndigoLight,
    onPrimary = BrandColors.IndigoDarkest,
    primaryContainer = BrandColors.IndigoDarkest,
    onPrimaryContainer = BrandColors.IndigoSoft,
    inversePrimary = BrandColors.Indigo,
    secondary = BrandColors.IndigoSoft,
    onSecondary = BrandColors.IndigoDarkest,
    secondaryContainer = BrandColors.DarkSurfaceHigh,
    onSecondaryContainer = BrandColors.IndigoSoft,
    tertiary = BrandColors.DarkSuccess,
    onTertiary = BrandColors.DarkSuccessContainer,
    tertiaryContainer = BrandColors.DarkSuccessContainer,
    onTertiaryContainer = BrandColors.DarkSuccess,
    background = BrandColors.DarkBackground,
    onBackground = BrandColors.DarkTextPrimary,
    surface = BrandColors.DarkSurface,
    onSurface = BrandColors.DarkTextPrimary,
    surfaceVariant = BrandColors.DarkSurfaceHigh,
    onSurfaceVariant = BrandColors.DarkTextSecondary,
    surfaceContainerLowest = BrandColors.DarkBackground,
    surfaceContainerLow = BrandColors.DarkSurface,
    surfaceContainer = BrandColors.DarkSurface,
    surfaceContainerHigh = BrandColors.DarkSurfaceHigh,
    surfaceContainerHighest = BrandColors.DarkSurfaceHigh,
    outline = BrandColors.DarkBorder,
    outlineVariant = BrandColors.DarkBorder,
    error = BrandColors.DarkError,
    onError = BrandColors.DarkErrorContainer,
    errorContainer = BrandColors.DarkErrorContainer,
    onErrorContainer = BrandColors.DarkError,
)

val AcShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

@Composable
fun AutoCouponsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AcTypography,
            shapes = AcShapes,
            content = content,
        )
    }
}

/** Shorthand for brand-specific colours: `AcTheme.colors.success`. */
object AcTheme {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
