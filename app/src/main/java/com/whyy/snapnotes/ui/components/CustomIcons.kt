package com.whyy.snapnotes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.whyy.snapnotes.R

/**
 * 自定义返回按钮图标（使用 ic_custom_back.png）。
 * 替代 MiuixIcons.Back，大小与默认 Icon 一致。
 */
@Composable
fun CustomBackIcon(
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.ic_custom_back),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/**
 * 自定义更多按钮图标（使用 ic_custom_more.png）。
 * 替代 MiuixIcons.More，大小与默认 Icon 一致。
 */
@Composable
fun CustomMoreIcon(
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.ic_custom_more),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
