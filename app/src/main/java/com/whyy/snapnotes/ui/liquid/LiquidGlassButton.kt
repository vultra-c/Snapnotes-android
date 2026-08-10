package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.isRenderEffectSupported
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 液态玻璃主按钮（参考 AndroidLiquidGlass 的 LiquidButton 示例）。
 *
 * - 玻璃开启时：按钮表面叠加毛玻璃 + 折射，按住并拖动时会产生「跟随指尖挤压形变」的
 *   液态反应（[InteractiveHighlight] 驱动，与 [LiquidGlassCard] 同款交互）。
 * - 玻璃关闭 / 不支持 RenderEffect 时：回退为普通主色按钮，保证功能不依赖特效。
 *
 * @param containerColor 玻璃表面底色（普通模式下的按钮底色）
 * @param shape 按钮圆角形状
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MiuixTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable RowScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val rootBackdrop = LocalLiquidGlassBackdrop.current
    // 玻璃效果仅保留在底部导航栏；按钮始终使用普通样式。
    val useGlass = false

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    val clickableModifier = Modifier.clickable(
        interactionSource = null,
        indication = null,
        role = Role.Button,
        enabled = enabled,
        onClick = onClick
    )

    if (useGlass && enabled) {
        val backdrop = rootBackdrop
        val isInteractive = config.interactive
        val subtle = if (config.subtleMode) 0.42f else 1f
        Row(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
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
                    onDrawSurface = {
                        drawRect(containerColor.copy(alpha = 0.70f))
                    }
                )
                .then(clickableModifier)
                .then(
                    if (isInteractive) {
                        Modifier
                            .then(interactiveHighlight.modifier)
                            .then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    }
                )
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    } else {
        Row(
            modifier
                .clip(shape)
                .background(
                    if (enabled) containerColor
                    else containerColor.copy(alpha = 0.38f)
                )
                .then(clickableModifier)
                .alpha(if (enabled) 1f else 0.6f)
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
