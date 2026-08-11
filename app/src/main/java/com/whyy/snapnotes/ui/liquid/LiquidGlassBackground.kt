package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 普通页面背景。
 *
 * 液态玻璃已整体下线：背景只绘制不透明的主题底色，不再挂载 backdrop 采样层。
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    accentColor: Color,
    secondaryColor: Color
) {
    Box(
        modifier
            .fillMaxSize()
            .background(backgroundColor)
    )
}
