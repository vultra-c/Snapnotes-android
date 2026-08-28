package com.whyy.snapnotes.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.R
import com.whyy.snapnotes.logic.FormulaPngRenderer
import com.whyy.snapnotes.logic.RawToLatexConverter
import com.whyy.snapnotes.theme.AppButtonColors
import com.whyy.snapnotes.ui.components.glasense.GlasenseButton
import com.whyy.snapnotes.ui.components.glasense.GlasenseGlassPanel
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroHeader
import com.whyy.snapnotes.ui.components.glasense.GlasenseHeroIconButton
import com.whyy.snapnotes.ui.components.glasense.GlasenseTextField
import com.whyy.snapnotes.ui.components.packed.PageContent
import com.whyy.snapnotes.ui.viewmodel.EditorEntry
import com.whyy.snapnotes.ui.viewmodel.EditorSubject
import kotlinx.coroutines.delay

/**
 * 编辑页：大标题 + 空状态虚线卡 / 科目玻璃卡列表 + 新建科目虚线卡 + 导出/推送按钮。
 * 视觉对齐设计图 2（iOS 白底、玻璃长条、虚线添加卡、蓝色主色）。
 */
@Composable
fun EditorScreen(
    subjects: List<EditorSubject>,
    formulaRenderer: FormulaPngRenderer?,
    onAddSubject: () -> Unit,
    onRemoveSubject: (Int) -> Unit,
    onUpdateSubjectName: (Int, String) -> Unit,
    onAddEntry: (Int) -> Unit,
    onRemoveEntry: (Int, Int) -> Unit,
    onUpdateEntry: (Int, Int, EditorEntry) -> Unit,
    onLoadFile: () -> Unit,
    onExportToFile: () -> Unit,
    onPushFile: () -> Unit,
    backdrop: LayerBackdrop,
    liquidGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val totalEntries = subjects.sumOf { it.entries.size }

    PageContent(
        state = lazyListState,
        modifier = modifier,
        tabPadding = true,
        bottomPadding = 120.dp
    ) {
        item {
            GlasenseHeroHeader(
                title = "编辑",
                subtitle = "自定义知识点 JSON",
                backdrop = backdrop,
                liquidGlass = liquidGlass,
                trailing = {
                    GlasenseHeroIconButton(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = "加载 JSON 文件",
                        backdrop = backdrop,
                        liquidGlass = liquidGlass,
                        onClick = onLoadFile
                    )
                }
            )
            if (subjects.isNotEmpty()) {
                Text(
                    text = "$totalEntries 个条目 · ${subjects.size} 个科目",
                    style = GlasenseTheme.type.subHeadline,
                    color = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            VGap(16.dp)
        }
        if (subjects.isEmpty()) {
            item {
                EditorEmptyCard(onLoadFile = onLoadFile)
                VGap(12.dp)
            }
        }
        items(subjects.size) { subjectIndex ->
            val subject = subjects[subjectIndex]
            SubjectCard(
                subject = subject,
                formulaRenderer = formulaRenderer,
                backdrop = backdrop,
                liquidGlass = liquidGlass,
                onRemoveSubject = { onRemoveSubject(subjectIndex) },
                onUpdateSubjectName = { onUpdateSubjectName(subjectIndex, it) },
                onAddEntry = { onAddEntry(subjectIndex) },
                onRemoveEntry = { entryIndex -> onRemoveEntry(subjectIndex, entryIndex) },
                onUpdateEntry = { entryIndex, entry -> onUpdateEntry(subjectIndex, entryIndex, entry) }
            )
            VGap(12.dp)
        }
        item {
            DashedAddSubjectCard(onClick = onAddSubject)
            VGap(20.dp)
        }
        item {
            GlasenseButton(
                onClick = onExportToFile,
                colors = AppButtonColors.action(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = GlasenseTheme.colors.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "导出 JSON 文件",
                    style = GlasenseTheme.type.bodyEmphasized,
                    color = GlasenseTheme.colors.primary
                )
            }
            VGap(12.dp)
        }
        item {
            GlasenseButton(
                onClick = onPushFile,
                colors = AppButtonColors.primary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_up),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "推送到手环",
                    style = GlasenseTheme.type.bodyEmphasized,
                    textAlign = TextAlign.Center
                )
            }
        }
        paddingItem(lazyListState)
    }
}

/** 空状态大卡：虚线边框 + 居中说明 + 加载入口。 */
@Composable
private fun EditorEmptyCard(onLoadFile: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .border(
                width = 1.5.dp,
                color = GlasenseTheme.colors.contentVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLoadFile
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GlasenseTheme.colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_document),
                    contentDescription = null,
                    tint = GlasenseTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "还没有任何科目",
                style = GlasenseTheme.type.headline,
                color = GlasenseTheme.colors.content,
                textAlign = TextAlign.Center
            )
            Text(
                text = "点击下方新建，或从文件 App 加载现有 JSON",
                style = GlasenseTheme.type.footnote,
                color = GlasenseTheme.colors.contentVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 虚线边框新建科目卡。 */
@Composable
private fun DashedAddSubjectCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(72.dp)
            .border(
                width = 1.5.dp,
                color = GlasenseTheme.colors.contentVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_add_large),
                contentDescription = null,
                tint = GlasenseTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "新建科目",
                style = GlasenseTheme.type.subHeadline,
                color = GlasenseTheme.colors.content
            )
        }
    }
}

/** 科目卡：玻璃长条容器，头部为名称输入 + 删除/展开，内容为条目列表。 */
@Composable
private fun SubjectCard(
    subject: EditorSubject,
    formulaRenderer: FormulaPngRenderer?,
    backdrop: LayerBackdrop,
    liquidGlass: Boolean,
    onRemoveSubject: () -> Unit,
    onUpdateSubjectName: (String) -> Unit,
    onAddEntry: () -> Unit,
    onRemoveEntry: (Int) -> Unit,
    onUpdateEntry: (Int, EditorEntry) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "SubjectChevron"
    )

    GlasenseGlassPanel(
        backdrop = backdrop,
        shape = RoundedCornerShape(24.dp),
        liquidGlass = liquidGlass,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(GlasenseTheme.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subject.name.trim().take(1).ifEmpty { "?" },
                        style = GlasenseTheme.type.headline,
                        color = GlasenseTheme.colors.primary
                    )
                }
                BasicTransparentField(
                    value = subject.name,
                    onValueChange = onUpdateSubjectName,
                    placeholder = "科目名",
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = "删除科目",
                    tint = GlasenseTheme.colors.error,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRemoveSubject
                        )
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subject.entries.forEachIndexed { entryIndex, entry ->
                        EntryCard(
                            entry = entry,
                            formulaRenderer = formulaRenderer,
                            idWarning = entryIdWarning(subject.name, entry, subject.entries),
                            onRemoveEntry = { onRemoveEntry(entryIndex) },
                            onUpdateEntry = { onUpdateEntry(entryIndex, it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(
                                width = 1.5.dp,
                                color = GlasenseTheme.colors.contentVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAddEntry
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_large),
                                contentDescription = null,
                                tint = GlasenseTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "添加条目",
                                style = GlasenseTheme.type.subHeadline,
                                color = GlasenseTheme.colors.content
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 条目卡：嵌套浅色卡片，头部标题输入，展开后为详情表单。 */
@Composable
private fun EntryCard(
    entry: EditorEntry,
    formulaRenderer: FormulaPngRenderer?,
    idWarning: String?,
    onRemoveEntry: () -> Unit,
    onUpdateEntry: (EditorEntry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "EntryChevron"
    )
    val e = entry

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlasenseTheme.colors.scrimNormal, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTransparentField(
                value = e.title,
                onValueChange = { onUpdateEntry(e.copy(title = it)) },
                placeholder = "标题 *",
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_trash),
                contentDescription = "删除条目",
                tint = GlasenseTheme.colors.error,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemoveEntry
                    )
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = if (expanded) "收起" else "展开",
                tint = GlasenseTheme.colors.contentVariant,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlasenseTextField(
                    value = e.id,
                    onValueChange = { onUpdateEntry(e.copy(id = it)) },
                    placeholder = "编号（可选，留空自动分配）",
                    modifier = Modifier.fillMaxWidth()
                )
                if (idWarning != null) {
                    Text(
                        text = idWarning,
                        style = GlasenseTheme.type.footnote,
                        color = GlasenseTheme.colors.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                GlasenseTextField(
                    value = e.desc,
                    onValueChange = { onUpdateEntry(e.copy(desc = it)) },
                    placeholder = "简介",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )
                GlasenseTextField(
                    value = e.raw,
                    onValueChange = { onUpdateEntry(e.copy(raw = it)) },
                    placeholder = "原文",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 8
                )

                Text(
                    text = "要点 (points)",
                    style = GlasenseTheme.type.subHeadlineEmphasized,
                    color = GlasenseTheme.colors.content,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                e.points.forEachIndexed { pIdx, point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlasenseTextField(
                            value = point,
                            onValueChange = { newValue ->
                                val newPoints = e.points.toMutableList()
                                newPoints[pIdx] = newValue
                                onUpdateEntry(e.copy(points = newPoints))
                            },
                            placeholder = "要点 ${pIdx + 1}",
                            modifier = Modifier.weight(1f)
                        )
                        if (e.points.size > 1) {
                            Icon(
                                painter = painterResource(R.drawable.ic_minus),
                                contentDescription = "删除要点",
                                tint = GlasenseTheme.colors.error,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val newPoints = e.points.toMutableList().also { it.removeAt(pIdx) }
                                        onUpdateEntry(e.copy(points = newPoints))
                                    }
                            )
                        }
                    }
                }

                InlineAddText(
                    text = "添加要点",
                    onClick = { onUpdateEntry(e.copy(points = e.points + "")) }
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "公式 (formulas)",
                    style = GlasenseTheme.type.subHeadlineEmphasized,
                    color = GlasenseTheme.colors.content,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                e.formulas.forEachIndexed { fIdx, formula ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlasenseTextField(
                                value = formula,
                                onValueChange = { newValue ->
                                    val newFormulas = e.formulas.toMutableList()
                                    newFormulas[fIdx] = newValue
                                    onUpdateEntry(e.copy(formulas = newFormulas))
                                },
                                placeholder = "公式 ${fIdx + 1}",
                                modifier = Modifier.weight(1f)
                            )
                            if (e.formulas.size > 1) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_minus),
                                    contentDescription = "删除公式",
                                    tint = GlasenseTheme.colors.error,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val newFormulas = e.formulas.toMutableList().also { it.removeAt(fIdx) }
                                            onUpdateEntry(e.copy(formulas = newFormulas))
                                        }
                                )
                            }
                        }
                        FormulaPreview(
                            raw = formula,
                            renderer = formulaRenderer,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                InlineAddText(
                    text = "添加公式",
                    onClick = { onUpdateEntry(e.copy(formulas = e.formulas + "")) }
                )
            }
        }
    }
}

/** 无背景内联文本输入（用于卡片头部行内编辑）。 */
@Composable
private fun BasicTransparentField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = GlasenseTheme.type.body,
                color = GlasenseTheme.colors.contentVariant
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = GlasenseTheme.type.body.copy(color = GlasenseTheme.colors.content),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(GlasenseTheme.colors.primary)
        )
    }
}

/** 蓝色内联添加文本按钮。 */
@Composable
private fun InlineAddText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = "+ $text",
        style = GlasenseTheme.type.subHeadline,
        color = GlasenseTheme.colors.primary,
        modifier = modifier
            .padding(start = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

private enum class FormulaPreviewState { Idle, Rendering, Ready, Failed }

@Composable
private fun FormulaPreview(
    raw: String,
    renderer: FormulaPngRenderer?,
    modifier: Modifier = Modifier
) {
    var state by remember(raw) { mutableStateOf(FormulaPreviewState.Idle) }
    var pngBytes by remember(raw) { mutableStateOf<ByteArray?>(null) }
    var errorMessage by remember(raw) { mutableStateOf("") }

    LaunchedEffect(raw, renderer) {
        pngBytes = null
        errorMessage = ""
        if (raw.isBlank() || renderer == null) {
            state = FormulaPreviewState.Idle
            return@LaunchedEffect
        }
        state = FormulaPreviewState.Rendering
        delay(400)
        val latex = RawToLatexConverter.convert(raw)
        val detail = renderer.renderDetail(listOf(latex), previewMode = true)
        if (detail == null || detail.png == null) {
            errorMessage = detail?.errorMessages?.firstOrNull() ?: "渲染失败"
            state = FormulaPreviewState.Failed
        } else {
            pngBytes = detail.png.bytes
            state = FormulaPreviewState.Ready
        }
    }

    when (state) {
        FormulaPreviewState.Idle -> Unit
        FormulaPreviewState.Rendering -> {
            Text(
                text = "渲染中…",
                style = GlasenseTheme.type.footnote,
                color = GlasenseTheme.colors.contentVariant,
                modifier = modifier
            )
        }
        FormulaPreviewState.Failed -> {
            Text(
                text = "无法渲染：$errorMessage",
                style = GlasenseTheme.type.footnote,
                color = GlasenseTheme.colors.error,
                modifier = modifier
            )
        }
        FormulaPreviewState.Ready -> {
            val bytes = pngBytes ?: return
            val bitmap = remember(bytes) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            if (bitmap != null) {
                // 预览渲染已按内容收缩 + 放大字号（36px×2 分辨率），这里按内容尺寸显示并
                // 封顶到屏宽：短公式保持大字号，超宽公式等比缩放进屏。
                val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                val displayWidth = minOf(bitmap.width.dp, screenWidthDp)
                val displayHeight =
                    displayWidth * (bitmap.height.toFloat() / bitmap.width.toFloat())
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161618))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "公式渲染预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(displayWidth)
                            .height(displayHeight)
                    )
                }
            }
        }
    }
}

/**
 * 手环内置知识点：各科目独立编号（语文1-68、数学1-12、英语1-8、物理1-15、
 * 化学1-10、生物1-11、历史1-6、地理1-6、政治1-7、信息技术1-16，合计159条）。
 * 用户给内置科目补充内容时，编号落在该科目内置区间内会被手环跳过（不覆盖）。
 */
private val BUILTIN_SUBJECT_ID_RANGES = mapOf(
    "语文" to 1..68,
    "数学" to 1..12,
    "英语" to 1..8,
    "物理" to 1..15,
    "化学" to 1..10,
    "生物" to 1..11,
    "历史" to 1..6,
    "地理" to 1..6,
    "政治" to 1..7,
    "信息技术" to 1..16,
)

/**
 * 计算某条目的编号冲突提示：
 * - 若科目名是手环内置科目，编号落在该科目内置区间内：推送后同编号条目不会被覆盖更新；
 * - 同科目内编号重复：手环按编号合并，重复条目不会新增。
 * 空编号不提示；两条可叠加。返回 null 表示无冲突。
 */
private fun entryIdWarning(
    subjectName: String,
    entry: EditorEntry,
    entries: List<EditorEntry>
): String? {
    val id = entry.id.trim()
    if (id.isEmpty()) return null
    val idNum = id.toIntOrNull() ?: return null
    val builtinRange = BUILTIN_SUBJECT_ID_RANGES[subjectName.trim()]
    val inBuiltinRange = builtinRange?.contains(idNum) == true
    val duplicated = entries.count { it.id.trim() == id } > 1
    if (!inBuiltinRange && !duplicated) return null
    return buildString {
        if (inBuiltinRange) {
            append("编号落在内置「$subjectName」已占用区间（${builtinRange!!.first}-${builtinRange.last}）内，推送后同编号条目不会被覆盖更新")
        }
        if (inBuiltinRange && duplicated) append("；")
        if (duplicated) append("该科目内编号重复，重复条目不会新增")
    }
}
