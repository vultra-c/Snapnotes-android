package com.whyy.snapnotes.ui.components.glasense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * 玻璃拟态纯色卡：圆角卡面 + 顶部高光渐变 + 1px 内描边，供列表内使用。
 * 列表内容挂 layerBackdrop 供固定 header / 底部导航采样，列表内卡片若再采样
 * 同一 backdrop 会形成渲染树环导致崩溃，因此列表内一律用这种静态玻璃质感卡面。
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
            .drawWithContent {
                drawContent()
                // 顶部高光：模拟玻璃对光的反射，自上而下快速衰减。
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.28f
                    )
                )
                // 内描边：1px 半透明描边收束边缘，玻璃片的「厚度」感。
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        content()
    }
}
