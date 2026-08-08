package com.whyy.snapnotes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.whyy.snapnotes.data.STORE_PACKS
import com.whyy.snapnotes.data.StorePack
import com.whyy.snapnotes.data.StoreSubject
import com.whyy.snapnotes.ui.components.MoreMenu
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 小抄商店页面。
 *
 * 以两列网格展示知识点包，点击包卡片可查看详情并选择一键导入全部或部分科目。
 * 官方包与未来用户上传包统一展示，不做特殊对待。
 */
@Composable
fun StoreScreen(
    onCreateFolder: (String) -> Unit = {},
    onImportSubject: (StoreSubject) -> Unit = {},
    onImportSubjects: (List<StoreSubject>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var showFolderDialog by remember { mutableStateOf(false) }
    var detailPack by remember { mutableStateOf<StorePack?>(null) }

    if (showFolderDialog) {
        com.whyy.snapnotes.ui.components.FolderCreationDialog(
            show = true,
            onConfirm = { name ->
                showFolderDialog = false
                onCreateFolder(name)
            },
            onDismiss = { showFolderDialog = false }
        )
    }

    detailPack?.let { pack ->
        StorePackDetailDialog(
            pack = pack,
            onDismiss = { detailPack = null },
            onImportAll = {
                detailPack = null
                onImportSubjects(pack.subjects)
            },
            onImportSelected = { selected ->
                detailPack = null
                onImportSubjects(selected)
            },
            onImportSingle = { subject ->
                detailPack = null
                onImportSubject(subject)
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = "小抄商店",
                largeTitle = "小抄商店",
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
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 40.dp
            )
        ) {
            // 商店介绍卡
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Info,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "获取知识点，轻松学习",
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "点击知识点包查看详情，支持一键导入或选择部分科目导入。",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            // 全部知识点包标题
            item {
                SmallTitle(
                    text = "全部知识点包",
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                )
            }

            // 两列网格展示知识点包
            items(STORE_PACKS.chunked(2)) { rowPacks ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowPacks.forEach { pack ->
                        StorePackCard(
                            pack = pack,
                            onClick = { detailPack = pack },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowPacks.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // 未来功能预告
            item {
                SmallTitle(
                    text = "敬请期待",
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "用户上传考点",
                            style = MiuixTheme.textStyles.title4,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = "未来将支持用户自行上传考点知识点到服务器，供所有人免费下载。",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 知识点包卡片（网格单元）。
 */
@Composable
private fun StorePackCard(
    pack: StorePack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pack.name,
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
            }

            // 免费徽章
            if (pack.isFree) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MiuixTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "免费",
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "${pack.subjects.size} 科目 · ${pack.totalEntries} 条",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            // 科目标签（取前 3 个）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pack.subjects.take(3).forEach { subject ->
                    SubjectTag(text = subject.name)
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = "开发者：${pack.author}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

/**
 * 知识点包详情对话框。
 * 支持一键导入全部科目，或勾选部分科目导入。
 */
@Composable
private fun StorePackDetailDialog(
    pack: StorePack,
    onDismiss: () -> Unit,
    onImportAll: () -> Unit,
    onImportSelected: (List<StoreSubject>) -> Unit,
    onImportSingle: (StoreSubject) -> Unit
) {
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
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
                        text = pack.name,
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

                Spacer(Modifier.height(4.dp))

                Text(
                    text = pack.description,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${pack.subjects.size} 科目",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${pack.totalEntries} 条知识点",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (pack.isFree) {
                        Text(
                            text = "免费",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 一键导入全部
                Button(
                    onClick = onImportAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Icon(
                        imageVector = MiuixIcons.Download,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "一键导入全部 ${pack.totalEntries} 条",
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "或选择科目导入",
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                // 科目列表（可滚动）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    pack.subjects.forEach { subject ->
                        val isSelected = selected[subject.name] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isSelected) {
                                        selected.remove(subject.name)
                                    } else {
                                        selected[subject.name] = true
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 选择圆圈
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MiuixTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = MiuixIcons.Ok,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subject.name,
                                    style = MiuixTheme.textStyles.body2,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${subject.entries.size} 条知识点",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }

                            // 单科导入按钮
                            Icon(
                                imageVector = MiuixIcons.Download,
                                contentDescription = "导入此科目",
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onImportSingle(subject) }
                            )
                        }
                    }
                }

                // 导入选中按钮
                val selectedCount = selected.count { it.value }
                if (selectedCount > 0) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val picked = pack.subjects.filter { selected[it.name] == true }
                            onImportSelected(picked)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            text = "导入选中 $selectedCount 个科目",
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectTag(text: String) {
    Card(
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer
        )
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
