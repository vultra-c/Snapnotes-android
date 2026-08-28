package com.whyy.snapnotes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.ui.components.glasense.GlasenseSurfaceCard
import com.whyy.snapnotes.ui.components.glasense.pressEffect
import com.whyy.snapnotes.ui.components.glasense.rememberPressInteractionSource

/**
 * 通用可折叠教程卡片（iOS 设置行风格）：浅蓝图标徽章 + 标题/副标题 + 旋转 chevron，
 * 点击展开/收起，内容随高度动画。内容区由 [content] 提供（通常放 [MarkdownText]）。
 */
@Composable
fun TutorialCard(
    title: String,
    subtitle: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "TutorialChevron"
    )
    val interaction = rememberPressInteractionSource()

    GlasenseSurfaceCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .pressEffect(interaction)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlasenseTheme.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = GlasenseTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = GlasenseTheme.type.bodyEmphasized,
                        color = GlasenseTheme.colors.content
                    )
                    Text(
                        text = subtitle,
                        style = GlasenseTheme.type.footnote,
                        color = GlasenseTheme.colors.contentVariant
                    )
                }
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_down),
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}
