package com.whyy.snapnotes.toolkit.gaussiangradient

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toColorLong

@Composable
fun Modifier.smoothGradientMask(
    color: Color,
    start: Float,
    end: Float,
    intensity: Float
): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        smoothGradientShaderMask(color, start, end, intensity)
    } else {
        smoothGradientBrushMask(color, start, end, intensity)
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.smoothGradientShaderMask(
    color: Color,
    start: Float,
    end: Float,
    intensity: Float
): Modifier {
    val shader = remember { RuntimeShader(GRADIENT_SHADER) }
    val brush = remember { ShaderBrush(shader) }

    // Apply the drawing logic behind the content of the composable.
    return this.drawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setColorUniform("tint", color.toColorLong())
        shader.setFloatUniform("tintIntensity", intensity)
        shader.setFloatUniform("tintRange", start, end)

        // Draw a rectangle covering the entire drawing area with the shader brush.
        drawRect(brush = brush)
    }
}

@Composable
private fun Modifier.smoothGradientBrushMask(
    color: Color,
    start: Float,
    end: Float,
    intensity: Float
): Modifier {
    val colors = remember(color, start, end, intensity) {
        smoothStepGradientColors(color, start, end, intensity)
    }

    return this.drawWithCache {
        val brush = Brush.verticalGradient(
            colors = colors,
            startY = 0f,
            endY = size.height
        )

        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}

private const val SmoothStepGradientSamples = 32

private fun smoothStepGradientColors(
    color: Color,
    start: Float,
    end: Float,
    intensity: Float
): List<Color> =
    List(SmoothStepGradientSamples + 1) { index ->
        val p = index / SmoothStepGradientSamples.toFloat()
        val mask = smoothStep(start, end, p)
        val alpha = (intensity * mask).coerceIn(0f, 1f)

        color.copy(alpha = alpha)
    }

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge0 == edge1) {
        return if (x < edge0) 0f else 1f
    }

    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
