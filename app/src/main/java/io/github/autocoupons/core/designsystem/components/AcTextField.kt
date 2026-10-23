package io.github.autocoupons.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.github.autocoupons.core.designsystem.AcIcons
import io.github.autocoupons.core.designsystem.AcTheme

/**
 * Branded single-line field with inline error text. When [passwordVisible] is non-null the field
 * masks its content and shows a show/hide toggle.
 */
@Composable
fun AcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    passwordVisible: Boolean? = null,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    enabled: Boolean = true,
    testTag: String = label,
) {
    val isPassword = passwordVisible != null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().testTag(testTag),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        shape = MaterialTheme.shapes.medium,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible == true) AcIcons.VisibilityOff else AcIcons.Visibility,
                        contentDescription = if (passwordVisible == true) "Hide $label" else "Show $label",
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && passwordVisible != true) PasswordVisualTransformation()
        else VisualTransformation.None,
        keyboardOptions = if (isPassword) keyboardOptions.copy(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
        else keyboardOptions,
        keyboardActions = keyboardActions,
        supportingText = when {
            error != null -> { { Text(error) } }
            supportingText != null -> { { Text(supportingText) } }
            else -> null
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = AcTheme.colors.border,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
