package io.github.autocoupons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.autocoupons.core.designsystem.AutoCouponsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Read once: the start destination only matters when the activity is created.
        val signedIn = (application as AutoCouponsApplication).container.demoSessionStore.session.value != null
        setContent {
            AutoCouponsTheme {
                // Surface supplies onBackground as the default content colour for both themes.
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(signedIn = signedIn)
                }
            }
        }
    }
}
