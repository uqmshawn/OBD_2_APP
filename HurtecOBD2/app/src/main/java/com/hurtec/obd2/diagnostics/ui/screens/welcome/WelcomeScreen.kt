package com.hurtec.obd2.diagnostics.ui.screens.welcome

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * Welcome Screen - First screen users see
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    navController: NavController,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    var logoScale by remember { mutableFloatStateOf(0f) }
    var contentAlpha by remember { mutableFloatStateOf(0f) }
    
    // Animation effects
    LaunchedEffect(Unit) {
        isVisible = true
        // Logo animation
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            logoScale = value
        }
        
        // Content fade in
        delay(500)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1000)
        ) { value, _ ->
            contentAlpha = value
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and App Name
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Hurtec OBD-II Logo",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Title
            Text(
                text = "Hurtec OBD-II",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.alpha(contentAlpha)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Text(
                text = "Professional Vehicle Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(contentAlpha)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Features List
            Column(
                modifier = Modifier.alpha(contentAlpha),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WelcomeFeatureItem(
                    icon = Icons.Default.Speed,
                    title = "Real-time Monitoring",
                    description = "Monitor engine parameters in real-time"
                )
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Warning,
                    title = "Diagnostic Trouble Codes",
                    description = "Read and clear DTCs with detailed explanations"
                )
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Analytics,
                    title = "Performance Analysis",
                    description = "Track vehicle performance and fuel efficiency"
                )
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Multiple Connections",
                    description = "Bluetooth, USB, and Wi-Fi support"
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Get Started Button
            Button(
                onClick = {
                    try {
                        CrashHandler.logInfo("WelcomeScreen: Get Started button clicked")
                        onGetStarted()
                    } catch (e: Exception) {
                        CrashHandler.handleException(e, "WelcomeScreen.GetStarted.onClick")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(contentAlpha),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Skip Button
            TextButton(
                onClick = {
                    try {
                        CrashHandler.logInfo("WelcomeScreen: Skip button clicked")
                        onSkip()
                    } catch (e: Exception) {
                        CrashHandler.handleException(e, "WelcomeScreen.Skip.onClick")
                    }
                },
                modifier = Modifier.alpha(contentAlpha)
            ) {
                Text(
                    text = "Skip Tutorial",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
        
        // Version info at bottom
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .alpha(contentAlpha)
        )
    }
}

@Composable
private fun WelcomeFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}
