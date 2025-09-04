package com.hurtec.obd2.diagnostics.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.databinding.ActivityMainBinding

/**
 * Main Activity for Hurtec OBD-II Diagnostics
 * Modern implementation with Navigation Component and Material Design 3
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Permission launcher for Bluetooth and Location permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, granted) ->
            when (permission) {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT -> {
                    if (granted) {
                        // Bluetooth permission granted
                        onBluetoothPermissionGranted()
                    } else {
                        // Show rationale or disable Bluetooth features
                        onBluetoothPermissionDenied()
                    }
                }
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                    if (granted) {
                        // Location permission granted
                        onLocationPermissionGranted()
                    } else {
                        // Show rationale
                        onLocationPermissionDenied()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImmersiveMode()
        setupNavigation()
        checkAndRequestPermissions()
    }

    private fun setupImmersiveMode() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set status bar and navigation bar colors
        window.statusBarColor = ContextCompat.getColor(this, R.color.hurtec_background)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.hurtec_surface)

        // Configure system bars appearance
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false

        // Keep screen on for OBD monitoring
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Configure bottom navigation with modern styling
        navView.apply {
            setupWithNavController(navController)
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.hurtec_surface))
            itemIconTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.bottom_nav_color_selector)
            itemTextColor = ContextCompat.getColorStateList(this@MainActivity, R.color.bottom_nav_color_selector)
        }

        // Handle navigation item selection with haptic feedback
        navView.setOnItemSelectedListener { item ->
            // Add haptic feedback for modern UX
            navView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    navController.navigate(R.id.navigation_dashboard)
                    true
                }
                R.id.navigation_diagnostics -> {
                    navController.navigate(R.id.navigation_diagnostics)
                    true
                }
                R.id.navigation_data -> {
                    navController.navigate(R.id.navigation_data)
                    true
                }
                R.id.navigation_settings -> {
                    navController.navigate(R.id.navigation_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Simplified permission handling to prevent crashes
        try {
            val permissionsToRequest = mutableListOf<String>()

            // Check Bluetooth permissions (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }

            // Check location permissions (required for Bluetooth scanning)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } catch (e: Exception) {
            // Ignore permission errors for now to prevent crashes
            showStatusMessage("Permission check failed - some features may be limited", true)
        }
    }

    private fun onBluetoothPermissionGranted() {
        // Initialize Bluetooth functionality
        showStatusMessage("Bluetooth permission granted - Ready to connect!", false)
        // TODO: Initialize Bluetooth adapter and scanning
    }

    private fun onBluetoothPermissionDenied() {
        // Show explanation for Bluetooth permission
        showStatusMessage("Bluetooth permission required for OBD-II connection", true)
        // TODO: Show dialog explaining why Bluetooth permission is needed
    }

    private fun onLocationPermissionGranted() {
        // Location permission granted - can now scan for Bluetooth devices
        showStatusMessage("Location permission granted - Can scan for devices", false)
        // TODO: Enable Bluetooth device scanning
    }

    private fun onLocationPermissionDenied() {
        // Show explanation for location permission
        showStatusMessage("Location permission required for Bluetooth scanning", true)
        // TODO: Show dialog explaining why location permission is needed for Bluetooth
    }

    private fun showStatusMessage(message: String, isError: Boolean) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)

        if (isError) {
            snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.status_error))
        } else {
            snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.status_connected))
        }

        snackbar.setTextColor(ContextCompat.getColor(this, R.color.hurtec_on_primary))
        snackbar.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
