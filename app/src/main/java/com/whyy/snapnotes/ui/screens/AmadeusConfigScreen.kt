package com.whyy.snapnotes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whyy.snapnotes.logic.ModelCatalog
import com.whyy.snapnotes.logic.ModelIconLoader
import com.whyy.snapnotes.logic.ModelInfo
import com.whyy.snapnotes.ui.components.AppCard
import com.whyy.snapnotes.ui.components.AppDialog
import com.whyy.snapnotes.ui.viewmodel.AmadeusConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 手环端 Amadeus AI 聊天助手的配置页（手机端）。
 *
 * - 全部字段只存手机端 SharedPreferences（见 [com.whyy.snapnotes.ui.viewmodel.SnapNotesViewModel] 的
 *   Amadeus 段），手环不传不存不感知。首次启动内置 NVIDIA NIM 配置（baseUrl/apiKey/model），
 *   开箱即可用；用户在配置页覆盖后以用户值为准。
 * - 「启用」关闭时下方 API 项依旧可见可填，但 [AmadeusConfig.isReady] 在未启用时永远为 false。
 * - Model 支持内置模型目录（带品牌图标）+ 自动获取（GET /v1/models）+ 手动输入三种来源。
 * - TopAppBar 右上角图标进「上下文管理菜单」（[AmadeusContextScreen]）。
 *
 * @param onBackClick 返回主页（由 NavDisplay 的 onBack 兜底，本回调用于 TopAppBar 返回箭头）。
 * @param onOpenContext 打开上下文管理菜单。
 * @param availableModels 自动获取到的可用模型列表（null=未获取，空=获取中或失败）
 * @param onFetchModels 触发获取可用模型
 */
@Composable
fun AmadeusConfigScreen(
    config: AmadeusConfig,
    onEnabledChange: (Boolean) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenContext: () -> Unit,
    availableModels: List<String>? = null,
    modelsLoading: Boolean = false,
    onFetchModels: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 当前正在编辑的文本字段；非 null 时弹出对应编辑对话框。
    var editingField by remember { mutableStateOf<EditField?>(null) }
    // 是否显示模型选择对话框
    var showModelPicker by remember { mutableStateOf(false) }

    // 记录最近一次成功获取模型的时间戳，用于在卡片上提示「上次获取 HH:mm」。
    var lastFetchTime by remember { mutableStateOf<Long?>(null) }
    val wasLoading = remember { mutableStateOf(false) }
    LaunchedEffect(modelsLoading, availableModels) {
        // 仅在「由加载中 -> 加载完成且拿到结果」这一跳变时记录时间，避免每次重组都覆盖。
        if (wasLoading.value && !modelsLoading && !availableModels.isNullOrEmpty()) {
            lastFetchTime = System.currentTimeMillis()
        }
        wasLoading.value = modelsLoading
    }

    BackHandler { onBackClick() }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = "Amadeus",
                largeTitle = "Amadeus",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenContext, modifier = Modifier.padding(end = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Notes, contentDescription = "上下文管理")
                    }
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
                SmallTitle(text = "基本", modifier = Modifier.padding(top = 12.dp))
                AppCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    BasicComponent(
                        title = "启用 Amadeus",
                        summary = "关闭后手环端聊天不会发起调用",
                        endActions = {
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = onEnabledChange
                            )
                        }
                    )
                }
            }
            item {
                SmallTitle(text = "API")
                AppCard(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    // AppCard 内容为 Box 布局，多个条目必须用 Column 纵向排布，否则会重叠
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BasicComponent(
                            title = "Base URL",
                            summary = config.baseUrl.ifBlank { "留空 = 走厂商默认" },
                            onClick = { editingField = EditField.BaseUrl }
                        )
                        BasicComponent(
                            title = "API Key",
                            summary = if (config.apiKey.isNotBlank()) "已设置（点此修改）" else "未设置",
                            onClick = { editingField = EditField.ApiKey }
                        )
                        BasicComponent(
                            title = "Model",
                            summary = if (config.model.isBlank()) "未设置"
                            else ModelCatalog.displayNameFor(config.model),
                            onClick = { showModelPicker = true }
                        )
                    }
                }
            }
            // 模型获取操作卡：用 primaryContainer 着色突出动作属性，左侧带 Refresh 徽标，
            // 加载中替换为小转圈；未填 API Key 时不再静默禁用，而是点击直接跳去填 Key。
            item {
                val apiKeyBlank = config.apiKey.isBlank()
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    containerColor = MiuixTheme.colorScheme.primaryContainer,
                    onClick = {
                        when {
                            apiKeyBlank -> editingField = EditField.ApiKey
                            modelsLoading -> Unit
                            else -> {
                                onFetchModels()
                                if (availableModels.isNullOrEmpty()) showModelPicker = true
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (modelsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = MiuixIcons.Refresh,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (modelsLoading) "正在获取可用模型…" else "获取可用模型",
                                style = MiuixTheme.textStyles.title4,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                            val subtitle = when {
                                modelsLoading -> "请稍候，正在请求 /v1/models"
                                apiKeyBlank -> "请先填写 API Key（点击去填写）"
                                !availableModels.isNullOrEmpty() -> {
                                    val count = "已获取 ${availableModels.size} 个模型"
                                    lastFetchTime?.let {
                                        "$count · 上次更新 ${formatFetchTime(it)}"
                                    } ?: count
                                }
                                availableModels != null -> "未获取到模型，点击重试"
                                else -> "点击从服务端拉取可用模型列表"
                            }
                            Text(
                                text = subtitle,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }

    // 文本编辑对话框（Base URL / API Key）
    AmadeusTextEditDialog(
        show = editingField != null && editingField != EditField.Model,
        title = editingField?.title ?: "",
        label = editingField?.label ?: "",
        hint = editingField?.hint ?: "",
        initial = when (editingField) {
            EditField.BaseUrl -> config.baseUrl
            EditField.ApiKey -> config.apiKey
            else -> ""
        },
        onDismiss = { editingField = null },
        onConfirm = { value ->
            editingField?.let { field ->
                when (field) {
                    EditField.BaseUrl -> onBaseUrlChange(value)
                    EditField.ApiKey -> onApiKeyChange(value)
                    else -> Unit
                }
            }
            editingField = null
        }
    )

    // 模型选择/手动输入对话框
    if (showModelPicker) {
        ModelPickerDialog(
            show = true,
            currentModel = config.model,
            apiKey = config.apiKey,
            availableModels = availableModels,
            loading = modelsLoading,
            onSelect = { model ->
                onModelChange(model)
                showModelPicker = false
            },
            onManualInput = { model ->
                onModelChange(model)
                showModelPicker = false
            },
            onRefresh = onFetchModels,
            onDismiss = { showModelPicker = false }
        )
    }
}

/**
 * 模型选择对话框：内置推荐模型（带品牌图标）+ 服务端获取模型 + 手动输入。
 *
 * - 打开时若从未获取过（[availableModels] == null）且已填 [apiKey]，自动触发一次 [onRefresh]。
 * - 模型超过 5 个时顶部出现搜索框，按名称/id 实时过滤。
 * - 每个模型条目左侧是 Gemini 风格的圆形品牌图标（@lobehub/icons CDN），右侧选中态打勾。
 */
@Composable
private fun ModelPickerDialog(
    show: Boolean,
    currentModel: String,
    apiKey: String,
    availableModels: List<String>?,
    loading: Boolean,
    onSelect: (String) -> Unit,
    onManualInput: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var manualText by remember { mutableStateOf(currentModel) }
    var showManualInput by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(show) {
        if (show) {
            manualText = currentModel
            showManualInput = false
            searchQuery = ""
            // 首次打开且从未获取过、已填 Key，则自动拉取一次。
            if (availableModels == null && apiKey.isNotBlank()) {
                onRefresh()
            }
        }
    }

    // 内置目录 + 服务端模型（去重后追加），始终有可选内容。
    val allModels = remember(availableModels) { ModelCatalog.merge(availableModels) }
    val showSearch = allModels.size > 5
    val filteredModels = remember(allModels, searchQuery) {
        if (searchQuery.isBlank()) allModels
        else allModels.filter {
            it.id.contains(searchQuery, ignoreCase = true) ||
                it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    AppDialog(
        title = "选择模型",
        summary = "从列表选择，或手动输入模型名",
        show = show,
        onDismissRequest = onDismiss,
        dismissText = "关闭",
        confirmText = "",
        onDismiss = onDismiss,
        onConfirm = onDismiss,
        content = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "模型（${allModels.size}）",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f)
                    )
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = "刷新",
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (showSearch) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        label = "搜索模型",
                        leadingIcon = {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Close,
                                        contentDescription = "清除"
                                    )
                                }
                            }
                        } else null,
                        singleLine = true
                    )
                }

                LazyColumn(modifier = Modifier.height(260.dp)) {
                    if (filteredModels.isEmpty()) {
                        item {
                            Text(
                                text = if (searchQuery.isBlank()) "暂无模型"
                                else "未找到匹配「$searchQuery」的模型",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    } else {
                        // key 用 model id，配合 animateItem() 才能在过滤/刷新时正确追踪并做位移动画。
                        items(filteredModels, key = { it.id }) { info ->
                            ModelListItem(
                                info = info,
                                isSelected = info.id == currentModel,
                                onClick = { onSelect(info.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 手动输入区域
                AnimatedVisibility(
                    visible = showManualInput,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        TextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            modifier = Modifier.fillMaxWidth().imePadding(),
                            singleLine = true,
                            label = "手动输入模型名"
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                text = "取消",
                                onClick = { showManualInput = false },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(20.dp))
                            TextButton(
                                text = "确定",
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                onClick = { onManualInput(manualText.trim()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (!showManualInput) {
                    TextButton(
                        text = "手动输入模型名",
                        onClick = { showManualInput = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

/**
 * 单个模型条目：左侧 Gemini 风格圆形品牌图标，中间友好名 + 模型 id，
 * 选中时以 primaryContainer 高亮、右侧打勾。
 */
@Composable
private fun ModelListItem(
    info: ModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MiuixTheme.colorScheme.primaryContainer else Color.Transparent
    val titleColor = if (isSelected) {
        MiuixTheme.colorScheme.onPrimaryContainer
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderIcon(provider = info.provider, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.displayName,
                style = MiuixTheme.textStyles.body2,
                color = titleColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (info.displayName != info.id) {
                Text(
                    text = info.id,
                    style = MiuixTheme.textStyles.footnote2,
                    color = titleColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = MiuixIcons.Ok,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Gemini 风格的模型品牌图标：白色圆形底 + 彩色品牌 logo。
 * 图标来自 @lobehub/icons 静态 CDN（见 [ModelIconLoader]）；加载失败时回退为品牌首字母。
 */
@Composable
private fun ProviderIcon(
    provider: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(provider) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(provider) {
        bitmap = withContext(Dispatchers.IO) { ModelIconLoader.load(provider)?.asImageBitmap() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.7f)
            )
        } else {
            Text(
                text = provider.take(1).uppercase(),
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Bold,
                color = providerAccent(provider)
            )
        }
    }
}

/** 品牌首字母回退色（logo 加载失败 / 加载中时用）。 */
private fun providerAccent(provider: String): Color = when (provider) {
    "nvidia" -> Color(0xFF76B900)
    "meta" -> Color(0xFF0866FF)
    "deepseek" -> Color(0xFF4D6BFE)
    "qwen" -> Color(0xFF615CED)
    "moonshot" -> Color(0xFF0D0E12)
    "mistral" -> Color(0xFFFA520F)
    "google" -> Color(0xFF4285F4)
    "microsoft" -> Color(0xFF00A4EF)
    "openai" -> Color(0xFF10A37F)
    "anthropic" -> Color(0xFFD97757)
    "zhipu" -> Color(0xFF3859FF)
    else -> Color(0xFF76B900)
}

/**
 * 文本编辑弹窗，使用 AppDialog 并利用其 show 参数控制显隐动画。
 */
@Composable
private fun AmadeusTextEditDialog(
    show: Boolean,
    title: String,
    label: String,
    hint: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(show, initial) {
        if (show) {
            text = initial
            delay(80)
            focusRequester.requestFocus()
        }
    }

    AppDialog(
        title = title,
        summary = hint,
        show = show,
        onDismissRequest = onDismiss,
        dismissText = "取消",
        confirmText = "确定",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(text.trim()) },
        content = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = label
            )
        }
    )
}

/** 把时间戳格式化成「HH:mm」用于卡片上的「上次更新」提示。失败返回空串。 */
private fun formatFetchTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

/** 可弹窗编辑的文本项；集中管理标题/标签/提示文案。 */
private enum class EditField(val title: String, val label: String, val hint: String) {
    BaseUrl("Base URL", "API 根地址", "留空走厂商默认，如 https://integrate.api.nvidia.com"),
    ApiKey("API Key", "密钥", "请输入API密钥"),
    Model("Model", "模型名", "如 meta/llama-3.3-70b-instruct")
}
