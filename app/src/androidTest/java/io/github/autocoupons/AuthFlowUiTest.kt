package io.github.autocoupons

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowUiTest {

    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun launchSignedOut() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AutoCouponsApplication
        app.container.demoSessionStore.end()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun close() = scenario.close()

    @Test fun emptyLoginShowsValidationErrors() {
        compose.onNodeWithText("Sign in").performScrollTo().performClick()
        compose.onNodeWithText("Enter your email address").assertIsDisplayed()
        compose.onNodeWithText("Enter your password").performScrollTo().assertIsDisplayed()
    }

    @Test fun invalidEmailIsRejected() {
        compose.onNodeWithTag("login_email").performTextInput("not-an-email")
        compose.onNodeWithTag("login_password").performTextInput("secret")
        compose.onNodeWithText("Sign in").performScrollTo().performClick()
        compose.onNodeWithText("Enter a valid email address, like name@example.com").performScrollTo().assertIsDisplayed()
    }

    @Test fun validLoginOpensHome() {
        compose.onNodeWithTag("login_email").performTextInput("asha@example.com")
        compose.onNodeWithTag("login_password").performTextInput("secret")
        compose.onNodeWithText("Sign in").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test fun registrationValidatesAndSucceeds() {
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.onNodeWithTag("register_name").performTextInput("Asha Rao")
        compose.onNodeWithTag("register_email").performTextInput("asha@example.com")
        compose.onNodeWithTag("register_password").performTextInput("Letters123!")
        compose.onNodeWithTag("register_confirm").performTextInput("Letters999!")
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.onNodeWithText("Passwords do not match").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("register_confirm").performTextClearance()
        compose.onNodeWithTag("register_confirm").performTextInput("Letters123!")
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun registerLinksBackToLogin() {
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.onNodeWithText("Create your account").assertIsDisplayed()
        compose.onNodeWithText("Sign in").performScrollTo().performClick()
        compose.onNodeWithText("Welcome back").assertIsDisplayed()
    }
}
