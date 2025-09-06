package com.hurtec.obd2.diagnostics.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Hurtec OBD-II Shapes
 * Modern rounded corners with automotive-inspired design
 */

val HurtecShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Custom shapes for automotive components
val AutomotiveShapes = object {
    val gauge = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(20.dp)
    val dialog = RoundedCornerShape(24.dp)
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
