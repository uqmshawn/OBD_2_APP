package com.hurtec.obd2.diagnostics.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hurtec.obd2.diagnostics.R

/**
 * Simple test activity to verify app functionality
 */
class TestActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout programmatically
        val textView = TextView(this).apply {
            text = "🚗 Hurtec OBD-II Test\n\nApp is working!\n\nThis is a minimal test to verify the app launches correctly."
            textSize = 18f
            setPadding(64, 64, 64, 64)
            setTextColor(getColor(R.color.hurtec_on_background))
        }
        
        setContentView(textView)
        
        // Set background color
        window.decorView.setBackgroundColor(getColor(R.color.hurtec_background))
    }
}
