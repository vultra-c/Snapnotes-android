package com.whyy.snapnotes.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * 菜单项：
 * - 创建文件夹
 * - Amadeus 对话（手机端与 AI 直接对话，支持上传文件生成 JSON）
 * - Amadeus 设置（配置 API Key / Model / Base URL）
 */
@Composable
fun MoreMenu(
    onCreateFolder: () -> Unit,
    onOpenAmadeusChat: () -> Unit = {},
    onOpenAmadeusConfig: () -> Unit = {},
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    var showPopup by remember { mutableStateOf(false) }

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
                DropdownImpl(
                    item = DropdownItem(
                        text = "创建文件夹",
                        icon = { m ->
                            Icon(
                                imageVector = MiuixIcons.Folder,
                                contentDescription = null,
                                modifier = m
                            )
                        }
                    ),
                    optionSize = 3,
                    isSelected = false,
                    index = 0,
                    onSelectedIndexChange = {
                        showPopup = false
                        onCreateFolder()
                    }
                )
                DropdownImpl(
                    item = DropdownItem(
                        text = "Amadeus 对话",
                        icon = { m ->
                            Icon(
                                imageVector = MiuixIcons.Notes,
                                contentDescription = null,
                                modifier = m
                            )
                        }
                    ),
                    optionSize = 3,
                    isSelected = false,
                    index = 1,
                    onSelectedIndexChange = {
                        showPopup = false
                        onOpenAmadeusChat()
                    }
                )
                DropdownImpl(
                    item = DropdownItem(
                        text = "Amadeus 设置",
                        icon = { m ->
                            Icon(
                                imageVector = MiuixIcons.Settings,
                                contentDescription = null,
                                modifier = m
                            )
                        }
                    ),
                    optionSize = 3,
                    isSelected = false,
                    index = 2,
                    onSelectedIndexChange = {
                        showPopup = false
                        onOpenAmadeusConfig()
                    }
                )
            }
        }
    }
}
