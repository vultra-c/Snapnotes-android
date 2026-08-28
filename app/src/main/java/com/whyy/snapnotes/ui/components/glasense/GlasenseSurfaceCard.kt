package com.whyy.snapnotes.ui.components.glasense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * 纯色 iOS 卡片：圆角卡面，供列表内使用。
 * 列表内容挂 layerBackdrop 供固定 header / 底部导航采样，列表内卡片若再采样
 * 同一 backdrop 会形成渲染树环导致崩溃，因此列表内一律用纯色卡面。
 */
@Composable
fun GlasenseSurfaceCard(
    shape: Shape,
    modifier: Modifier = Modifier,
    color: Color = GlasenseTheme.colors.cardBackground,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color, shape)
    ) {
        content()
    }
}
