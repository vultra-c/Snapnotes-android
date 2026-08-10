package com.whyy.snapnotes.ui.liquid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.isRenderEffectSupported

/**
 * 液态玻璃弹出层表面（下拉菜单 / 长按菜单 / 对话框通用）。
 *
 * - 全屏半透明遮罩（可选 [scrimColor]），点击 [onDismissRequest] 关闭；
 * - 内容面板带毛玻璃（vibrancy + blur）与轻微折射，圆角胶囊；
 * - 展开/收起带淡入 + 轻微缩放的弹性动画。
 */
@Composable
fun LiquidGlassPopupSurface(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    shape: Shape = RoundedCornerShape(24.dp),
    surfaceColor: Color = Color.White.copy(alpha = 0.55f),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val rootBackdrop = LocalLiquidGlassBackdrop.current
    // 弹出层和对话框不再使用液态玻璃，仅保留普通半透明面板与开合动画。
    val useGlass = false

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
            initialScale = 0.92f
        ),
        exit = fadeOut(tween(140)) + scaleOut(
            animationSpec = tween(140),
            targetScale = 0.96f
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 28.dp)
                    .then(
                        if (useGlass) {
                            Modifier.drawBackdrop(
                                backdrop = rootBackdrop!!,
                                shape = { shape },
                                effects = {
                                    val subtle = if (config.subtleMode) 0.55f else 1f
                                    vibrancy()
                                    blur(18.dp.toPx() * subtle)
                                },
                                onDrawSurface = {
                                    drawRect(surfaceColor)
                                }
                            )
                        } else {
                            Modifier.background(surfaceColor, shape)
                        }
                    )
                    .padding(contentPadding),
                content = content
            )
        }
    }
}
