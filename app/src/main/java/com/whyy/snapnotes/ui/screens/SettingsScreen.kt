package com.whyy.snapnotes.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.theme.AppColors
import com.whyy.snapnotes.theme.AppSpecs
import com.whyy.snapnotes.ui.components.glasense.GlasensePageHeader
import com.whyy.snapnotes.ui.components.glasense.GlasenseSwitch
import com.whyy.snapnotes.ui.components.packed.ConfigContainer
import com.whyy.snapnotes.ui.components.packed.ConfigEntryItem
import com.whyy.snapnotes.ui.components.packed.PageContent
import com.whyy.snapnotes.ui.theme.AppearanceMode

@Composable
fun SettingsScreen(
    appearanceMode: AppearanceMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    useBuiltinFileManager: Boolean,
    onUseBuiltinFileManagerChange: (Boolean) -> Unit,
    lastExportDirSummary: String?,
    onPickExportDir: () -> Unit,
    onOpenAbout: () -> Unit,
    onResetFirstSyncConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    PageContent(
        state = lazyListState,
        modifier = modifier,
        tabPadding = true
    ) {
        item {
            GlasensePageHeader(title = "设置")
        }
        item {
            ConfigContainer(title = "外观", backgroundColor = AppColors.cardBackground) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppearanceModeRow(
                        selected = appearanceMode,
                        onSelect = onAppearanceModeChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SwitchRow(
                        title = "动态取色",
                        summary = "开启后按系统壁纸生成整套配色（Monet）",
                        checked = dynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            }
            VGap()
        }
        item {
            ConfigContainer(title = "导入", backgroundColor = AppColors.cardBackground) {
                SwitchRow(
                    title = "使用内置文件管理器",
                    summary = "开启后用应用内文件浏览器选择 JSON；关闭后调用系统文件选择器",
                    checked = useBuiltinFileManager,
                    onCheckedChange = onUseBuiltinFileManagerChange
                )
            }
            VGap()
        }
        item {
            ConfigContainer(title = "导出", backgroundColor = AppColors.cardBackground) {
                ConfigEntryItem(
                    color = AppColors.primary,
                    icon = painterResource(R.drawable.ic_folder),
                    title = "导出目录",
                    onClick = onPickExportDir
                )
            }
            VGap()
        }
        item {
            ConfigContainer(title = "其他", backgroundColor = AppColors.cardBackground) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ConfigEntryItem(
                        color = AppColors.primary,
                        icon = painterResource(R.drawable.ic_arrow_counterclockwise),
                        title = "重置首次同步确认",
                        onClick = onResetFirstSyncConfirm
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ConfigEntryItem(
                        color = AppColors.primary,
                        icon = painterResource(R.drawable.ic_mini_info),
                        title = "关于",
                        onClick = onOpenAbout
                    )
                }
            }
        }
        paddingItem(lazyListState)
    }
}

/** 主题模式选择行：标题 + 右侧三段式选择（跟随系统 / 浅色 / 深色）。 */
@Composable
private fun AppearanceModeRow(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "应用主题",
                style = GlasenseTheme.type.body,
                color = AppColors.content
            )
            Text(
                text = "选择浅色、深色或跟随系统",
                style = GlasenseTheme.type.footnote,
                color = AppColors.contentVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .background(AppColors.segmentedControlBackground, AppSpecs.buttonShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val options = AppearanceMode.entries
            options.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier
                        .then(
                            if (isSelected) Modifier.background(
                                AppColors.segmentedControlIndicator,
                                AppSpecs.buttonShape
                            ) else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(mode) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = mode.label,
                        style = GlasenseTheme.type.footnote,
                        color = if (isSelected) AppColors.onSegmentedControlIndicator else AppColors.onSegmentedControlBackground
                    )
                }
            }
        }
    }
}

/** 通用开关行：标题 + 说明 + 右侧 Glasense 开关。 */
@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlasenseTheme.type.body,
                color = AppColors.content
            )
            Text(
                text = summary,
                style = GlasenseTheme.type.footnote,
                color = AppColors.contentVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        GlasenseSwitch(
            backgroundColor = AppColors.inactiveTrack,
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
