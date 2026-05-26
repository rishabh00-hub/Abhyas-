package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Cosmic Slate Primary Palette
val CosmicBackground = Color(0xFF020617) // deep slate black
val CosmicSurface = Color(0xFF0F172A)    // dark slate card background
val CosmicSurfaceVariant = Color(0xFF1E293B) // lighter slate
val CosmicBorder = Color(0xFF312E81)     // deep indigo border
val CosmicPrimary = Color(0xFF4F46E5)    // glow indigo
val CosmicSecondary = Color(0xFF6366F1)  // soft indigo
val CosmicAccentCheck = Color(0xFF10B981) // Emerald accent for accuracy/complete
val CosmicAccentCritical = Color(0xFFEF4444) // Crimson warning accent
val CosmicAccentAlert = Color(0xFFF59E0B)    // Amber warning

// Subject-Specific Palette (for Visual Mapping)
val ColorPhysics = Color(0xFF3B82F6)    // Sky Blue
val ColorChemistry = Color(0xFFEC4899)  // Rose pink
val ColorMaths = Color(0xFF10B981)      // Green-emerald
val ColorBiology = Color(0xFF8B5CF6)    // Violet-purple
val ColorGeneral = Color(0xFFF59E0B)    // Amber
val ColorOther = Color(0xFF64748B)      // Cool Slate

fun subjectBadgeColor(subjectName: String): Color = when (subjectName) {
    "Physics" -> ColorPhysics
    "Chemistry" -> ColorChemistry
    "Maths" -> ColorMaths
    "Biology" -> ColorBiology
    "General" -> ColorGeneral
    else -> ColorOther
}
