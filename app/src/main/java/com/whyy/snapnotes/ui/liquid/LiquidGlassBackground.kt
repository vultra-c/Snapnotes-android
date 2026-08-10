package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

@Composable
fun LiquidGlassBackground(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    accentColor: Color,
    secondaryColor: Color
) {
    Box(
        modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .drawBehind {
                drawRect(backgroundColor)
                drawRadialBlob(
                    center = Offset(size.width * 0.08f, size.height * 0.14f),
                    radius = size.maxDimension * 0.48f,
                    color = accentColor.copy(alpha = 0.10f)
                )
                drawRadialBlob(
                    center = Offset(size.width * 0.92f, size.height * 0.30f),
                    radius = size.maxDimension * 0.40f,
                    color = secondaryColor.copy(alpha = 0.07f)
                )
                drawRadialBlob(
                    center = Offset(size.width * 0.45f, size.height * 0.95f),
                    radius = size.maxDimension * 0.52f,
                    color = accentColor.copy(alpha = 0.05f)
                )
            }
    )
}

private fun DrawScope.drawRadialBlob(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        )
    )
}
