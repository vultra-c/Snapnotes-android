package com.whyy.snapnotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.ui.LocalTabVisible
import com.whyy.snapnotes.ui.pageContentBackdrop
import com.whyy.snapnotes.ui.rememberPageBackdrop
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroHeader
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroIconButton
import com.whyy.snapnotes.ui.components.glasense.GlasenseMenu
import com.whyy.snapnotes.ui.components.glasense.MenuDivider
import com.whyy.snapnotes.ui.components.glasense.MenuItemData
import com.whyy.snapnotes.ui.components.glasense.MenuState
import com.whyy.snapnotes.ui.components.glasense.GlasenseSurfaceCard
import com.whyy.snapnotes.ui.components.glasense.pressEffect
import com.whyy.snapnotes.ui.components.glasense.rememberPressInteractionSource
import com.whyy.snapnotes.ui.components.packed.PageContent
import com.whyy.snapnotes.ui.viewmodel.PushRecord
import com.whyy.snapnotes.ui.viewmodel.toReadableBytes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 历史记录时间过滤范围。 */
private enum class HistoryFilter(val label: String) {
    All("全部"),
    Today("今天"),
    Week("7 天"),
    Earlier("更早")
}

/**
 * 历史页：固定磨砂大标题 + 胶囊时间过滤 + 状态点记录卡（纯色 iOS 卡 + 按压反馈）+
 * 玻璃菜单（编辑/重新推送/删除）+ 多选批量删除。
 * 视觉对齐设计图 3（iOS 白底、胶囊过滤器、绿/蓝/灰状态点）。
 */
@Composable
fun HistoryScreen(
    records: List<PushRecord>,
    onRepush: (PushRecord) -> Unit,
    onDeleteRequest: (PushRecord) -> Unit,
    onBatchDeleteRequest: (List<PushRecord>) -> Unit,
    onEditRecord: (PushRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val pageBackdrop = rememberPageBackdrop()
    val tabVisible = LocalTabVisible.current
    val liquidGlass = true && tabVisible
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var filter by remember { mutableStateOf(HistoryFilter.All) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var menuTargetRecord by remember { mutableStateOf<PushRecord?>(null) }
    var menuAnchorBounds by remember { mutableStateOf(Rect.Zero) }

    val filteredRecords = remember(records, filter) { records.filter { matchesFilter(it, filter) } }
    val allSelected = filteredRecords.isNotEmpty() && filteredRecords.all { it.id in selectedIds }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .pageContentBackdrop(pageBackdrop),
            tabPadding = true,
            topPadding = { with(density) { headerHeightPx.toDp() } },
            bottomPadding = 120.dp
        ) {
            if (selectionMode && filteredRecords.isNotEmpty()) {
                item {
                    Text(
                        text = if (allSelected) "取消全选" else "全选",
                        style = GlasenseTheme.type.subHeadline,
                        color = GlasenseTheme.colors.primary,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedIds =
                                    if (allSelected) emptySet() else filteredRecords.map { it.id }.toSet()
                            }
                    )
                    VGap(8.dp)
                }
            }
            item {
                HistoryFilterBar(
                    selected = filter,
                    onSelect = { filter = it },
                    counts = remember(records) {
                        HistoryFilter.entries.associateWith { f -> records.count { matchesFilter(it, f) } }
                    }
                )
                VGap(16.dp)
            }
            if (filteredRecords.isEmpty()) {
                item {
                    HistoryEmptyCard(filter = filter)
                    VGap(12.dp)
                }
            } else {
                items(filteredRecords.size, key = { filteredRecords[it].id }) { index ->
                    val record = filteredRecords[index]
                    val isSelected = record.id in selectedIds
                    Box(modifier = Modifier.animateItem()) {
                        HistoryRecordCard(
                            record = record,
                            timeText = timeFmt.format(Date(record.pushedAt)),
                            isSelected = isSelected,
                            selectionMode = selectionMode,
                            onToggleSelect = {
                                selectedIds = if (isSelected) {
                                    selectedIds - record.id
                                } else {
                                    selectedIds + record.id
                                }
                            },
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = if (isSelected) {
                                        selectedIds - record.id
                                    } else {
                                        selectedIds + record.id
                                    }
                                } else {
                                    onEditRecord(record)
                                }
                            },
                            onLongClick = {
                                selectionMode = true
                                selectedIds = selectedIds + record.id
                            },
                            onMenuRequest = { anchorBounds ->
                                menuTargetRecord = record
                                menuAnchorBounds = anchorBounds
                            }
                        )
                    }
                    VGap(12.dp)
                }
            }
            paddingItem(lazyListState)
        }

        GlasenseHeroHeader(
            title = if (selectionMode) "已选 ${selectedIds.size} 条" else "历史",
            subtitle = if (selectionMode) null else "本机推送记录",
            backdrop = pageBackdrop,
            liquidGlass = liquidGlass,
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { headerHeightPx = it.height },
            trailing = {
                if (selectionMode) {
                    // 多选操作入口：占位图标，正式图标待替换。
                    GlasenseHeroIconButton(
                        painter = painterResource(R.drawable.ic_trash),
                        contentDescription = "删除所选",
                        backdrop = pageBackdrop,
                        liquidGlass = liquidGlass,
                        tint = if (selectedIds.isEmpty()) {
                            GlasenseTheme.colors.contentVariant
                        } else {
                            GlasenseTheme.colors.error
                        },
                        onClick = {
                            val selected = filteredRecords.filter { it.id in selectedIds }
                            if (selected.isNotEmpty()) onBatchDeleteRequest(selected)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    GlasenseHeroIconButton(
                        painter = painterResource(R.drawable.ic_cross),
                        contentDescription = "退出多选",
                        backdrop = pageBackdrop,
                        liquidGlass = liquidGlass,
                        tint = GlasenseTheme.colors.content,
                        onClick = { exitSelection() }
                    )
                } else {
                    // 多选入口：占位图标，正式图标待替换。
                    GlasenseHeroIconButton(
                        painter = painterResource(R.drawable.ic_square_dashed),
                        contentDescription = "多选",
                        backdrop = pageBackdrop,
                        liquidGlass = liquidGlass,
                        onClick = { selectionMode = true }
                    )
                }
            }
        )
    }

    val menuRecord = menuTargetRecord
    if (menuRecord != null) {
        // 菜单是 PageContent 的兄弟节点，采样 pageBackdrop 安全（无渲染树环）。
        GlasenseMenu(
            menuState = MenuState(
                isVisible = true,
                anchorBounds = menuAnchorBounds,
                items = listOf(
                    MenuItemData(
                        text = "编辑",
                        icon = painterResource(R.drawable.ic_square_and_pencil),
                        onClick = {
                            menuTargetRecord = null
                            onEditRecord(menuRecord)
                        }
                    ),
                    MenuItemData(
                        text = "重新推送",
                        icon = painterResource(R.drawable.ic_arrow_counterclockwise),
                        onClick = {
                            menuTargetRecord = null
                            onRepush(menuRecord)
                        }
                    ),
                    MenuDivider,
                    MenuItemData(
                        text = "删除",
                        icon = painterResource(R.drawable.ic_trash),
                        isDestructive = true,
                        onClick = {
                            menuTargetRecord = null
                            onDeleteRequest(menuRecord)
                        }
                    )
                )
            ),
            backdrop = pageBackdrop,
            onDismiss = { menuTargetRecord = null }
        )
    }
}

private fun matchesFilter(record: PushRecord, filter: HistoryFilter): Boolean {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    return when (filter) {
        HistoryFilter.All -> true
        HistoryFilter.Today -> record.pushedAt >= startOfToday
        HistoryFilter.Week -> record.pushedAt in weekAgo until startOfToday
        HistoryFilter.Earlier -> record.pushedAt < weekAgo
    }
}

/** 胶囊分段过滤器：全部 / 今天 / 7 天 / 更早，选中为浅蓝胶囊；等分宽度防换行。 */
@Composable
private fun HistoryFilterBar(
    selected: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit,
    counts: Map<HistoryFilter, Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            GlasenseTheme.colors.primary.copy(alpha = 0.14f)
                        } else {
                            GlasenseTheme.colors.scrimNormal
                        },
                        CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(option) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildString {
                        append(option.label)
                        counts[option]?.let { if (option != HistoryFilter.All) append(" $it") }
                    },
                    style = GlasenseTheme.type.subHeadline,
                    color = if (isSelected) {
                        GlasenseTheme.colors.primary
                    } else {
                        GlasenseTheme.colors.contentVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

/** 记录卡：状态点 + 文件名/摘要 + 更多菜单入口（纯色 iOS 卡 + 按压反馈）。 */
@Composable
private fun HistoryRecordCard(
    record: PushRecord,
    timeText: String,
    isSelected: Boolean,
    selectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuRequest: (Rect) -> Unit
) {
    val statusColor = recordStatusColor(record)
    val subjects = record.subjects.joinToString("、")
        .let { if (it.length > 24) it.take(24) + "…" else it }
    var menuBounds by remember { mutableStateOf(Rect.Zero) }
    val interaction = rememberPressInteractionSource()

    GlasenseSurfaceCard(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) {
            GlasenseTheme.colors.primary.copy(alpha = 0.10f)
        } else {
            GlasenseTheme.colors.cardBackground
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .pressEffect(interaction)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                GlasenseTheme.colors.primary
                            } else {
                                GlasenseTheme.colors.contentVariant.copy(alpha = 0.3f)
                            },
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleSelect
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkmark),
                            contentDescription = null,
                            tint = GlasenseTheme.colors.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            } else {
                // 状态点：今天=绿，7 天=蓝，更早=灰。
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }
            FileIconBadge()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = GlasenseTheme.type.bodyEmphasized,
                    color = GlasenseTheme.colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (subjects.isNotBlank()) append("科目：$subjects")
                        else append("科目：未知")
                        append("\n${record.fileSize.toReadableBytes()} · $timeText")
                    },
                    style = GlasenseTheme.type.footnote,
                    color = GlasenseTheme.colors.contentVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .onGloballyPositioned { menuBounds = it.boundsInWindow() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onMenuRequest(menuBounds) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ellipsis),
                    contentDescription = "更多操作",
                    tint = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** 记录状态点颜色：今天绿、7 天内蓝、更早灰。 */
@Composable
private fun recordStatusColor(record: PushRecord): Color {
    val startOfToday = remember(record.pushedAt) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val weekAgo = remember(record.pushedAt) {
        System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    }
    return when {
        record.pushedAt >= startOfToday -> Color(0xFF34C759)
        record.pushedAt >= weekAgo -> GlasenseTheme.colors.primary
        else -> GlasenseTheme.colors.contentVariant
    }
}

/** 浅蓝圆角方块内的文件图标徽章。 */
@Composable
private fun FileIconBadge() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(GlasenseTheme.colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_document),
            contentDescription = null,
            tint = GlasenseTheme.colors.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** 空状态：居中图标 + 说明。 */
@Composable
private fun HistoryEmptyCard(filter: HistoryFilter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GlasenseTheme.colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_time_line),
                contentDescription = null,
                tint = GlasenseTheme.colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = when (filter) {
                HistoryFilter.All -> "还没有推送过的文件"
                else -> "该时间段没有推送记录"
            },
            style = GlasenseTheme.type.headline,
            color = GlasenseTheme.colors.content,
            textAlign = TextAlign.Center
        )
        Text(
            text = "推送成功的文件会在这里留一份，便于重新编辑或推送",
            style = GlasenseTheme.type.footnote,
            color = GlasenseTheme.colors.contentVariant,
            textAlign = TextAlign.Center
        )
    }
}
