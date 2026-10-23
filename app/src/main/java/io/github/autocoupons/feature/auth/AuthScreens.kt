package io.github.autocoupons.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.autocoupons.core.designsystem.AcBrushes
import io.github.autocoupons.core.designsystem.AcTheme
import io.github.autocoupons.core.designsystem.BrandColors
import io.github.autocoupons.core.designsystem.CouponTicketsArt
import io.github.autocoupons.core.designsystem.Spacing
import io.github.autocoupons.core.designsystem.SystemBarIcons
import io.github.autocoupons.core.designsystem.components.AcPrimaryButtonFull
import io.github.autocoupons.core.designsystem.components.AcSecondaryButton
import io.github.autocoupons.core.designsystem.components.AcTextButton
import io.github.autocoupons.core.designsystem.components.AcTextField
import io.github.autocoupons.core.designsystem.components.BrandMark

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    AuthScaffold(
        heroTitle = "Save on every checkout",
        heroBody = "We find, test and apply the best coupon for your bag.",
        title = "Welcome back",
        subtitle = "Sign in to keep saving.",
    ) {
        AcTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            error = state.emailError,
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            enabled = !state.submitting,
            testTag = "login_email",
        )
        AcTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            error = state.passwordError,
            leadingIcon = Icons.Filled.Lock,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.signIn() }),
            passwordVisible = state.passwordVisible,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            enabled = !state.submitting,
            testTag = "login_password",
        )
        Spacer(Modifier.height(Spacing.xs))
        AcPrimaryButtonFull("Sign in", onClick = { focus.clearFocus(); viewModel.signIn() }, loading = state.submitting)
        OrDivider()
        AcSecondaryButton(
            "Continue as guest",
            onClick = viewModel::continueWithDemoAccount,
            enabled = !state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
        FooterLink(prompt = "New to AutoCoupons?", action = "Create account", onClick = onCreateAccount, enabled = !state.submitting)
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    LaunchedEffect(state.registered) { if (state.registered) onRegistered() }

    AuthScaffold(
        heroTitle = "Never miss a coupon",
        heroBody = "Join in seconds and let AutoCoupons do the hunting.",
        title = "Create your account",
        subtitle = "It only takes a minute.",
    ) {
        AcTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = "Full name",
            error = state.nameError,
            leadingIcon = Icons.Filled.Person,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = next,
            enabled = !state.submitting,
            testTag = "register_name",
        )
        AcTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            error = state.emailError,
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = next,
            enabled = !state.submitting,
            testTag = "register_email",
        )
        AcTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            error = state.passwordError,
            supportingText = "At least ${AuthValidator.MIN_PASSWORD_LENGTH} characters. Mix letters, numbers or symbols",
            leadingIcon = Icons.Filled.Lock,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = next,
            passwordVisible = state.passwordVisible,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            enabled = !state.submitting,
            testTag = "register_password",
        )
        StrengthMeter(state.strength)
        AcTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmChange,
            label = "Confirm password",
            error = state.confirmError,
            leadingIcon = Icons.Filled.Lock,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.register() }),
            passwordVisible = state.confirmVisible,
            onTogglePasswordVisibility = viewModel::toggleConfirmVisibility,
            enabled = !state.submitting,
            testTag = "register_confirm",
        )
        Spacer(Modifier.height(Spacing.xs))
        AcPrimaryButtonFull("Create account", onClick = { focus.clearFocus(); viewModel.register() }, loading = state.submitting)
        FooterLink(prompt = "Already have an account?", action = "Sign in", onClick = onBackToLogin, enabled = !state.submitting)
    }
}

private val HeroHeight = 300.dp
private val SheetOverlap = 36.dp

/** Brand hero with artwork behind a rounded form sheet that scrolls up over it. */
@Composable
private fun AuthScaffold(
    heroTitle: String,
    heroBody: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    SystemBarIcons(onDarkBackground = true)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val screenHeight = maxHeight
        Box(
            Modifier
                .fillMaxWidth()
                .height(HeroHeight + SheetOverlap)
                .background(AcBrushes.Hero),
        ) {
            CouponTicketsArt(
                Modifier
                    .size(170.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = (-SheetOverlap - 8.dp)),
            )
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    BrandMark(size = 36.dp, container = Color.White, glyph = BrandColors.Indigo)
                    Text("AutoCoupons", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    heroTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    // Reserve the art's width rather than a fixed text width, so narrow screens and
                    // large font sizes wrap the text instead of running it under the illustration.
                    modifier = Modifier.padding(end = 126.dp).widthIn(max = 260.dp),
                )
                Text(
                    heroBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.padding(end = 126.dp).widthIn(max = 240.dp),
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(HeroHeight))
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight - HeroHeight),
            ) {
                Column(
                    Modifier
                        .widthIn(max = 480.dp)
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary)
                    Spacer(Modifier.height(Spacing.xs))
                    content()
                }
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        HorizontalDivider(Modifier.weight(1f), color = AcTheme.colors.border)
        Text("or", style = MaterialTheme.typography.labelMedium, color = AcTheme.colors.textTertiary)
        HorizontalDivider(Modifier.weight(1f), color = AcTheme.colors.border)
    }
}

@Composable
private fun StrengthMeter(strength: PasswordStrength) {
    if (strength == PasswordStrength.EMPTY) return
    val color = when (strength) {
        PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
        PasswordStrength.FAIR -> AcTheme.colors.warning
        else -> AcTheme.colors.success
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        LinearProgressIndicator(
            progress = { strength.score / 3f },
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = AcTheme.colors.border,
            drawStopIndicator = {},
        )
        Text("Strength: ${strength.label}", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun FooterLink(prompt: String, action: String, onClick: () -> Unit, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = AcTheme.colors.textSecondary)
        AcTextButton(action, onClick = onClick, enabled = enabled)
    }
}
