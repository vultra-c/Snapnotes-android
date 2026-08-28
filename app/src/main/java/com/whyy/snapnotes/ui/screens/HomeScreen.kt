package com.whyy.snapnotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.logic.BandStorageInfoData
import com.whyy.snapnotes.theme.AppButtonColors
import com.whyy.snapnotes.ui.components.StorageRingCard
import com.whyy.snapnotes.ui.components.FormulaTutorial
import com.whyy.snapnotes.ui.components.JsonFileTutorial
import com.whyy.snapnotes.ui.LocalTabVisible
import com.whyy.snapnotes.ui.pageContentBackdrop
import com.whyy.snapnotes.ui.rememberPageBackdrop
import com.whyy.snapnotes.ui.components.glasense.GlasenseButton
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroHeader
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroIconButton
import com.whyy.snapnotes.ui.components.glasense.GlasenseSurfaceCard
import com.whyy.snapnotes.ui.components.glasense.pressEffect
import com.whyy.snapnotes.ui.components.glasense.rememberPressInteractionSource
import com.whyy.snapnotes.ui.components.packed.PageContent
import com.whyy.snapnotes.ui.viewmodel.ConnectionState
import com.whyy.snapnotes.ui.viewmodel.SelectedFileState
import com.whyy.snapnotes.ui.viewmodel.toReadableBytes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.layout.onSizeChanged

/**
 * 主页：固定磨砂大标题区（滚过内容被模糊）+ 连接状态 + 知识库文件卡 + 虚线添加卡 +
 * 存储卡 + 推送按钮 + 教程。视觉对齐设计图 1（iOS 白底、玻璃按钮、蓝色主色）。
 */
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
    val pageBackdrop = rememberPageBackdrop()
    val tabVisible = LocalTabVisible.current
    val liquidGlass = true
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

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
            item {
                ConnectionStatusRow(
                    connectionState = connectionState,
                    amadeusSummary = amadeusSummary,
                    onTroubleshoot = onTroubleshoot
                )
                VGap(20.dp)
            }
            item {
                Text(
                    text = "知识库",
                    style = GlasenseTheme.type.subHeadline,
                    color = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
            }
            item {
                if (selectedFile != null) {
                    KnowledgeFileCard(
                        fileName = selectedFile.fileName,
                        fileSize = selectedFile.fileSize.toReadableBytes(),
                        onClick = onPickFile
                    )
                    VGap(12.dp)
                }
                DashedAddCard(onClick = onPickFile)
                VGap(20.dp)
            }
            item {
                StorageRingCard(
                    storageInfo = storageInfo,
                    isRefreshing = storageRefreshing,
                    onRefresh = onRefreshStorage,
                    isConnected = connectionState.isConnected
                )
                VGap(20.dp)
            }
            item {
                GlasenseButton(
                    onClick = onStartPush,
                    enabled = selectedFile != null,
                    colors = AppButtonColors.primary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (selectedFile != null) "开始推送到手环" else "请先选择文件",
                        style = GlasenseTheme.type.bodyEmphasized,
                        textAlign = TextAlign.Center
                    )
                }
                VGap(20.dp)
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

        GlasenseHeroHeader(
            title = "闪念小抄",
            subtitle = null,
            backdrop = pageBackdrop,
            liquidGlass = liquidGlass && tabVisible,
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { headerHeightPx = it.height },
            trailing = {
                GlasenseHeroIconButton(
                    painter = painterResource(R.drawable.ic_add_large),
                    contentDescription = "选择 JSON 文件",
                    backdrop = pageBackdrop,
                    liquidGlass = liquidGlass && tabVisible,
                    onClick = onPickFile
                )
            }
        )
    }
}

/** 状态行：绿点已连接 / 红点异常（可点进排查）/ Amadeus 摘要。 */
@Composable
private fun ConnectionStatusRow(
    connectionState: ConnectionState,
    amadeusSummary: String,
    onTroubleshoot: () -> Unit
) {
    val connected = connectionState.isConnected
    val failed = connectionState.statusText.contains("失败") ||
            connectionState.statusText.contains("断开") ||
            connectionState.statusText.contains("未安装") ||
            connectionState.statusText.contains("不受支持")
    val dotColor = when {
        connected -> Color(0xFF34C759)
        failed -> GlasenseTheme.colors.error
        else -> GlasenseTheme.colors.contentVariant
    }
    val statusText = connectionState.statusText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(if (failed) Modifier.clickable(onClick = onTroubleshoot) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = statusText,
            style = GlasenseTheme.type.subHeadline,
            color = GlasenseTheme.colors.content
        )
        if (amadeusEnabled(amadeusSummary)) {
            Text(
                text = "  ·  Amadeus $amadeusSummary",
                style = GlasenseTheme.type.subHeadline,
                color = GlasenseTheme.colors.contentVariant
            )
        }
    }
}

private fun amadeusEnabled(summary: String): Boolean = summary != "未启用"

/** 知识库文件卡：浅蓝图标容器 + 文件名/大小 + 更换箭头（纯色 iOS 卡 + 按压效果）。 */
@Composable
private fun KnowledgeFileCard(
    fileName: String,
    fileSize: String,
    onClick: () -> Unit
) {
    val interaction = rememberPressInteractionSource()

    GlasenseSurfaceCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .pressEffect(interaction)
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileIconBadge()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = GlasenseTheme.type.bodyEmphasized,
                    color = GlasenseTheme.colors.content
                )
                Text(
                    text = fileSize,
                    style = GlasenseTheme.type.footnote,
                    color = GlasenseTheme.colors.contentVariant
                )
            }
            Text(
                text = "更换",
                style = GlasenseTheme.type.subHeadline,
                color = GlasenseTheme.colors.primary
            )
            Icon(
                painter = painterResource(R.drawable.ic_forward_nav),
                contentDescription = null,
                tint = GlasenseTheme.colors.contentVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 虚线边框添加卡：+ 添加 JSON 文件。 */
@Composable
private fun DashedAddCard(onClick: () -> Unit) {
    val interaction = rememberPressInteractionSource()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(112.dp)
            .pressEffect(interaction)
            .border(
                width = 1.5.dp,
                color = GlasenseTheme.colors.contentVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_add_large),
                contentDescription = null,
                tint = GlasenseTheme.colors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "添加 JSON 文件",
                style = GlasenseTheme.type.subHeadline,
                color = GlasenseTheme.colors.content
            )
            Text(
                text = "支持从文件 App 导入",
                style = GlasenseTheme.type.footnote,
                color = GlasenseTheme.colors.contentVariant
            )
        }
    }
}

/** 浅蓝圆角方块内的文件图标（设计图同款）。 */
@Composable
private fun FileIconBadge() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlasenseTheme.colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_document),
            contentDescription = null,
            tint = GlasenseTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
