package com.whyy.snapnotes.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

/**
 * 可复用的「更多」菜单按钮：显示 More 图标，点击弹出下拉菜单。
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
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    var showPopup by remember { mutableStateOf(false) }

    // 构建菜单项列表：仅包含回调非 null 的项
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
        OverlayListPopup(
            show = showPopup,
            popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { showPopup = false }
        ) {
            ListPopupColumn {
                items.forEachIndexed { index, item ->
                    DropdownImpl(
                        item = DropdownItem(
                            text = item.text,
                            icon = { m ->
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = m
                                )
                            }
                        ),
                        optionSize = items.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = { item.onClick() }
                    )
                }
            }
        }
    }
}
