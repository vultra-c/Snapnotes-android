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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 统一的按压反馈（iOS 式）：按住时轻微缩小 + 内容变暗，快速响应、立即回弹。
 * 深浅色卡片上都清晰可感知。
 */
fun Modifier.pressEffect(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
    pressedDimAlpha: Float = 0.08f,
    enabled: Boolean = true
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "PressScale"
    )
    val dim by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedDimAlpha else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "PressDim"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }
        .drawWithContent {
            drawContent()
            if (dim > 0.001f) {
                drawRect(color = Color.Black.copy(alpha = dim))
            }
        }
}

/** 便捷创建 interactionSource 的组合（配合 pressEffect 使用）。 */
@Composable
fun rememberPressInteractionSource(): MutableInteractionSource =
    remember { MutableInteractionSource() }
