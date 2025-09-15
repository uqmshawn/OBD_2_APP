package com.hurtec.obd2.diagnostics.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hurtec.obd2.diagnostics.ui.navigation.HurtecNavigation
import com.hurtec.obd2.diagnostics.ui.theme.HurtecTheme
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.AndroidEntryPoint

/**
 * Modern MainActivity with Jetpack Compose
 * Replaces the old fragment-based approach with modern navigation
 */
@AndroidEntryPoint
class ModernMainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            CrashHandler.logInfo("ModernMainActivity: Starting onCreate...")

            // Install splash screen
            installSplashScreen()

            super.onCreate(savedInstanceState)
            enableEdgeToEdge()

            CrashHandler.logInfo("ModernMainActivity: Setting content...")

            setContent {
                HurtecApp()
            }

            CrashHandler.logInfo("ModernMainActivity: onCreate completed successfully")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ModernMainActivity.onCreate")

            // Try to continue with basic setup
            try {
                super.onCreate(savedInstanceState)
                setContent {
                    HurtecTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                }
            } catch (fallbackException: Exception) {
                CrashHandler.handleException(fallbackException, "ModernMainActivity.onCreate.fallback")
                finish() // Close the app if we can't even show a basic screen
            }
        }
    }
}

@Composable
fun HurtecApp() {
    CrashHandler.logInfo("HurtecApp: Starting app composition...")

    HurtecTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HurtecNavigation()
        }
    }
}
