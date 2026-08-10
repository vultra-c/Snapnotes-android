package com.whyy.snapnotes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whyy.snapnotes.ui.components.FolderCreationDialog
import com.whyy.snapnotes.ui.components.MoreMenu
import com.whyy.snapnotes.ui.liquid.LiquidGlassCard
import com.whyy.snapnotes.ui.liquid.LiquidGlassConfig
import com.whyy.snapnotes.ui.liquid.LiquidGlassSlider
import com.whyy.snapnotes.ui.liquid.LiquidGlassToggle
import com.whyy.snapnotes.ui.theme.AppearanceMode
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsScreen(
    appearanceMode: AppearanceMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
    onLiquidGlassConfigChange: (LiquidGlassConfig) -> Unit = {},
    useBuiltinFileManager: Boolean,
    onUseBuiltinFileManagerChange: (Boolean) -> Unit,
    lastExportDirSummary: String?,
    onPickExportDir: () -> Unit,
    onOpenAbout: () -> Unit,
    onResetFirstSyncConfirm: () -> Unit,
    onCreateFolder: (String) -> Unit = {},
    // 试验性功能（可选，由外层传入；未传则显示为关闭/不可用但仍展示入口）
    experimentalPagesPreview: Boolean = false,
    onExperimentalPagesPreviewChange: (Boolean) -> Unit = {},
    experimentalInlineSearch: Boolean = false,
    onExperimentalInlineSearchChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var showFolderDialog by remember { mutableStateOf(false) }
    var experimentalExpanded by remember { mutableStateOf(false) }

    if (showFolderDialog) {
        FolderCreationDialog(
            show = true,
            onConfirm = { name ->
                showFolderDialog = false
                onCreateFolder(name)
            },
            onDismiss = { showFolderDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = "设置",
                largeTitle = "设置",
                scrollBehavior = scrollBehavior,
                actions = {
                    MoreMenu(
                        onCreateFolder = { showFolderDialog = true }
                    )
                }
            )
        },
        popupHost = {}
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .overScrollVertical()
                .scrollEndHaptic(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 40.dp
            )
        ) {
            item {
                SmallTitle(text = "外观", modifier = Modifier.padding(top = 12.dp))
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        WindowDropdownPreference(
                            title = "应用主题",
                            summary = "选择浅色、深色或跟随系统",
                            items = AppearanceMode.entries.map { it.label },
                            selectedIndex = AppearanceMode.entries.indexOf(appearanceMode).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                onAppearanceModeChange(AppearanceMode.entries[index])
                            }
                        )
                        BasicComponent(
                            title = "动态取色",
                            summary = "开启后按系统壁纸生成整套配色（Monet）",
                            endActions = {
                                LiquidGlassToggle(
                                    checked = dynamicColor,
                                    onCheckedChange = onDynamicColorChange
                                )
                            }
                        )
                    }
                }
            }
            item {
                SmallTitle(text = "导入")
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    BasicComponent(
                        title = "使用内置文件管理器",
                        summary = "开启后用应用内文件浏览器选择 JSON；关闭后调用系统文件选择器",
                        endActions = {
                            LiquidGlassToggle(
                                checked = useBuiltinFileManager,
                                onCheckedChange = onUseBuiltinFileManagerChange
                            )
                        }
                    )
                }
            }
            item {
                SmallTitle(text = "导出")
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    BasicComponent(
                        title = "导出目录",
                        summary = if (lastExportDirSummary != null) {
                            "最近导出到：$lastExportDirSummary"
                        } else {
                            "未导出过；在编辑器点击「导出 JSON 文件」可选择目录"
                        },
                        startAction = {
                            top.yukonga.miuix.kmp.basic.Icon(
                                imageVector = MiuixIcons.Folder,
                                contentDescription = "导出目录",
                                tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        onClick = onPickExportDir
                    )
                }
            }
            // ── 试验性功能：液态玻璃整体与新增试验项迁入此分组，默认关闭不影响流程 ──
            item {
                SmallTitle(text = "试验性功能", modifier = Modifier.padding(top = 12.dp))
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BasicComponent(
                            title = "试验性功能",
                            summary = if (experimentalExpanded) "收起 · 关闭后不影响任何现有流程" else "包含液态玻璃与两项预览特性（默认关闭）",
                            onClick = { experimentalExpanded = !experimentalExpanded },
                            endActions = {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = if (experimentalExpanded) MiuixIcons.Reset else MiuixIcons.Info,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        )
                        if (experimentalExpanded) {
                            // 液态玻璃总开关与细粒度调节
                            BasicComponent(
                                title = "启用液态玻璃",
                                summary = "卡片/导航的毛玻璃与折射；关闭后回退普通样式",
                                endActions = {
                                    LiquidGlassToggle(
                                        checked = liquidGlassConfig.enabled,
                                        onCheckedChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(enabled = v)) }
                                    )
                                }
                            )
                            if (liquidGlassConfig.enabled) {
                                BasicComponent(
                                    title = "柔和模式",
                                    summary = "降低折射/阴影，突出阅读内容",
                                    endActions = {
                                        LiquidGlassToggle(
                                            checked = liquidGlassConfig.subtleMode,
                                            onCheckedChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(subtleMode = v)) }
                                        )
                                    }
                                )
                                BasicComponent(
                                    title = "色散效果",
                                    summary = "边缘彩虹色散（Android 13+）",
                                    endActions = {
                                        LiquidGlassToggle(
                                            checked = liquidGlassConfig.chromaticAberration,
                                            onCheckedChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(chromaticAberration = v)) }
                                        )
                                    }
                                )
                                BasicComponent(
                                    title = "拖动反馈",
                                    summary = "按住拖动时的挤压跟随",
                                    endActions = {
                                        LiquidGlassToggle(
                                            checked = liquidGlassConfig.interactive,
                                            onCheckedChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(interactive = v)) }
                                        )
                                    }
                                )
                                SliderSettingRow(
                                    title = "模糊强度",
                                    summary = "背景模糊半径",
                                    value = liquidGlassConfig.blurRadiusDp,
                                    range = 2f..16f,
                                    onValueChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(blurRadiusDp = v)) }
                                )
                                SliderSettingRow(
                                    title = "折射强度",
                                    summary = "折射位移幅度",
                                    value = liquidGlassConfig.refractionAmountDp,
                                    range = 2f..28f,
                                    onValueChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(refractionAmountDp = v)) }
                                )
                                SliderSettingRow(
                                    title = "折射高度",
                                    summary = "透镜纵深感",
                                    value = liquidGlassConfig.refractionHeightDp,
                                    range = 2f..16f,
                                    onValueChange = { v -> onLiquidGlassConfigChange(liquidGlassConfig.copy(refractionHeightDp = v)) }
                                )
                            }
                            // 两个纯试验项：仅本地开关，后续可对接真实实现
                            BasicComponent(
                                title = "页面预览动效",
                                summary = "进入详情时的共享元素/视差预览（试验，需重启生效）",
                                endActions = {
                                    LiquidGlassToggle(
                                        checked = experimentalPagesPreview,
                                        onCheckedChange = onExperimentalPagesPreviewChange
                                    )
                                }
                            )
                            BasicComponent(
                                title = "列表内联搜索",
                                summary = "在历史/商店列表顶部常驻搜索框（试验）",
                                endActions = {
                                    LiquidGlassToggle(
                                        checked = experimentalInlineSearch,
                                        onCheckedChange = onExperimentalInlineSearchChange
                                    )
                                }
                            )
                        }
                    }
                }
            }
            item {
                SmallTitle(text = "其他")
                LiquidGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BasicComponent(
                            title = "重置首次同步确认",
                            summary = "下次推送时重新显示 Vela 同步注意事项的倒计时确认弹窗",
                            startAction = {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = MiuixIcons.Reset,
                                    contentDescription = "重置",
                                    tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            onClick = onResetFirstSyncConfirm
                        )
                        BasicComponent(
                            title = "关于",
                            summary = "开发者信息、参考项目",
                            startAction = {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = MiuixIcons.Info,
                                    contentDescription = "关于",
                                    tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            onClick = onOpenAbout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSettingRow(
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Spacer(Modifier.width(16.dp))
        LiquidGlassSlider(
            value = { value },
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1.2f)
        )
    }
}
