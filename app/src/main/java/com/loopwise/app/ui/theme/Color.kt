package com.loopwise.app.ui.theme

import androidx.compose.ui.graphics.Color

// Curated palette for "build" habits — bright, saturated (spec §5).
// Used for boolean/counter/duration habits and tasks.
object BuildPalette {
    val Blue = Color(0xFF3B82F6)
    val Teal = Color(0xFF14B8A6)
    val Amber = Color(0xFFF59E0B)
    val Rose = Color(0xFFF43F5E)
    val Violet = Color(0xFF8B5CF6)
    val Green = Color(0xFF22C55E)
    val Cyan = Color(0xFF06B6D4)
    val Pink = Color(0xFFEC4899)
    val Orange = Color(0xFFF97316)
    val Indigo = Color(0xFF6366F1)
    val Lime = Color(0xFF84CC16)
    val Sky = Color(0xFF0EA5E9)

    val all = listOf(Blue, Teal, Amber, Rose, Violet, Green, Cyan, Pink, Orange, Indigo, Lime, Sky)
}

// Muted/dark palette for avoidance ("quit") habits — reinforces the
// restraint framing vs. the bright build-habit palette (spec §2.3, §5).
object QuitPalette {
    val Charcoal = Color(0xFF37474F)
    val Maroon = Color(0xFF8E2438)
    val DeepBrown = Color(0xFF5D4037)
    val ForestGreen = Color(0xFF2E5339)
    val NavyInk = Color(0xFF1E293B)
    val Rust = Color(0xFFB3492D)

    val all = listOf(Charcoal, Maroon, DeepBrown, ForestGreen, NavyInk, Rust)
}

val LoopwisePrimary = Color(0xFF5B5FEF)
val LoopwiseOnPrimary = Color(0xFFFFFFFF)
val LoopwiseBackground = Color(0xFFF7F7FB)
val LoopwiseSurface = Color(0xFFFFFFFF)
val LoopwiseOnSurface = Color(0xFF1C1B1F)
val LoopwiseOnSurfaceVariant = Color(0xFF6B7280)

val LoopwiseBackgroundDark = Color(0xFF121212)
val LoopwiseSurfaceDark = Color(0xFF1E1E1E)
val LoopwiseOnSurfaceDark = Color(0xFFECECEC)
val LoopwiseOnSurfaceVariantDark = Color(0xFFA0A0A8)
