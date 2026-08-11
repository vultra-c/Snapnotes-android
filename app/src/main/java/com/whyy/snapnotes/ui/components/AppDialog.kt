package com.whyy.snapnotes.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    summary: String = "",
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = onDismissRequest,
    content: (@Composable () -> Unit)? = null
) {
    AppPopupSurface(
        visible = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface
                )
            }
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            if (content != null) {
                Spacer(Modifier.height(14.dp))
                androidx.compose.foundation.layout.Box(modifier = Modifier.imePadding()) {
                    content()
                }
            }
            Spacer(Modifier.height(16.dp))
            RowOfButtons(
                dismissText = dismissText,
                confirmText = confirmText,
                confirmEnabled = confirmEnabled,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun RowOfButtons(
    dismissText: String,
    confirmText: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dismissText.isNotBlank()) {
            top.yukonga.miuix.kmp.basic.TextButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(0.dp))
        }
        top.yukonga.miuix.kmp.basic.TextButton(
            text = confirmText,
            enabled = confirmEnabled,
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        )
    }
}
