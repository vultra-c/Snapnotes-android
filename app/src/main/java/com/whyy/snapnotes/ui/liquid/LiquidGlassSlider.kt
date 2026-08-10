package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
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

@Composable
fun LiquidGlassSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val subtle = if (config.subtleMode) 0.42f else 1f
    val rootBackdrop = LocalLiquidGlassBackdrop.current
    // 玻璃效果仅保留在底部导航栏；滑块始终使用普通样式。
    val useGlass = false

    val accentColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
    val trackColor = Color(0xFF787878).copy(alpha = 0.2f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        // 拖动/点击由下方全宽手势层驱动；DampedDragAnimation 只负责液态挤压反馈与拇指动画。
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = (valueRange.endInclusive - valueRange.start) / 100f,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> }
            )
        }
        // 外部值变化时同步拇指位置：经 rememberUpdatedState 始终读取最新 value，
        // 避免 snapshotFlow 捕获到首次组合的旧闭包导致拇指不跟手。
        val currentValue by rememberUpdatedState(value)
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentValue() }
                .collectLatest { newValue ->
                    if (dampedDragAnimation.targetValue != newValue) {
                        dampedDragAnimation.updateValue(newValue)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(trackColor)
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // 全宽手势层（透明）：点击定位 + 水平拖动。拖动锁定水平方向，
        // 垂直方向手势不被消费，仍由外层页面滚动处理（修复滑块被页面滑动抢走、无法调整的问题）。
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(valueRange, trackWidth, isLtr) {
                    detectTapGestures { position ->
                        val ratio = (position.x / trackWidth).coerceIn(0f, 1f)
                        val range = valueRange.endInclusive - valueRange.start
                        val targetValue = if (isLtr) {
                            valueRange.start + range * ratio
                        } else {
                            valueRange.endInclusive - range * ratio
                        }
                        dampedDragAnimation.animateToValue(targetValue)
                        onValueChange(targetValue)
                    }
                }
                .pointerInput(dampedDragAnimation, valueRange, trackWidth, isLtr) {
                    var startValue = value()
                    var totalDelta = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            startValue = currentValue()
                            totalDelta = 0f
                            dampedDragAnimation.press()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDelta += dragAmount
                            val range = valueRange.endInclusive - valueRange.start
                            val target = (startValue + range * (totalDelta / trackWidth) * (if (isLtr) 1f else -1f))
                                .coerceIn(valueRange)
                            onValueChange(target)
                            // 拖动时直接驱动拇指动画，保证即时跟手
                            dampedDragAnimation.updateValue(target)
                        },
                        onDragEnd = { dampedDragAnimation.release() },
                        onDragCancel = { dampedDragAnimation.release() }
                    )
                }
        )

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(
                    if (useGlass) {
                        Modifier.drawBackdrop(
                            backdrop = rememberCombinedBackdrop(
                                rootBackdrop!!,
                                rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                    val progress = dampedDragAnimation.pressProgress
                                    val scaleX = lerp(2f / 3f, 1f, progress)
                                    val scaleY = lerp(0f, 1f, progress)
                                    scale(scaleX, scaleY) {
                                        drawBackdrop()
                                    }
                                }
                            ),
                            shape = { androidx.compose.foundation.shape.CircleShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                blur(8f.dp.toPx() * (1f - progress) * subtle)
                                lens(
                                    10f.dp.toPx() * progress * subtle,
                                    14f.dp.toPx() * progress * subtle,
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
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(Color.White.copy(alpha = 1f - progress))
                            }
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    if (useGlass) Color.Transparent else accentColor,
                    androidx.compose.foundation.shape.CircleShape
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
