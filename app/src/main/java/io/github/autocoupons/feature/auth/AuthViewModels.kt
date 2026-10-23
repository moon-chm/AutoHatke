package io.github.autocoupons.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.autocoupons.appContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Short pause so the button's progress state is perceivable; no network call happens. */
private const val SIMULATED_SIGN_IN_MS = 500L

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val submitting: Boolean = false,
    val signedIn: Boolean = false,
)

class LoginViewModel(private val sessions: DemoSessionStore) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    private var validateLive = false

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = if (validateLive) AuthValidator.emailError(value) else null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = if (validateLive) AuthValidator.loginPasswordError(value) else null)
    }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun signIn() {
        val current = _state.value
        if (current.submitting) return
        validateLive = true
        val emailError = AuthValidator.emailError(current.email)
        val passwordError = AuthValidator.loginPasswordError(current.password)
        _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (emailError != null || passwordError != null) return
        complete(DemoSession(displayNameFrom(current.email), current.email.trim())) { "Welcome back, $it" }
    }

    /** One-tap access with a fixed, documented demo identity. */
    fun continueWithDemoAccount() {
        if (_state.value.submitting) return
        complete(DemoSession(DEMO_NAME, DEMO_EMAIL)) { "You're browsing as a guest" }
    }

    private fun complete(session: DemoSession, welcome: (name: String) -> String) {
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            delay(SIMULATED_SIGN_IN_MS)
            sessions.start(session, welcome(session.displayName))
            // The password is discarded immediately; it was never sent, logged or stored.
            _state.update { LoginUiState(email = it.email, signedIn = true) }
        }
    }

    companion object {
        const val DEMO_NAME = "Guest"
        const val DEMO_EMAIL = "guest@autocoupons.example"

        val Factory = viewModelFactory {
            initializer { LoginViewModel(this.appContainer.demoSessionStore) }
        }

        internal fun displayNameFrom(email: String): String =
            email.substringBefore('@').split('.', '_', '-').filter { it.isNotBlank() }
                .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                .ifBlank { DEMO_NAME }
    }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val submitting: Boolean = false,
    val registered: Boolean = false,
) {
    val strength: PasswordStrength get() = AuthValidator.strength(password)
}

class RegisterViewModel(private val sessions: DemoSessionStore) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()
    private var validateLive = false

    fun onNameChange(value: String) = edit { it.copy(name = value) }
    fun onEmailChange(value: String) = edit { it.copy(email = value) }
    fun onPasswordChange(value: String) = edit { it.copy(password = value) }
    fun onConfirmChange(value: String) = edit { it.copy(confirmPassword = value) }
    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirmVisibility() = _state.update { it.copy(confirmVisible = !it.confirmVisible) }

    fun register() {
        if (_state.value.submitting) return
        validateLive = true
        val validated = validate(_state.value)
        _state.value = validated
        if (listOf(validated.nameError, validated.emailError, validated.passwordError, validated.confirmError)
                .any { it != null }
        ) return

        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            delay(SIMULATED_SIGN_IN_MS)
            val session = DemoSession(validated.name.trim(), validated.email.trim())
            sessions.start(session, "Welcome to AutoCoupons, ${session.displayName}")
            _state.update { RegisterUiState(name = it.name, email = it.email, registered = true) }
        }
    }

    private fun edit(transform: (RegisterUiState) -> RegisterUiState) = _state.update {
        val next = transform(it)
        if (validateLive) validate(next) else next
    }

    private fun validate(s: RegisterUiState) = s.copy(
        nameError = AuthValidator.nameError(s.name),
        emailError = AuthValidator.emailError(s.email),
        passwordError = AuthValidator.registrationPasswordError(s.password),
        confirmError = AuthValidator.confirmPasswordError(s.password, s.confirmPassword),
    )

    companion object {
        val Factory = viewModelFactory {
            initializer { RegisterViewModel(this.appContainer.demoSessionStore) }
        }
    }
}
