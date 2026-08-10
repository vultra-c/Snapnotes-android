package com.whyy.snapnotes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.whyy.snapnotes.ui.liquid.LiquidGlassPopupSurface
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 可复用的「更多」菜单按钮：显示 More 图标，点击弹出液态玻璃下拉菜单。
 *
 * 菜单项按需显示（传入 null 则不显示对应项）：
 * - 创建文件夹（[onCreateFolder] 非 null 时显示）
 * - Amadeus 对话（[onOpenAmadeusChat] 非 null 时显示）
 * - Amadeus 设置（[onOpenAmadeusConfig] 非 null 时显示）
 */
@Composable
fun MoreMenu(
    onCreateFolder: (() -> Unit)? = null,
    onOpenAmadeusChat: (() -> Unit)? = null,
    onOpenAmadeusConfig: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showPopup by remember { mutableStateOf(false) }

    data class MenuItem(val text: String, val icon: ImageVector, val onClick: () -> Unit)
    val items = remember(onCreateFolder, onOpenAmadeusChat, onOpenAmadeusConfig) {
        buildList {
            onCreateFolder?.let {
                add(MenuItem("创建文件夹", MiuixIcons.Folder) {
                    showPopup = false
                    it()
                })
            }
            onOpenAmadeusChat?.let {
                add(MenuItem("Amadeus 对话", MiuixIcons.Notes) {
                    showPopup = false
                    it()
                })
            }
            onOpenAmadeusConfig?.let {
                add(MenuItem("Amadeus 设置", MiuixIcons.Settings) {
                    showPopup = false
                    it()
                })
            }
        }
    }

    if (items.isEmpty()) return

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        IconButton(onClick = { showPopup = true }) {
            Icon(
                imageVector = MiuixIcons.More,
                contentDescription = "更多"
            )
        }
        // 液态玻璃弹出层：与全应用统一的毛玻璃风格
        LiquidGlassPopupSurface(
            visible = showPopup,
            onDismissRequest = { showPopup = false },
            shape = RoundedCornerShape(20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    BasicComponent(
                        title = item.text,
                        startAction = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 6.dp, end = 14.dp)
                                    .size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = item.onClick
                            )
                            .padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}
