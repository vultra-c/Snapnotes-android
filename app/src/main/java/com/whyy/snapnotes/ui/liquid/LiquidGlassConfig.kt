package com.whyy.snapnotes.ui.liquid

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop

data class LiquidGlassConfig(
    val enabled: Boolean = false,
    // 默认更克制：降低折射与模糊，让玻璃“安静”不抢阅读注意
    val blurRadiusDp: Float = 5f,
    val refractionAmountDp: Float = 8f,
    val refractionHeightDp: Float = 6f,
    val chromaticAberration: Boolean = false,
    val interactive: Boolean = true,
    val subtleMode: Boolean = true
)

val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }
