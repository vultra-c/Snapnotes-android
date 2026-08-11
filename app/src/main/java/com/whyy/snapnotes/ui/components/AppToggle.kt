package com.whyy.snapnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = MiuixTheme.colorScheme.primary
    val trackColor = Color(0xFF787878).copy(alpha = 0.2f)
    val density = LocalDensity.current
    val plainTrackTravel = with(density) { 36f.dp.toPx() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
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
                        if (isLtr) lerp(padding, padding + plainTrackTravel, if (checked) 1f else 0f)
                        else lerp(-padding, -(padding + plainTrackTravel), if (checked) 1f else 0f)
                }
                .background(Color.White, CircleShape)
                .size(24.dp, 24.dp)
        )
    }
}
