package com.whyy.snapnotes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.whyy.snapnotes.ui.viewmodel.BandFileNode
import com.whyy.snapnotes.ui.viewmodel.BandTreeUiState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 推送目标文件夹选择对话框。
 *
 * 以手环端文件树形式展示可选目标目录，用户可选择「主页（根目录）」或任意子文件夹作为
 * JSON 推送目标。[defaultFolderId] 为 null 时默认选中根目录（主页），否则默认选中最近
 * 新建的文件夹并自动展开其所在路径使其可见。
 *
 * 状态：
 * - [BandTreeUiState.Loading] 显示加载指示器
 * - [BandTreeUiState.Error] 显示错误信息
 * - [BandTreeUiState.Ready] 渲染可展开的文件夹树
 *
 * 交互：
 * - 点击行任意位置选中该文件夹（单选）
 * - 点击行首箭头展开/收起子文件夹（箭头自身消费点击，不会触发选中）
 * - 选中行以主题色高亮，并展示单选指示器
 *
 * @param show 是否显示对话框
 * @param treeState 手环文件树 UI 状态
 * @param defaultFolderId 默认选中的文件夹 id，null 表示根目录（主页）
 * @param onConfirm 确认回调，返回选中的文件夹 id（null = 主页/根目录）
 * @param onDismiss 关闭回调
 * @param modifier 修饰符
 */
@Composable
fun FolderSelectionDialog(
    show: Boolean,
    treeState: BandTreeUiState,
    defaultFolderId: String?,
    onConfirm: (folderId: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    // 选中的文件夹 id：null 表示根目录（主页）
    var selectedFolderId by remember { mutableStateOf<String?>(defaultFolderId) }
    // 展开状态：用文件夹 id 记录，FolderSelectionRootId 代表根目录「主页」
    var expandedFolders by remember { mutableStateOf(setOf(FolderSelectionRootId)) }

    // 树加载完成时设置默认选中，并自动展开默认文件夹所在路径
    LaunchedEffect(treeState, defaultFolderId) {
        val tree = (treeState as? BandTreeUiState.Ready)?.tree
        if (tree != null) {
            selectedFolderId = defaultFolderId
            expandedFolders = if (defaultFolderId != null) {
                setOf(FolderSelectionRootId) + ancestorFolderIds(tree, defaultFolderId)
            } else {
                setOf(FolderSelectionRootId)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择推送目标文件夹",
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = MiuixIcons.Close,
                            contentDescription = "关闭",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 提示文案
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "第一次默认推送到主页，新建文件夹后默认推送到最新文件夹",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 可滚动的树内容区（树过大时纵向滚动）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 根目录「主页」行
                    SelectionRow(
                        name = "主页 (根目录)",
                        icon = MiuixIcons.Home,
                        iconTint = MiuixTheme.colorScheme.primary,
                        depth = 0,
                        isSelected = selectedFolderId == null,
                        isExpanded = FolderSelectionRootId in expandedFolders,
                        expandable = true,
                        onSelect = { selectedFolderId = null },
                        onToggleExpand = {
                            expandedFolders = if (FolderSelectionRootId in expandedFolders) {
                                expandedFolders - FolderSelectionRootId
                            } else {
                                expandedFolders + FolderSelectionRootId
                            }
                        }
                    )

                    // 根目录展开后展示文件树内容（加载中 / 错误 / 文件夹树）
                    AnimatedVisibility(
                        visible = FolderSelectionRootId in expandedFolders,
                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        when (treeState) {
                            is BandTreeUiState.Loading -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "正在加载手环文件...",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                            }

                            is BandTreeUiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onError,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = treeState.message,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onError,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is BandTreeUiState.Ready -> {
                                if (treeState.tree.isEmpty()) {
                                    Text(
                                        text = "暂无文件夹，将推送到主页",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            top = 8.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                } else {
                                    treeState.tree.forEach { node ->
                                        if (node.isFolder) {
                                            FolderNodeRow(
                                                node = node,
                                                depth = 1,
                                                selectedId = selectedFolderId,
                                                expandedFolders = expandedFolders,
                                                onSelect = { selectedFolderId = it },
                                                onToggleExpand = { id ->
                                                    expandedFolders = if (id in expandedFolders) {
                                                        expandedFolders - id
                                                    } else {
                                                        expandedFolders + id
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onConfirm(selectedFolderId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            text = "确认推送",
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个可选行：展开箭头 + 类型图标 + 名称 + 单选指示器。
 *
 * - 点击行任意位置选中该文件夹（单选）
 * - 点击行首箭头切换子级可见性（[IconButton] 自身消费点击，不会触发整行选中）
 * - 选中行以主题色半透明背景高亮，名称与指示器同步着色
 *
 * @param name 显示名称
 * @param icon 类型图标
 * @param iconTint 图标着色
 * @param depth 缩进层级（每层 16dp）
 * @param isSelected 是否选中
 * @param isExpanded 是否展开
 * @param expandable 是否可展开（有子文件夹时为 true）
 * @param onSelect 选中回调
 * @param onToggleExpand 展开/收起回调
 */
@Composable
private fun SelectionRow(
    name: String,
    icon: ImageVector,
    iconTint: Color,
    depth: Int,
    isSelected: Boolean,
    isExpanded: Boolean,
    expandable: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit
) {
    // 展开/收起箭头旋转：收起指向右（▶），展开指向下（▼）
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 270f else 180f,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "chevronRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 展开/收起箭头
        if (expandable) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(chevronRotation)
                )
            }
        } else {
            Spacer(Modifier.size(28.dp))
        }

        Spacer(Modifier.width(4.dp))

        // 类型图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(10.dp))

        // 名称
        Text(
            text = name,
            style = MiuixTheme.textStyles.title4,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        // 单选指示器
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * 递归渲染文件夹节点行：选中态高亮 + 子文件夹展开/收起动画。
 *
 * 仅渲染文件夹节点（content 类型不参与目标选择）。文件夹无子文件夹时不显示展开箭头。
 *
 * @param node 当前文件夹节点
 * @param depth 缩进层级
 * @param selectedId 当前选中的文件夹 id（null 表示根目录）
 * @param expandedFolders 已展开的文件夹 id 集合
 * @param onSelect 选中回调
 * @param onToggleExpand 展开/收起回调
 */
@Composable
private fun FolderNodeRow(
    node: BandFileNode,
    depth: Int,
    selectedId: String?,
    expandedFolders: Set<String>,
    onSelect: (String) -> Unit,
    onToggleExpand: (String) -> Unit
) {
    val isSelected = node.id == selectedId
    val isExpanded = node.id in expandedFolders
    val subFolders = remember(node) { node.children.filter { it.isFolder } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        SelectionRow(
            name = node.name,
            icon = MiuixIcons.Folder,
            iconTint = MiuixTheme.colorScheme.primary,
            depth = depth,
            isSelected = isSelected,
            isExpanded = isExpanded,
            expandable = subFolders.isNotEmpty(),
            onSelect = { onSelect(node.id) },
            onToggleExpand = { onToggleExpand(node.id) }
        )

        // 子文件夹（带展开/收起动画）
        AnimatedVisibility(
            visible = isExpanded && subFolders.isNotEmpty(),
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column {
                subFolders.forEach { child ->
                    FolderNodeRow(
                        node = child,
                        depth = depth + 1,
                        selectedId = selectedId,
                        expandedFolders = expandedFolders,
                        onSelect = onSelect,
                        onToggleExpand = onToggleExpand
                    )
                }
            }
        }
    }
}

/** 根目录「主页」在展开状态集合中使用的占位 id（不参与推送，仅用于展开/收起记录）。 */
private const val FolderSelectionRootId = "bt_root"

/**
 * 查找 [targetId] 在树中的所有祖先文件夹 id（不含 [targetId] 本身），
 * 用于在默认选中某子文件夹时自动展开其所在路径，使其可见。
 */
private fun ancestorFolderIds(nodes: List<BandFileNode>, targetId: String): List<String> {
    fun walk(list: List<BandFileNode>, path: List<String>): List<String>? {
        for (n in list) {
            if (n.id == targetId) return path
            if (n.isFolder) {
                walk(n.children, path + n.id)?.let { return it }
            }
        }
        return null
    }
    return walk(nodes, emptyList()) ?: emptyList()
}
