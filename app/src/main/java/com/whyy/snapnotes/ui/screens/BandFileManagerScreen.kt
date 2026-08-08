package com.whyy.snapnotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whyy.snapnotes.ui.viewmodel.BandTreeState
import com.whyy.snapnotes.ui.viewmodel.BandTreeNode
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
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
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 扁平化后的树节点，带层级深度用于缩进展示。 */
private data class FlatTreeNode(
    val id: String,
    val name: String,
    val type: String,
    val depth: Int
)

/** 递归把 BandTreeNode 树展开为扁平列表，folder 节点的 children 跟在后面并加深一层。 */
private fun flattenTree(nodes: List<BandTreeNode>, depth: Int = 0): List<FlatTreeNode> {
    val result = mutableListOf<FlatTreeNode>()
    for (node in nodes) {
        result.add(FlatTreeNode(node.id, node.name, node.type, depth))
        if (node.type == "folder" && node.children.isNotEmpty()) {
            result.addAll(flattenTree(node.children, depth + 1))
        }
    }
    return result
}

/**
 * 手环端文件树管理页：展示从手环同步来的文件夹/文件结构，支持新建文件夹、重命名、删除。
 *
 * - 数据来源：ViewModel 的 [BandTreeState]，由 "tree" tag 监听器解析手环推送的 treeData。
 * - 新建/重命名/删除操作通过 ViewModel 发 BLE 消息给手环，手环回包后自动刷新树。
 *
 * @param state 手环文件树状态。
 * @param onCreateFolder 新建文件夹回调（传入文件夹名）。
 * @param onRenameNode 重命名回调（传入节点 id 和新名称）。
 * @param onDeleteNode 删除回调（传入节点 id）。
 * @param onRefresh 手动刷新树。
 * @param onBackClick 返回上一页。
 */
@Composable
fun BandFileManagerScreen(
    state: BandTreeState,
    onCreateFolder: (name: String) -> Unit,
    onRenameNode: (nodeId: String, newName: String) -> Unit,
    onDeleteNode: (nodeId: String) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    // 对话框状态
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FlatTreeNode?>(null) }
    var deleteTarget by remember { mutableStateOf<FlatTreeNode?>(null) }

    val flatNodes = remember(state.tree) { flattenTree(state.tree) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = "手环文件管理",
                largeTitle = "手环文件管理",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(imageVector = MiuixIcons.Refresh, contentDescription = "刷新")
                    }
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(imageVector = MiuixIcons.Add, contentDescription = "新建文件夹")
                    }
                }
            )
        },
        popupHost = {}
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp
            )
        ) {
            when {
                state.isLoading -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "正在同步手环文件树...",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        }
                    }
                }

                state.error != null -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "同步失败",
                                style = MiuixTheme.textStyles.title3,
                                color = MiuixTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error!!,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                flatNodes.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Folder,
                                contentDescription = "Empty",
                                modifier = Modifier.size(64.dp),
                                tint = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "手环上还没有文件",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击右上角 + 新建文件夹",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        }
                    }
                }

                else -> {
                    item { SmallTitle(text = "文件列表") }
                    item {
                        Column {
                            flatNodes.forEachIndexed { index, node ->
                                val isFirst = index == 0
                                val isLast = index == flatNodes.lastIndex
                                val shape = when {
                                    flatNodes.size == 1 -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                    isFirst -> androidx.compose.foundation.shape.RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp
                                    )
                                    isLast -> androidx.compose.foundation.shape.RoundedCornerShape(
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                    else -> androidx.compose.ui.graphics.RectangleShape
                                }

                                Card(
                                    modifier = Modifier.clip(shape),
                                    cornerRadius = 0.dp
                                ) {
                                    BandFileItemRow(
                                        node = node,
                                        onRename = { renameTarget = node },
                                        onDelete = { deleteTarget = node }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 新建文件夹对话框
    BandTextInputDialog(
        show = showCreateDialog,
        title = "新建文件夹",
        summary = "在手环根目录下创建新文件夹",
        label = "文件夹名称",
        initial = "",
        onDismiss = { showCreateDialog = false },
        onConfirm = { name ->
            showCreateDialog = false
            onCreateFolder(name)
        }
    )

    // 重命名对话框
    val renaming = renameTarget
    BandTextInputDialog(
        show = renaming != null,
        title = "重命名",
        summary = "修改「${renaming?.name ?: ""}」的名称",
        label = "新名称",
        initial = renaming?.name ?: "",
        onDismiss = { renameTarget = null },
        onConfirm = { newName ->
            val target = renameTarget
            renameTarget = null
            if (target != null) {
                onRenameNode(target.id, newName)
            }
        }
    )

    // 删除确认对话框
    val deleting = deleteTarget
    OverlayDialog(
        title = "删除「${deleting?.name ?: ""}」？",
        summary = if (deleting != null) {
            if (deleting.type == "folder") {
                "删除文件夹将同时删除其中的所有子文件和子文件夹，此操作不可撤销。"
            } else {
                "删除后无法恢复，此操作不可撤销。"
            }
        } else "",
        show = deleting != null,
        onDismissRequest = { deleteTarget = null },
        renderInRootScaffold = false
    ) {
        if (deleting != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "取消",
                    onClick = { deleteTarget = null },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "删除",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val target = deleteTarget
                        deleteTarget = null
                        if (target != null) {
                            onDeleteNode(target.id)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 单个文件/文件夹行：图标 + 名称 + 更多操作下拉菜单。 */
@Composable
private fun BandFileItemRow(
    node: FlatTreeNode,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    BasicComponent(
        title = node.name,
        summary = if (node.type == "folder") "文件夹" else "文件",
        modifier = modifier,
        startAction = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                // 层级缩进
                Spacer(modifier = Modifier.width((node.depth * 16).dp))
                Icon(
                    imageVector = if (node.type == "folder") MiuixIcons.Folder else MiuixIcons.File,
                    contentDescription = if (node.type == "folder") "Folder" else "File",
                    tint = if (node.type == "folder") {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    }
                )
            }
        },
        endActions = {
            Box {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = "更多操作",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(2.dp)
                        .clickable { menuExpanded = true }
                )
                OverlayListPopup(
                    show = menuExpanded,
                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                    alignment = PopupPositionProvider.Align.End,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    ListPopupColumn {
                        DropdownImpl(
                            item = DropdownItem(
                                text = "重命名",
                                icon = { m ->
                                    Icon(
                                        imageVector = MiuixIcons.Edit,
                                        contentDescription = null,
                                        modifier = m
                                    )
                                }
                            ),
                            optionSize = 2,
                            isSelected = false,
                            index = 0,
                            onSelectedIndexChange = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownImpl(
                            item = DropdownItem(
                                text = "删除",
                                icon = { m ->
                                    Icon(
                                        imageVector = MiuixIcons.Delete,
                                        contentDescription = null,
                                        modifier = m
                                    )
                                }
                            ),
                            optionSize = 2,
                            isSelected = false,
                            index = 1,
                            onSelectedIndexChange = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    )
}

/**
 * 文本输入对话框：复用于「新建文件夹」和「重命名」。
 * 使用 OverlayDialog 并利用其 show 参数控制显隐动画。
 */
@Composable
private fun BandTextInputDialog(
    show: Boolean,
    title: String,
    summary: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(show) { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(show, initial) {
        if (show) {
            text = initial
            delay(80)
            runCatching { focusRequester.requestFocus() }
        }
    }

    OverlayDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = onDismiss,
        renderInRootScaffold = false
    ) {
        Column {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
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
                    onClick = {
                        val clean = text.trim()
                        if (clean.isNotEmpty()) {
                            onConfirm(clean)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
