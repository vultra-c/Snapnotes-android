package com.whyy.snapnotes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.logic.BandStorageInfoData
import com.whyy.snapnotes.theme.AppButtonColors
import com.whyy.snapnotes.theme.AppColors
import com.whyy.snapnotes.theme.AppSpecs
import com.whyy.snapnotes.ui.components.AmadeusConfigCard
import com.whyy.snapnotes.ui.components.FormulaTutorial
import com.whyy.snapnotes.ui.components.JsonFileTutorial
import com.whyy.snapnotes.ui.components.StorageRingCard
import com.whyy.snapnotes.ui.components.glasense.GlasenseButton
import com.whyy.snapnotes.ui.components.glasense.GlasensePageHeader
import com.whyy.snapnotes.ui.components.packed.PageContent
import com.whyy.snapnotes.ui.viewmodel.ConnectionState
import com.whyy.snapnotes.ui.viewmodel.SelectedFileState
import com.whyy.snapnotes.ui.viewmodel.toReadableBytes

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
    amadeusEnabled: Boolean = false,
    amadeusReady: Boolean = false,
    amadeusSummary: String = "未启用",
    onOpenAmadeus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    PageContent(
        state = lazyListState,
        modifier = modifier,
        tabPadding = true,
        bottomPadding = 120.dp
    ) {
        item {
            GlasensePageHeader(title = "闪念小抄")
        }
        item {
            // 顶部一排：左连接手环卡片，右 Amadeus 配置入口卡片，并排各占半宽。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    onTroubleshoot = onTroubleshoot,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                AmadeusConfigCard(
                    enabled = amadeusEnabled,
                    ready = amadeusReady,
                    summary = amadeusSummary,
                    onClick = onOpenAmadeus,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            VGap(12.dp)
        }
        item {
            StorageRingCard(
                storageInfo = storageInfo,
                isRefreshing = storageRefreshing,
                onRefresh = onRefreshStorage,
                isConnected = connectionState.isConnected
            )
            VGap(12.dp)
        }
        item {
            if (selectedFile != null) {
                SelectedFileCard(
                    fileName = selectedFile.fileName,
                    fileSize = selectedFile.fileSize.toReadableBytes(),
                    onClick = onPickFile
                )
            } else {
                PickFileCard(onClick = onPickFile)
            }
            VGap(12.dp)
        }
        item {
            GlasenseButton(
                onClick = onStartPush,
                enabled = selectedFile != null,
                colors = AppButtonColors.primary(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedFile != null) "开始推送到手环" else "请先选择文件",
                    style = GlasenseTheme.type.bodyEmphasized,
                    textAlign = TextAlign.Center
                )
            }
            VGap(12.dp)
        }
        item {
            JsonFileTutorial(modifier = Modifier.fillMaxWidth())
            VGap(12.dp)
        }
        item {
            FormulaTutorial(modifier = Modifier.fillMaxWidth())
        }
        paddingItem(lazyListState)
    }
}

@Composable
private fun SelectedFileCard(
    fileName: String,
    fileSize: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cardBackground, AppSpecs.cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_checkmark_circle),
            contentDescription = null,
            tint = AppColors.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = GlasenseTheme.type.bodyEmphasized,
                color = AppColors.content,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = fileSize,
                style = GlasenseTheme.type.footnote,
                color = AppColors.contentVariant
            )
        }
        Text(
            text = "更换",
            style = GlasenseTheme.type.subHeadline,
            color = AppColors.primary
        )
    }
}

@Composable
private fun PickFileCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.cardBackground, AppSpecs.cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = AppColors.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "选择 JSON 知识点文件",
                style = GlasenseTheme.type.bodyEmphasized,
                color = AppColors.content
            )
            Text(
                text = "支持 { \"科目名\": [条目...] } 结构的 JSON",
                style = GlasenseTheme.type.footnote,
                color = AppColors.contentVariant
            )
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
    val containerColor = when (stage) {
        ConnectionStage.Connected -> AppColors.primary
        ConnectionStage.Error -> AppColors.error
        else -> AppColors.cardBackground
    }
    val titleColor = when (stage) {
        ConnectionStage.Connected -> AppColors.onPrimary
        ConnectionStage.Error -> AppColors.onError
        else -> AppColors.content
    }
    val summaryColor = when (stage) {
        ConnectionStage.Connected -> AppColors.onPrimary.copy(alpha = 0.8f)
        ConnectionStage.Error -> AppColors.onError.copy(alpha = 0.8f)
        else -> AppColors.contentVariant
    }

    // 仅在失败态允许整卡点击进排查页；其它态（连接中/已连/空闲）不可点。
    val cardClick: (() -> Unit)? = if (stage == ConnectionStage.Error) onTroubleshoot else null

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
        },
        label = "ConnectionStatus",
        modifier = modifier
    ) { currentStage ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .background(containerColor, AppSpecs.cardShape)
                .then(
                    if (cardClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = cardClick
                        )
                    } else Modifier
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentStage) {
                ConnectionStage.Connected -> Icon(
                    painter = painterResource(R.drawable.ic_checkmark),
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(24.dp)
                )
                ConnectionStage.Error -> Icon(
                    painter = painterResource(R.drawable.ic_xmark_bold),
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(20.dp)
                )
                else -> Icon(
                    painter = painterResource(R.drawable.ic_mini_info),
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connectionState.statusText,
                    style = GlasenseTheme.type.subHeadlineEmphasized,
                    color = titleColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = connectionState.descriptionText,
                    style = GlasenseTheme.type.footnote,
                    color = summaryColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}
