package com.whyy.snapnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MiuixTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable RowScope.() -> Unit
) {
    val clickableModifier = Modifier.clickable(
        interactionSource = null,
        indication = null,
        role = Role.Button,
        enabled = enabled,
        onClick = onClick
    )
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
