package io.github.autocoupons

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kills the WebView renderer with `chrome://crash` (the approach Android's termination-handling
 * docs recommend) while a coupon session is running. The app must survive, show the error state,
 * and recover into a fresh WebView on "Try again".
 */
@RunWith(AndroidJUnit4::class)
class RendererCrashTest {

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun startSignedOut() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AutoCouponsApplication
        if (app.container.demoSessionStore.session.value != null) {
            compose.onNodeWithTag("tab_profile").performClick()
            compose.onNodeWithText("Sign out").performScrollTo().performClick()
        }
    }

    @Test fun rendererCrashMidSessionShowsErrorAndRecovers() {
        compose.onNodeWithText("Continue as guest").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home_search").performTextInput("https://appassets.androidplatform.net/assets/demo-store/cart.html")
        compose.onNodeWithTag("home_search").performImeAction()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("assistant_bubble").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("assistant_bubble").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("progress_count").fetchSemanticsNodes().isNotEmpty() }

        compose.runOnUiThread { findWebView(compose.activity.window.decorView)!!.loadUrl("chrome://crash") }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("The page stopped responding").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithText("Try again").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("assistant_bubble").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("assistant_bubble").performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("outcome_verified").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }
}
