package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val rootBackdrop = LocalLiquidGlassBackdrop.current
    val useGlass = config.enabled && rootBackdrop != null && isRenderEffectSupported()

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    if (useGlass) {
        val backdrop = rootBackdrop
        val isInteractive = config.interactive
        // Keep visual tuning outside the effect builder so it is shared by the
        // backdrop highlight and the effect configuration.
        val subtle = if (config.subtleMode) 0.42f else 1f
        val highlightScale = if (config.subtleMode) 0.55f else 1f
        Box(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        // 柔和模式保留玻璃层次，但把折射/模糊压到阅读友好的范围。
                        vibrancy()
                        blur(config.blurRadiusDp.dp.toPx() * subtle)
                        lens(
                            config.refractionHeightDp.dp.toPx() * subtle,
                            config.refractionAmountDp.dp.toPx() * subtle,
                            depthEffect = true,
                            chromaticAberration = config.chromaticAberration
                        )
                    },
                    layerBlock = if (isInteractive) {
                        {
                            val width = size.width
                            val height = size.height

                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 2f.dp.toPx() / height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 2f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                        (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                        (height / width).fastCoerceAtMost(1f)
                        }
                    } else {
                        null
                    },
                    highlight = {
                        val progress = interactiveHighlight.pressProgress
                        if (progress > 0f) {
                            Highlight.Default.copy(alpha = progress * highlightScale)
                        } else {
                            null
                        }
                    },
                    shadow = {
                        Shadow(
                            radius = 6f.dp,
                            color = Color.Black.copy(alpha = 0.04f)
                        )
                    },
                    innerShadow = {
                        val progress = interactiveHighlight.pressProgress
                        if (progress > 0f) {
                            InnerShadow(
                                radius = 6f.dp * progress,
                                alpha = progress * 0.7f
                            )
                        } else {
                            null
                        }
                    },
                    onDrawSurface = {
                        // 更稳定的表面底色减少背景折射对正文对比度的干扰。
                        drawRect(containerColor.copy(alpha = 0.48f))
                    }
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (isInteractive) {
                        Modifier
                            .then(interactiveHighlight.modifier)
                            .then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    }
                )
                .padding(contentPadding),
            content = content
        )
    } else {
        Box(
            modifier
                .clip(shape)
                .background(containerColor)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(contentPadding),
            content = content
        )
    }
}
