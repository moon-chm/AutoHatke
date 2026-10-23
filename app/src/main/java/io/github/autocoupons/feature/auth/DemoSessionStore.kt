package io.github.autocoupons.feature.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Non-sensitive identity for the demo session. Passwords are never part of it. */
data class DemoSession(val displayName: String, val email: String)

/**
 * In-memory demo session. Nothing is written to disk and nothing is sent anywhere, so a process
 * restart returns to the Login screen. This is deliberate: there is no real account system.
 */
class DemoSessionStore {
    private val _session = MutableStateFlow<DemoSession?>(null)
    val session: StateFlow<DemoSession?> = _session.asStateFlow()

    private val _welcome = MutableStateFlow<String?>(null)

    /** One-shot message shown on Home after sign-in or registration. */
    val welcome: StateFlow<String?> = _welcome.asStateFlow()

    fun start(session: DemoSession, welcomeMessage: String) {
        _session.value = session
        _welcome.value = welcomeMessage
    }

    fun consumeWelcome() {
        _welcome.value = null
    }

    fun end() {
        _session.value = null
        _welcome.value = null
    }
}
