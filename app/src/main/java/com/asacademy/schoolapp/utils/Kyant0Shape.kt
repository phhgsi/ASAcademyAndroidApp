package com.asacademy.schoolapp.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Kyant0-style continuous curvature Capsule & Smooth Squircle shapes for Jetpack Compose.
 */
object Kyant0Shapes {
    val Capsule: Shape = RoundedCornerShape(percent = 50)
    val CardSquircle: Shape = RoundedCornerShape(20.dp)
    val ButtonSquircle: Shape = RoundedCornerShape(50)
    val AvatarSquircle: Shape = RoundedCornerShape(18.dp)
    val PillChip: Shape = RoundedCornerShape(50)
}
