package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 液态玻璃开关（参考 AndroidLiquidGlass 的 LiquidToggle 示例）。
 *
 * - 玻璃开启时：轨道为半透明玻璃层，滑块叠加毛玻璃 + 折射，按住滑块可左右拖动，
 *   拖动中滑块随手指位移并有「液态挤压」反应（[DampedDragAnimation] 驱动）。
 * - 玻璃关闭 / 不支持 RenderEffect 时：回退为普通开关，单击切换。
 */
@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val subtle = if (config.subtleMode) 0.42f else 1f
    val rootBackdrop = LocalLiquidGlassBackdrop.current
    val useGlass = config.enabled && rootBackdrop != null && isRenderEffectSupported()

    val accentColor = MiuixTheme.colorScheme.primary
    val trackColor = Color(0xFF787878).copy(alpha = 0.2f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val touchSlop = with(density) { 8f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    // DampedDragAnimation 挂在 remember 上，回调闭包只在首次组合创建。
    // checked / onCheckedChange 必须经 rememberUpdatedState 读取最新值，
    // 否则回调里拿到的永远是第一次组合的旧状态（表现为开关只能切换一次）。
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    // 拖动结束：按手指最终停留的位置吸附到最近的端点（读 fraction 而非 targetValue，
                    // 避免微小的抖动被当成拖动后吸附回原值、导致开关“点了没反应”）。
                    val snapped = if (fraction >= 0.5f) 1f else 0f
                    fraction = snapped
                    currentOnCheckedChange(snapped == 1f)
                }
                didDrag = false
                totalDrag = 0f
            },
            onDrag = { _, dragAmount ->
                totalDrag += dragAmount.x
                // 超过触摸阈值才认定为拖动；纯点击（含轻微抖动）不进入拖动分支，交给下方点击切换。
                if (!didDrag && abs(totalDrag) > touchSlop) {
                    didDrag = true
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(checked) {
        snapshotFlow { checked }
            .collectLatest { isChecked ->
                val target = if (isChecked) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    if (useGlass) {
        val backdrop = rootBackdrop
        val trackBackdrop = rememberLayerBackdrop()

        Box(modifier, contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .layerBackdrop(trackBackdrop)
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind {
                        val fraction = dampedDragAnimation.value
                        drawRect(lerp(trackColor, accentColor, fraction))
                    }
                    .size(64.dp, 28.dp)
                    // 整轨任意位置轻点即切换（不要求点中滑块）。拖动分支 didDrag==false 时才响应，避免双触发
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (!didDrag) {
                                val newChecked = !currentChecked
                                fraction = if (newChecked) 1f else 0f
                                currentOnCheckedChange(newChecked)
                            }
                        })
                    }
            )

            Box(
                Modifier
                    .graphicsLayer {
                        val fraction = dampedDragAnimation.value
                        val padding = 2f.dp.toPx()
                        translationX =
                            if (isLtr) lerp(padding, padding + dragWidth, fraction)
                            else lerp(-padding, -(padding + dragWidth), fraction)
                    }
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(
                            backdrop,
                            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                val progress = dampedDragAnimation.pressProgress
                                val scaleX = lerp(2f / 3f, 0.75f, progress)
                                val scaleY = lerp(0f, 0.75f, progress)
                                scale(scaleX, scaleY) {
                                    drawBackdrop()
                                }
                            }
                        ),
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            blur(8f.dp.toPx() * (1f - progress) * subtle)
                            lens(
                                5f.dp.toPx() * progress * subtle,
                                10f.dp.toPx() * progress * subtle,
                                chromaticAberration = config.chromaticAberration
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = progress * if (config.subtleMode) 0.55f else 1f
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(alpha = 0.05f)
                            )
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 4f.dp * progress,
                                alpha = progress * if (config.subtleMode) 0.7f else 1f
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 50f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(Color.White.copy(alpha = 1f - progress))
                        }
                    )
                    .size(40.dp, 24.dp)
            )
        }
    } else {
        Box(modifier, contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (checked) accentColor else trackColor)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onCheckedChange(!checked) }
                    )
                    .size(64.dp, 28.dp)
            )
            Box(
                Modifier
                    .graphicsLayer {
                        val padding = 2f.dp.toPx()
                        translationX =
                            if (isLtr) lerp(padding, padding + dragWidth, if (checked) 1f else 0f)
                            else lerp(-padding, -(padding + dragWidth), if (checked) 1f else 0f)
                    }
                    .background(Color.White, CircleShape)
                    .size(24.dp, 24.dp)
            )
        }
    }
}
