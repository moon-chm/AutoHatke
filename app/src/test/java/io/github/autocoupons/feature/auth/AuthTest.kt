package io.github.autocoupons.feature.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthValidatorTest {
    @Test fun `email validation`() {
        assertEquals("Enter your email address", AuthValidator.emailError(""))
        assertNotNull(AuthValidator.emailError("not-an-email"))
        assertNotNull(AuthValidator.emailError("a@b"))
        assertNotNull(AuthValidator.emailError("a b@example.com"))
        assertNull(AuthValidator.emailError("shopper@example.com"))
        assertNull(AuthValidator.emailError("  first.last+tag@mail.co.in "))
    }

    @Test fun `login password is only required`() {
        assertEquals("Enter your password", AuthValidator.loginPasswordError(""))
        assertNull(AuthValidator.loginPasswordError("x"))
    }

    @Test fun `registration password rules and strength`() {
        assertEquals(PasswordStrength.EMPTY, AuthValidator.strength(""))
        assertEquals(PasswordStrength.WEAK, AuthValidator.strength("short1"))
        assertEquals(PasswordStrength.WEAK, AuthValidator.strength("alllowercase"))
        assertEquals(PasswordStrength.FAIR, AuthValidator.strength("letters123"))
        assertEquals(PasswordStrength.STRONG, AuthValidator.strength("Letters123!"))
        assertEquals(PasswordStrength.STRONG, AuthValidator.strength("Longerpassword12"))
        assertNotNull(AuthValidator.registrationPasswordError("abc"))
        assertNotNull(AuthValidator.registrationPasswordError("alllowercase"))
        assertNull(AuthValidator.registrationPasswordError("letters123"))
    }

    @Test fun `confirmation must match`() {
        assertEquals("Re-enter your password", AuthValidator.confirmPasswordError("letters123", ""))
        assertEquals("Passwords do not match", AuthValidator.confirmPasswordError("letters123", "letters124"))
        assertNull(AuthValidator.confirmPasswordError("letters123", "letters123"))
    }

    @Test fun `name validation`() {
        assertNotNull(AuthValidator.nameError("  "))
        assertNotNull(AuthValidator.nameError("A"))
        assertNull(AuthValidator.nameError("Asha Rao"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `invalid login shows errors and starts no session`() = runTest(dispatcher) {
        val sessions = DemoSessionStore()
        val vm = LoginViewModel(sessions)
        vm.onEmailChange("bad")
        vm.signIn()
        advanceUntilIdle()
        assertNotNull(vm.state.value.emailError)
        assertNotNull(vm.state.value.passwordError)
        assertNull(sessions.session.value)
    }

    @Test fun `valid login starts a demo session and discards the password`() = runTest(dispatcher) {
        val sessions = DemoSessionStore()
        val vm = LoginViewModel(sessions)
        vm.onEmailChange("asha.rao@example.com")
        vm.onPasswordChange("anything")
        vm.signIn()
        assertTrue(vm.state.value.submitting)
        advanceUntilIdle()
        assertTrue(vm.state.value.signedIn)
        assertEquals("", vm.state.value.password)
        assertEquals(DemoSession("Asha Rao", "asha.rao@example.com"), sessions.session.value)
        assertNotNull(sessions.welcome.value)
    }

    @Test fun `guest access needs no input`() = runTest(dispatcher) {
        val sessions = DemoSessionStore()
        LoginViewModel(sessions).continueWithDemoAccount()
        advanceUntilIdle()
        assertEquals(LoginViewModel.DEMO_EMAIL, sessions.session.value?.email)
    }

    @Test fun `password visibility toggles`() {
        val vm = LoginViewModel(DemoSessionStore())
        assertFalse(vm.state.value.passwordVisible)
        vm.togglePasswordVisibility()
        assertTrue(vm.state.value.passwordVisible)
    }

    @Test fun `registration with mismatched passwords is refused`() = runTest(dispatcher) {
        val sessions = DemoSessionStore()
        val vm = RegisterViewModel(sessions)
        vm.onNameChange("Asha Rao")
        vm.onEmailChange("asha@example.com")
        vm.onPasswordChange("letters123")
        vm.onConfirmChange("letters999")
        vm.register()
        advanceUntilIdle()
        assertEquals("Passwords do not match", vm.state.value.confirmError)
        assertNull(sessions.session.value)
        // Errors update live once the user has tried to submit.
        vm.onConfirmChange("letters123")
        assertNull(vm.state.value.confirmError)
    }

    @Test fun `successful registration starts a session and clears passwords`() = runTest(dispatcher) {
        val sessions = DemoSessionStore()
        val vm = RegisterViewModel(sessions)
        vm.onNameChange("Asha Rao")
        vm.onEmailChange("asha@example.com")
        vm.onPasswordChange("Letters123!")
        vm.onConfirmChange("Letters123!")
        vm.register()
        advanceUntilIdle()
        assertTrue(vm.state.value.registered)
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.confirmPassword)
        assertEquals("Asha Rao", sessions.session.value?.displayName)
    }
}
