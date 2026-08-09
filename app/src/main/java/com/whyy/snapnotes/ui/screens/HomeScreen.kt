package com.whyy.snapnotes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whyy.snapnotes.logic.BandStorageInfoData
import com.whyy.snapnotes.ui.components.AiPromptCard
import com.whyy.snapnotes.ui.components.FormulaTutorial
import com.whyy.snapnotes.ui.components.FolderCreationDialog
import com.whyy.snapnotes.ui.components.JsonFileTutorial
import com.whyy.snapnotes.ui.components.MoreMenu
import com.whyy.snapnotes.ui.components.StorageRingCard
import com.whyy.snapnotes.ui.viewmodel.ConnectionState
import androidx.compose.foundation.basicMarquee
import com.whyy.snapnotes.ui.viewmodel.SelectedFileState
import com.whyy.snapnotes.ui.viewmodel.toReadableBytes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.IntrinsicSize

private enum class ConnectionStage {
    Idle,
    Connecting,
    Connected,
    Error
}

private fun ConnectionState.stage(): ConnectionStage {
    return when {
        isConnected -> ConnectionStage.Connected
        statusText.contains("连接中") || statusText.contains("授权中") || statusText.contains("拉起") || statusText.contains("尝试") -> ConnectionStage.Connecting
        statusText.contains("失败") || statusText.contains("断开") || statusText.contains("未安装") || statusText.contains("不受支持") -> ConnectionStage.Error
        else -> ConnectionStage.Idle
    }
}

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    selectedFile: SelectedFileState?,
    storageInfo: BandStorageInfoData?,
    storageRefreshing: Boolean,
    onRefreshStorage: () -> Unit,
    onPickFile: () -> Unit,
    onStartPush: () -> Unit,
    onTroubleshoot: () -> Unit = {},
    onOpenAmadeusChat: () -> Unit = {},
    amadeusEnabled: Boolean = false,
    amadeusReady: Boolean = false,
    amadeusSummary: String = "",
    onCreateFolder: (String) -> Unit = {},
    onOpenBandFiles: () -> Unit = {},
    onOpenLocalStorage: () -> Unit = {},
    onNavigateToEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()
    var showFolderDialog by remember { mutableStateOf(false) }

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
                title = "闪念小抄",
                largeTitle = "闪念小抄",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            // 连接状态卡片与 Amadeus 卡片并排
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    onTroubleshoot = onTroubleshoot,
                    modifier = Modifier.weight(1f)
                )
                com.whyy.snapnotes.ui.components.AmadeusConfigCard(
                    enabled = amadeusEnabled,
                    ready = amadeusReady,
                    summary = amadeusSummary,
                    onClick = onOpenAmadeusChat,
                    modifier = Modifier.weight(1f)
                )
            }

            StorageRingCard(
                storageInfo = storageInfo,
                isRefreshing = storageRefreshing,
                onRefresh = onRefreshStorage,
                isConnected = connectionState.isConnected
            )

            // 手环文件管理入口 + 本地存储库入口（并排）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBandFiles,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.File,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手环文件管理",
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "浏览手环文件",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLocalStorage,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Notes,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "本地存储库",
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "管理本地考点",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            if (selectedFile != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(0.dp),
                        onClick = onPickFile,
                        showIndication = true,
                        pressFeedbackType = PressFeedbackType.Tilt
                    ) {
                        BasicComponent(
                            title = selectedFile.fileName,
                            summary = selectedFile.fileSize.toReadableBytes(),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            endActions = {
                                TextButton(text = "更换", onClick = onPickFile)
                            }
                        )
                    }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPickFile,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "选择 JSON 知识点文件",
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "支持 { \"科目名\": [条目...] } 结构的 JSON",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onStartPush,
                enabled = selectedFile != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(interactionSource = null, indication = SinkFeedback()),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(
                    text = if (selectedFile != null) "开始推送到手环" else "请先选择文件",
                    color = MiuixTheme.colorScheme.onPrimary
                )
            }

            AiPromptCard(modifier = Modifier.fillMaxWidth())

            JsonFileTutorial(modifier = Modifier.fillMaxWidth())

            FormulaTutorial(modifier = Modifier.fillMaxWidth())

            // ── 知识点编辑器入口 ──
            SmallTitle(text = "知识点管理", modifier = Modifier.padding(top = 8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToEditor,
                showIndication = true,
                pressFeedbackType = PressFeedbackType.Tilt
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Edit,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "打开知识点编辑器",
                            style = MiuixTheme.textStyles.title4,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "编辑科目、条目、公式，导入导出 JSON",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(180f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    onTroubleshoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = connectionState.stage()
    val colors = when (stage) {
        ConnectionStage.Connected -> CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primaryContainer)
        ConnectionStage.Error -> CardDefaults.defaultColors(color = MiuixTheme.colorScheme.errorContainer)
        else -> CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)
    }
    val titleColor = when (stage) {
        ConnectionStage.Connected -> MiuixTheme.colorScheme.onPrimaryContainer
        ConnectionStage.Error -> MiuixTheme.colorScheme.onErrorContainer
        else -> MiuixTheme.colorScheme.onSurface
    }
    val summaryColor = when (stage) {
        ConnectionStage.Connected -> MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        ConnectionStage.Error -> MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

    // 仅在失败态允许整卡点击进排查页；其它态（连接中/已连/空闲）不可点。
    val cardClick: (() -> Unit)? = if (stage == ConnectionStage.Error) onTroubleshoot else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (cardClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = cardClick
                    )
                } else Modifier
            ),
        colors = colors,
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = false
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "ConnectionStatus"
        ) { currentStage ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentStage) {
                    ConnectionStage.Connecting -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    ConnectionStage.Connected -> Icon(MiuixIcons.Ok, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
                    ConnectionStage.Error -> Icon(MiuixIcons.Close, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
                    ConnectionStage.Idle -> Icon(MiuixIcons.Info, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connectionState.statusText,
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = connectionState.descriptionText,
                        style = MiuixTheme.textStyles.body2,
                        color = summaryColor,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}
