package com.whyy.snapnotes.ui.liquid

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop

data class LiquidGlassConfig(
    val enabled: Boolean = false,
    val blurRadiusDp: Float = 8f,
    val refractionAmountDp: Float = 16f,
    val refractionHeightDp: Float = 10f,
    val chromaticAberration: Boolean = false,
    val interactive: Boolean = true
)

val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }
