package com.whyy.snapnotes.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.whyy.snapnotes.theme.AppColors

/** 当前活动 tab 页面上报的玻璃源（PageContent 的 LayerBackdrop），供底部导航条采样。 */
val LocalActivePageBackdrop = compositionLocalOf<MutableState<LayerBackdrop?>?> { null }

/** 本页是否为当前可见 tab（用于决定是否上报玻璃源）。 */
val LocalTabVisible = compositionLocalOf { true }

/**
 * 页面级玻璃源：LayerBackdrop 挂在本页 PageContent 上，滚动内容是采样素材。
 * 页面内的固定 header / 底部操作栏 / 弹出菜单（PageContent 的兄弟节点）采样它获得
 * 真实的内容模糊；底部导航条通过 LocalActivePageBackdrop 读取当前可见页的源。
 *
 * 同时把本页注册为活动玻璃源（仅当本页可见时）。
 */
@Composable
fun rememberPageBackdrop(): LayerBackdrop {
    val pageBackground = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = pageBackground,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }
    val activeBackdrop = LocalActivePageBackdrop.current
    val tabVisible = LocalTabVisible.current
    LaunchedEffect(backdrop, tabVisible) {
        if (tabVisible) {
            activeBackdrop?.value = backdrop
        }
    }
    return backdrop
}

/** 给 LazyColumn 内容层挂玻璃源的标准修饰链。 */
fun Modifier.pageContentBackdrop(backdrop: LayerBackdrop): Modifier =
    this.then(Modifier.layerBackdrop(backdrop))
