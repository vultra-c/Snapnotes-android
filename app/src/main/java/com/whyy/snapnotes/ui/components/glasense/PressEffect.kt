package com.whyy.snapnotes.ui.components.glasense

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 统一的按压反馈：按住时轻微放大 + 白色高光叠加（Apple 式 press flash）。
 * 长条形按钮横向缩放明显，幅度默认收敛到 1.04。
 */
fun Modifier.pressEffect(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 1.04f,
    pressedFlashAlpha: Float = 0.16f,
    enabled: Boolean = true
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(0.6f, 400f),
        label = "PressScale"
    )
    val flash by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedFlashAlpha else 0f,
        animationSpec = spring(0.6f, 400f),
        label = "PressFlash"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }
        .drawWithContent {
            drawContent()
            if (flash > 0.001f) {
                drawRect(
                    color = Color.White,
                    alpha = flash,
                    blendMode = BlendMode.Plus
                )
            }
        }
}

/** 便捷创建 interactionSource 的组合（配合 pressEffect 使用）。 */
@Composable
fun rememberPressInteractionSource(): MutableInteractionSource =
    remember { MutableInteractionSource() }
