package com.example.vivora

import androidx.compose.ui.graphics.Brush
import com.example.vivora.ui.theme.*

// Main screen backdrop — dark navy fading into a deep metallic green
val VivoraBackgroundGradient = Brush.verticalGradient(
    colors = listOf(DeepNavy, MidnightBlue, MetallicGreenDark)
)

// For buttons, upload box, avatars — the actual "metallic shine" gradient
val VivoraAccentGradient = Brush.linearGradient(
    colors = listOf(MetallicGreenLight, MetallicGreen, MetallicGreenDark)
)

// Subtle gradient for cards sitting on top of the background
val VivoraCardGradient = Brush.linearGradient(
    colors = listOf(SurfaceBlue, MidnightBlue)
)

// Splash screen — consistent with main brand, but using a radial glow for impact
val VivoraSplashGradient = Brush.radialGradient(
    colors = listOf(MetallicGreen, MidnightBlue, DeepNavy),
    radius = 1200f
)