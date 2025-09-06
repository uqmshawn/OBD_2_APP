package com.hurtec.obd2.diagnostics.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hurtec.obd2.diagnostics.ui.navigation.HurtecNavigation
import com.hurtec.obd2.diagnostics.ui.theme.HurtecTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Modern MainActivity with Jetpack Compose
 * Replaces the old fragment-based approach with modern navigation
 */
@AndroidEntryPoint
class ModernMainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            HurtecApp()
        }
    }
}

@Composable
fun HurtecApp() {
    HurtecTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HurtecNavigation()
        }
    }
}
