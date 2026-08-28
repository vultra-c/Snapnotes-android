package com.whyy.snapnotes.ui.components.glasense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.theme.AppColors
import com.whyy.snapnotes.theme.AppSpecs

/**
 * Glasense 风格的确认/输入对话框便捷包装。
 * 将 [GlasenseDialog] 的 DialogState/show 细节收拢，供各业务对话框直接使用。
 */
@Composable
fun GlasenseAlertDialog(
    show: Boolean,
    title: String,
    message: String? = null,
    items: List<DialogItemData>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    val backdrop = rememberLayerBackdrop { drawRect(AppColors.background) }
    val dialogState = remember(show, title, message, items) {
        DialogState(
            isVisible = show,
            title = title,
            message = message,
            items = items
        )
    }
    GlasenseDialog(
        dialogState = dialogState,
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = modifier,
        content = content
    )
}

/**
 * Glasense 风格文本输入框：小标签 + 圆角填充输入区。
 * 交互与 Miuix TextField 接近（value/onValueChange + label），方便页面平移。
 */
@Composable
fun GlasenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = GlasenseTheme.type.footnote,
                color = AppColors.contentVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            Spacer(Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .background(AppColors.scrimNormal, AppSpecs.textFieldShape),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .heightIn(min = 24.dp),
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                textStyle = GlasenseTheme.type.body.copy(color = AppColors.content),
                cursorBrush = SolidColor(AppColors.primary)
            )
        }
    }
}
