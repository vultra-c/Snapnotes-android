package com.whyy.snapnotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whyy.snapnotes.ui.viewmodel.BandFileNode
import com.whyy.snapnotes.ui.viewmodel.BandTreeUiState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.whyy.snapnotes.ui.liquid.LiquidGlassCard
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 手环端文件树管理页面。
 *
 * 功能：
 * - 以文件树形式浏览手环端的所有文件和文件夹
 * - 支持创建文件夹（在任意层级）
 * - 支持删除节点（文件或文件夹，文件夹递归删除）
 * - 支持重命名节点
 * - 支持选择文件夹作为导入考点目标目录（[pickMode] 下每行展示导入按钮）
 * - 长按节点弹出上下文菜单（新建子文件夹 / 导入考点到此文件夹 / 重命名 / 删除）
 * - Loading / Ready / Error 状态间使用滑入+淡入动画过渡
 *
 * @param state 树数据状态
 * @param onBackClick 返回
 * @param onRefresh 刷新文件树
 * @param onCreateFolder 创建文件夹
 * @param onDeleteNode 删除节点
 * @param onRenameNode 重命名节点
 * @param onImportToFolder 选择此文件夹导入考点（选择模式下可用）
 * @param onImportToFolderClick 非选择模式下，从上下文菜单「导入考点到此文件夹」触发的新回调
 * @param pickMode 是否为「选择文件夹导入」模式
 */
@Composable
fun BandFileTreeScreen(
    state: BandTreeUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onCreateFolder: (name: String, parentId: String) -> Unit,
    onDeleteNode: (nodeId: String) -> Unit,
    onRenameNode: (nodeId: String, newName: String) -> Unit,
    onImportToFolder: ((folderId: String, folderName: String) -> Unit)? = null,
    onImportToFolderClick: ((folderId: String, folderName: String) -> Unit)? = null,
    pickMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    // 对话框状态
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var createFolderParentId by remember { mutableStateOf("bt_root") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetId by remember { mutableStateOf("") }
    var renameTargetName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf("") }
    var deleteTargetName by remember { mutableStateOf("") }
    var deleteTargetIsFolder by remember { mutableStateOf(false) }

    // 展开状态记忆（用节点 ID 记录是否展开）
    val expandedFolders = remember { mutableStateOf(setOf("bt_root")) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = if (pickMode) "选择导入目录" else "手环文件管理",
                largeTitle = if (pickMode) "选择导入目录" else "手环文件管理",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.padding(end = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        popupHost = {}
    ) { paddingValues ->
        // Loading -> Ready -> Error 之间使用滑入 + 淡入的平滑过渡
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (slideInVertically(tween(300)) { it / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutVertically(tween(300)) { -it / 4 } + fadeOut(tween(300)))
            },
            contentAlignment = Alignment.TopStart,
            label = "BandTreeState",
            modifier = Modifier.fillMaxSize()
        ) { currentState ->
            when (currentState) {
                is BandTreeUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "正在加载手环文件...",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                is BandTreeUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Info,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onError,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = currentState.message,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onError,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(
                            text = "重试",
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = onRefresh
                        )
                    }
                }

                is BandTreeUiState.Ready -> {
                    val (folderCount, fileCount) = remember(currentState.tree) {
                        countTree(currentState.tree)
                    }
                    val headerText = if (folderCount == 0 && fileCount == 0) {
                        "手环存储"
                    } else {
                        "手环存储 · $folderCount 文件夹 / $fileCount 文件"
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .scrollEndHaptic(),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = 24.dp
                        )
                    ) {
                        // 选择模式下顶部的导入提示横幅
                        if (pickMode && onImportToFolder != null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .background(
                                            MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "请选择要导入的文件夹",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        item {
                            SmallTitle(
                                text = headerText,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        if (currentState.tree.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    onCreate = {
                                        createFolderParentId = "bt_root"
                                        showCreateFolderDialog = true
                                    }
                                )
                            }
                        } else {
                            items(currentState.tree, key = { it.id }) { node ->
                                TreeItemView(
                                    node = node,
                                    depth = 0,
                                    expandedFolders = expandedFolders.value,
                                    onToggleExpand = { nodeId ->
                                        expandedFolders.value = if (nodeId in expandedFolders.value) {
                                            expandedFolders.value - nodeId
                                        } else {
                                            expandedFolders.value + nodeId
                                        }
                                    },
                                    onCreateFolder = { parentId ->
                                        createFolderParentId = parentId
                                        showCreateFolderDialog = true
                                    },
                                    onDeleteNode = { nodeId, name, isFolder ->
                                        deleteTargetId = nodeId
                                        deleteTargetName = name
                                        deleteTargetIsFolder = isFolder
                                        showDeleteConfirm = true
                                    },
                                    onRenameNode = { nodeId, currentName ->
                                        renameTargetId = nodeId
                                        renameTargetName = currentName
                                        showRenameDialog = true
                                    },
                                    onImportToFolder = onImportToFolder,
                                    onImportToFolderClick = onImportToFolderClick,
                                    pickMode = pickMode
                                )
                            }

                            // 根级「新建文件夹」主操作按钮
                            item {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        createFolderParentId = "bt_root"
                                        showCreateFolderDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .pressable(interactionSource = null, indication = SinkFeedback()),
                                    colors = ButtonDefaults.buttonColorsPrimary()
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "新建文件夹",
                                        color = MiuixTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 创建文件夹对话框
    if (showCreateFolderDialog) {
        BandTextInputDialog(
            show = true,
            title = "新建文件夹",
            label = "文件夹名称",
            hint = "请输入文件夹名称",
            initial = "",
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                showCreateFolderDialog = false
                onCreateFolder(name, createFolderParentId)
            }
        )
    }

    // 重命名对话框
    if (showRenameDialog) {
        BandTextInputDialog(
            show = true,
            title = "重命名",
            label = "新名称",
            hint = "请输入新名称",
            initial = renameTargetName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                showRenameDialog = false
                onRenameNode(renameTargetId, name)
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        OverlayDialog(
            title = "确认删除",
            summary = "「${deleteTargetName}」将被永久删除" +
                if (deleteTargetIsFolder) "，文件夹内所有内容也将一并删除" else "",
            show = true,
            onDismissRequest = { showDeleteConfirm = false },
            renderInRootScaffold = false
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "取消",
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "删除",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNode(deleteTargetId)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 递归渲染文件树节点。
 *
 * - 文件夹使用 [MiuixIcons.Folder] 图标，展开时轻微放大；右侧展开箭头旋转指示状态。
 * - 文件使用 [MiuixIcons.File] 图标。
 * - 非选择模式下长按节点弹出上下文菜单（新建子文件夹 / 导入考点到此文件夹 / 重命名 / 删除）。
 * - 选择模式下每个文件夹行展示「导入到此文件夹」按钮。
 */
@Composable
private fun TreeItemView(
    node: BandFileNode,
    depth: Int,
    expandedFolders: Set<String>,
    onToggleExpand: (String) -> Unit,
    onCreateFolder: (parentId: String) -> Unit,
    onDeleteNode: (nodeId: String, name: String, isFolder: Boolean) -> Unit,
    onRenameNode: (nodeId: String, currentName: String) -> Unit,
    onImportToFolder: ((folderId: String, folderName: String) -> Unit)?,
    onImportToFolderClick: ((folderId: String, folderName: String) -> Unit)?,
    pickMode: Boolean
) {
    val isExpanded = node.id in expandedFolders
    var showContextMenu by remember { mutableStateOf(false) }

    // 文件夹图标展开时的放大动画
    val folderScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "folderScale"
    )
    // 展开/收起箭头旋转：收起指向右（▶），展开指向下（▼）
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 270f else 180f,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "chevronRotate"
    )

    // 上下文菜单项（非选择模式）
    val actions = buildList {
        if (node.isFolder) {
            add(NodeMenuAction("新建子文件夹", MiuixIcons.Add) { onCreateFolder(node.id) })
            if (onImportToFolderClick != null) {
                add(
                    NodeMenuAction("导入考点到此文件夹", MiuixIcons.Send) {
                        onImportToFolderClick(node.id, node.name)
                    }
                )
            }
        }
        add(NodeMenuAction("重命名", MiuixIcons.Edit) { onRenameNode(node.id, node.name) })
        add(NodeMenuAction("删除", MiuixIcons.Delete) { onDeleteNode(node.id, node.name, node.isFolder) })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Box {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(start = (depth * 16).dp)
                    .combinedClickable(
                        onClick = {
                            if (node.isFolder) onToggleExpand(node.id)
                        },
                        onLongClick = {
                            if (!pickMode) showContextMenu = true
                        }
                    ),
                onClick = null,
                containerColor = MiuixTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 类型图标
                    if (node.isFolder) {
                        Icon(
                            imageVector = MiuixIcons.Folder,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(folderScale)
                        )
                    } else {
                        Icon(
                            imageVector = MiuixIcons.File,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // 名称 + 副标题
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.name,
                            style = MiuixTheme.textStyles.title4,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (node.isFolder && node.children.isNotEmpty()) {
                            Text(
                                text = "${node.children.size} 项",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        } else if (node.isContent) {
                            Text(
                                text = "文件",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                    // 展开/收起箭头（文件夹、且非选择模式）
                    if (node.isFolder && !(pickMode && onImportToFolder != null)) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    // 选择模式下的导入按钮
                    if (pickMode && node.isFolder && onImportToFolder != null) {
                        IconButton(
                            onClick = { onImportToFolder(node.id, node.name) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Send,
                                contentDescription = "导入到此文件夹",
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 长按上下文菜单（非选择模式）
            if (!pickMode) {
                OverlayListPopup(
                    show = showContextMenu,
                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                    alignment = PopupPositionProvider.Align.End,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    ListPopupColumn {
                        actions.forEachIndexed { index, action ->
                            DropdownImpl(
                                item = DropdownItem(
                                    text = action.text,
                                    icon = { m ->
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = null,
                                            modifier = m
                                        )
                                    }
                                ),
                                optionSize = actions.size,
                                isSelected = false,
                                index = index,
                                onSelectedIndexChange = {
                                    showContextMenu = false
                                    action.onClick()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 子节点（带展开/收起动画）
        AnimatedVisibility(
            visible = isExpanded && node.isFolder,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column {
                if (node.children.isEmpty()) {
                    Text(
                        text = "（空文件夹）",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = (depth * 16 + 52).dp, top = 8.dp, bottom = 8.dp)
                    )
                } else {
                    node.children.forEach { child ->
                        TreeItemView(
                            node = child,
                            depth = depth + 1,
                            expandedFolders = expandedFolders,
                            onToggleExpand = onToggleExpand,
                            onCreateFolder = onCreateFolder,
                            onDeleteNode = onDeleteNode,
                            onRenameNode = onRenameNode,
                            onImportToFolder = onImportToFolder,
                            onImportToFolderClick = onImportToFolderClick,
                            pickMode = pickMode
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空状态：树为空时展示图标与「新建文件夹」引导。
 */
@Composable
private fun EmptyStateCard(onCreate: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier.padding(horizontal = 12.dp),
        containerColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = MiuixIcons.Folder,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "手环上暂无文件",
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "新建一个文件夹开始整理，推送的知识点也会显示在这里",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "新建文件夹", color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}

/**
 * 文本输入对话框（复用于创建文件夹/重命名）。
 */
@Composable
private fun BandTextInputDialog(
    show: Boolean,
    title: String,
    label: String,
    hint: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    androidx.compose.runtime.LaunchedEffect(show, initial) {
        if (show) text = initial
    }
    OverlayDialog(
        title = title,
        summary = hint,
        show = show,
        onDismissRequest = onDismiss,
        renderInRootScaffold = false
    ) {
        Column {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = label
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "确定",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { onConfirm(text.trim()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 上下文菜单项。 */
private data class NodeMenuAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/** 递归统计树中的文件夹与文件数量。 */
private fun countTree(nodes: List<BandFileNode>): Pair<Int, Int> {
    var folders = 0
    var files = 0
    fun walk(list: List<BandFileNode>) {
        for (n in list) {
            if (n.isFolder) {
                folders++
                walk(n.children)
            } else {
                files++
            }
        }
    }
    walk(nodes)
    return folders to files
}
