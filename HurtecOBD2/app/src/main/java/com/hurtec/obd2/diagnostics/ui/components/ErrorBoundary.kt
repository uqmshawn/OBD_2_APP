package com.hurtec.obd2.diagnostics.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Error boundary component to catch and display errors gracefully
 */
@Composable
fun ErrorBoundary(
    onRetry: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // For now, just render the content directly
    // In a real implementation, you'd use error handling at the ViewModel level
    content()
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry
        ) {
            Text("Try Again")
        }
    }
}

/**
 * Safe composable wrapper that provides fallback content
 */
@Composable
fun SafeComposable(
    fallback: @Composable () -> Unit = {
        Text(
            "Content unavailable",
            modifier = Modifier.padding(16.dp)
        )
    },
    content: @Composable () -> Unit
) {
    // For now, just render the content directly
    // Error handling should be done at the ViewModel level
    content()
}
