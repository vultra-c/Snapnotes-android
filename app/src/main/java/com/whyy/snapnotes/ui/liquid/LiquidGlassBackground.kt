package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * 普通页面背景。
 *
 * 页面不再使用液态玻璃背景或彩色光斑，但保留 backdrop 层作为底部导航栏
 * 液态玻璃的背景采样来源。
 */
@Composable
fun LiquidGlassBackground(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    accentColor: Color,
    secondaryColor: Color
) {
    Box(
        modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .drawBehind {
                drawRect(backgroundColor)
            }
    )
}
