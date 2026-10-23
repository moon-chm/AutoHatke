package io.github.autocoupons

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs Google's Accessibility Test Framework over each native screen (touch-target size, contrast,
 * missing labels, duplicate descriptions…). With checks enabled, every interaction also re-checks
 * the screen, so a failure names the screen it was found on.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityChecksTest {

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun startSignedOut() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AutoCouponsApplication
        if (app.container.demoSessionStore.session.value != null) {
            compose.onNodeWithTag("tab_profile").performClick()
            compose.onNodeWithText("Sign out").performScrollTo().performClick()
        }
        compose.enableAccessibilityChecks()
    }

    @Test fun nativeScreensPassAccessibilityChecks() {
        compose.onRoot().tryPerformAccessibilityChecks() // Login

        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.onRoot().tryPerformAccessibilityChecks() // Create account
        compose.onNodeWithText("Sign in").performScrollTo().performClick()

        compose.onNodeWithText("Continue as guest").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().tryPerformAccessibilityChecks() // Home

        compose.onNodeWithTag("tab_savings").performClick()
        compose.onRoot().tryPerformAccessibilityChecks() // Savings

        compose.onNodeWithTag("tab_profile").performClick()
        compose.onRoot().tryPerformAccessibilityChecks() // Profile

        compose.onNodeWithText("Privacy & security").performClick()
        compose.onRoot().tryPerformAccessibilityChecks() // Info sheet
    }

    @Test fun browserAndCouponResultsPassAccessibilityChecks() {
        compose.onNodeWithText("Continue as guest").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty() }

        // Paste the Modara bag link, the same way a shopper can paste any product or cart link.
        compose.onNodeWithTag("home_search").performTextInput("https://appassets.androidplatform.net/assets/demo-store/cart.html")
        compose.onNodeWithTag("home_search").performImeAction()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("assistant_bubble").fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().tryPerformAccessibilityChecks() // Browser on the bag page

        compose.onNodeWithTag("assistant_bubble").performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("outcome_verified").fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().tryPerformAccessibilityChecks() // Coupon results sheet
    }
}
