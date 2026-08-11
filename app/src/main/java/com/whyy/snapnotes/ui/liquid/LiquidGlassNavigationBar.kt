package com.whyy.snapnotes.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class LiquidGlassNavTab(
    val icon: ImageVector,
    val label: String
)

/**
 * 底部导航栏。
 *
 * 液态玻璃效果已整体下线：导航栏与页面其它组件一样使用不透明的普通样式，
 * 不再采样背景/半透明渲染，避免部分机型出现界面半透明、掉帧等问题。
 * 组件名与参数签名保持不变，调用方无需改动。
 */
@Composable
fun LiquidGlassNavigationBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    containerColor: Color,
    accentColor: Color,
    shape: Shape
) {
    LiquidGlassNavigationBarPlain(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        tabs = tabs,
        modifier = modifier,
        containerColor = containerColor,
        accentColor = accentColor,
        shape = shape
    )
}

@Composable
private fun LiquidGlassNavigationBarPlain(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    containerColor: Color,
    accentColor: Color,
    shape: Shape
) {
    val textColor = MiuixTheme.colorScheme.onSurface
    val unselectedColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(containerColor, shape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTabIndex() == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) accentColor else unselectedColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) accentColor else textColor
                )
            }
        }
    }
}
