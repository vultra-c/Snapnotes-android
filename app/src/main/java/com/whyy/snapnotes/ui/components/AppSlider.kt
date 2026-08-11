package com.whyy.snapnotes.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val accentColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
    val trackColor = Color(0xFF787878).copy(alpha = 0.2f)
    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
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
        val currentValue by rememberUpdatedState(value)
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentValue() }
                .collectLatest { newValue ->
                    if (dampedDragAnimation.targetValue != newValue) {
                        dampedDragAnimation.updateValue(newValue)
                    }
                }
        }
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
                .background(accentColor, androidx.compose.foundation.shape.CircleShape)
                .size(40f.dp, 24f.dp)
        )
    }
}
