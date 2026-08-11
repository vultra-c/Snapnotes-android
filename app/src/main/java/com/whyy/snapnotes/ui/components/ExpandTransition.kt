package com.whyy.snapnotes.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class ExpandOrigin(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    companion object {
        val None = ExpandOrigin(0f, 0f, 0f, 0f)
    }
}

@Composable
fun ExpandTransition(
    origin: ExpandOrigin,
    onBackRequested: () -> Unit,
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier,
    originCornerRadiusDp: Float = 28f,
    scrimMaxAlpha: Float = 0.32f,
    maxBlurRadiusDp: Float = 32f,
    content: @Composable (onRequestExit: () -> Unit) -> Unit
) {
    val density = LocalDensity.current
    var rendering by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val enterSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun requestExit() {
        if (!finishing) {
            finishing = true
            onBackRequested()
        }
    }

    BackHandler(enabled = rendering && !finishing) { requestExit() }

    LaunchedEffect(Unit) {
        rendering = true
        progress.animateTo(1f, enterSpec)
    }

    LaunchedEffect(finishing) {
        if (finishing) {
            progress.snapTo(1f)
            progress.animateTo(
                0f,
                tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            onExitFinished()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val targetW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val targetH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val originW = max(origin.width, 1f)
        val originH = max(origin.height, 1f)
        val scale = if (originW >= targetW) {
            1f
        } else {
            (originW / targetW) + (1f - originW / targetW) * progress.value
        }
        val originCenterX = origin.left + originW / 2f
        val originCenterY = origin.top + originH / 2f
        val targetCenterX = targetW / 2f
        val targetCenterY = targetH / 2f
        val offsetX = (originCenterX - targetCenterX) * (1f - progress.value)
        val offsetY = (originCenterY - targetCenterY) * (1f - progress.value)
        val cornerRadius = (originCornerRadiusDp * (1f - progress.value)).coerceAtLeast(0f).dp
        val scrimAlpha = scrimMaxAlpha * progress.value
        if (rendering) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = scrimAlpha }
                        .background(Color.Black)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .clip(RoundedCornerShape(cornerRadius))
                ) {
                    content(::requestExit)
                }
            }
        }
    }
}
