package com.whyy.snapnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class AppNavTab(
    val icon: ImageVector,
    val label: String
)

@Composable
fun AppNavigationBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<AppNavTab>,
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
                verticalArrangement = Arrangement.Center
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
