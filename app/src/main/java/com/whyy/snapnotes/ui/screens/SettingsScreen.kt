package com.whyy.snapnotes.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.theme.AppColors
import com.whyy.snapnotes.theme.AppSpecs
import com.whyy.snapnotes.ui.LocalTabVisible
import com.whyy.snapnotes.ui.pageContentBackdrop
import com.whyy.snapnotes.ui.rememberPageBackdrop
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroHeader
import com.whyy.snapnotes.ui.components.glasense.GlasenseSwitch
import com.whyy.snapnotes.ui.components.packed.ConfigContainer
import com.whyy.snapnotes.ui.components.packed.ConfigEntryItem
import com.whyy.snapnotes.ui.components.packed.PageContent
import com.whyy.snapnotes.ui.theme.AppearanceMode

@Composable
fun SettingsScreen(
    appearanceMode: AppearanceMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    useBuiltinFileManager: Boolean,
    onUseBuiltinFileManagerChange: (Boolean) -> Unit,
    lastExportDirSummary: String?,
    onPickExportDir: () -> Unit,
    onOpenAbout: () -> Unit,
    onResetFirstSyncConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val pageBackdrop = rememberPageBackdrop()
    val tabVisible = LocalTabVisible.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        // 大标题收起进度：列表上滑约 360px 内从 0 过渡到 1。
        val headerCollapse = {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (lazyListState.firstVisibleItemScrollOffset / 360f).coerceIn(0f, 1f)
            }
        }
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .pageContentBackdrop(pageBackdrop),
            tabPadding = true,
            topPadding = { with(density) { headerHeightPx.toDp() } }
        ) {
            item {
                ConfigContainer(title = "外观", backgroundColor = AppColors.cardBackground) {
                    AppearanceModeRow(
                        selected = appearanceMode,
                        onSelect = onAppearanceModeChange
                    )
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
                        icon = painterResource(R.drawable.ic_square_dashed),
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
                            icon = painterResource(R.drawable.ic_square_dashed),
                            title = "重置首次同步确认",
                            onClick = onResetFirstSyncConfirm
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ConfigEntryItem(
                            color = AppColors.primary,
                            icon = painterResource(R.drawable.ic_square_dashed),
                            title = "关于",
                            onClick = onOpenAbout
                        )
                    }
                }
            }
            paddingItem(lazyListState)
        }

        GlasenseHeroHeader(
            title = "设置",
            subtitle = null,
            backdrop = pageBackdrop,
            liquidGlass = true && tabVisible,
            collapseProgress = headerCollapse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { headerHeightPx = it.height }
        )
    }
}

/** 主题模式选择行：标题 + 说明在上，三段式分段选择器独占一行，指示器滑动切换。 */
@Composable
private fun AppearanceModeRow(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.segmentedControlBackground, AppSpecs.buttonShape)
                .padding(3.dp)
        ) {
            val options = AppearanceMode.entries
            val itemWidth = maxWidth / options.size
            val selectedIndex = selected.ordinal
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
                label = "SegmentIndicator"
            )
            // 滑动指示器。
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(30.dp)
                    .background(AppColors.segmentedControlIndicator, AppSpecs.buttonShape)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                options.forEach { mode ->
                    val isSelected = mode == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            style = GlasenseTheme.type.footnote,
                            color = if (isSelected) {
                                AppColors.onSegmentedControlIndicator
                            } else {
                                AppColors.onSegmentedControlBackground
                            }
                        )
                    }
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
