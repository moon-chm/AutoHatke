package io.github.autocoupons.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.Sizes

/** Brand-gradient pill. Falls back to a flat muted fill when disabled. */
@Composable
fun AcPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val active = enabled || loading
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .defaultMinSize(minHeight = Sizes.buttonHeight)
            .clip(CircleShape)
            .then(
                if (active) Modifier.background(AcBrushes.Primary)
                else Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
            )
            .semantics { if (loading) stateDescription = "Loading" },
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = if (loading) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
    ) {
        ButtonContent(text, icon, loading)
    }
}

@Composable
fun AcSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = Sizes.buttonHeight),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.35f else 0.12f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        ButtonContent(text, icon, loading = false)
    }
}

@Composable
fun AcTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.defaultMinSize(minHeight = Sizes.minTouch)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, loading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Full-width variant used by forms and sheets. */
@Composable
fun AcPrimaryButtonFull(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) = AcPrimaryButton(text, onClick, modifier.fillMaxWidth(), enabled, loading, icon)
