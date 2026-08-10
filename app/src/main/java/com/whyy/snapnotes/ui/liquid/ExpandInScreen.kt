@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.whyy.snapnotes.ui.liquid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.isRenderEffectSupported
import kotlin.math.max

/**
 * 展开入口的原始位置（窗口坐标，px）。
 *
 * 由首页入口卡片在 [Modifier.onGloballyPositioned] 中记录，点击后传给
 * [ExpandInScreen]，目标页即从该位置“逐渐展开并铺满屏幕”。
 */
data class ExpandOrigin(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    companion object {
        val None = ExpandOrigin(0f, 0f, 0f, 0f)
    }
}

/**
 * 类似桌面「应用打开」的展开动画：
 *
 * - 进入：目标页从 [origin] 卡片位置按弹簧放大到全屏，圆角由卡片半径渐隐到 0；
 * - 背景：全屏遮罩随进度加深（半透明 + 渐进模糊），模糊强度与展开进度同步；
 * - 退出：调用 [onBackRequested] 后先反向收缩回 [origin]，动画结束再回调
 *   [onExitFinished]，由导航层真正移除页面，避免“收缩动画被导航切换截断”。
 */
@Composable
fun ExpandInScreen(
    origin: ExpandOrigin,
    onBackRequested: () -> Unit,
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier,
    originCornerRadiusDp: Float = 28f,
    scrimMaxAlpha: Float = 0.45f,
    maxBlurRadiusDp: Float = 42f,
    content: @Composable (onRequestExit: () -> Unit) -> Unit
) {
    val density = LocalDensity.current
    val useBlur = isRenderEffectSupported()

    var rendering by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // progress: 0 = 卡片原位（最小），1 = 铺满全屏
    val progress by animateFloatAsState(
        targetValue = if (finishing) 0f else 1f,
        animationSpec = if (finishing) springSpec else springSpec,
        label = "expandProgress"
    )

    val originW = max(origin.width, 1f)
    val originH = max(origin.height, 1f)
    val targetW = max(rootSize.width, 1f)
    val targetH = max(rootSize.height, 1f)

    val scale = if (originW >= targetW) {
        1f
    } else {
        (originW / targetW) + (1f - originW / targetW) * progress
    }
    // 从卡片中心平移到屏幕中心（初始位置）
    val originCenterX = origin.left + originW / 2f
    val originCenterY = origin.top + originH / 2f
    val targetCenterX = targetW / 2f
    val targetCenterY = targetH / 2f
    val offsetX = (originCenterX - targetCenterX) * (1f - progress)
    val offsetY = (originCenterY - targetCenterY) * (1f - progress)

    val cornerRadius = (originCornerRadiusDp * (1f - progress)).coerceAtLeast(0f).dp
    val scrimAlpha = scrimMaxAlpha * progress
    val blurRadiusPx = with(density) { maxBlurRadiusDp.dp.toPx() * progress }

    fun requestExit() {
        if (!finishing) {
            finishing = true
            onBackRequested()
        }
    }

    BackHandler(enabled = rendering && !finishing) { requestExit() }

    // 进入：首帧先渲染（保持 0 尺寸的图形层也无妨），再触发动画
    LaunchedEffect(Unit) { rendering = true }

    // 退出动画结束后，才真正移除页面
    LaunchedEffect(finishing) {
        if (finishing) {
            kotlinx.coroutines.delay(420)
            onExitFinished()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val size = coords.size
                rootSize = androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        AnimatedVisibility(
            visible = rendering,
            enter = fadeIn(tween(60)),
            exit = fadeOut(tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 背景遮罩：随展开进度加深（半透明 + 模糊渐进）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha = scrimAlpha
                            renderEffect = if (useBlur && blurRadiusPx > 0.5f) {
                                androidx.compose.ui.graphics.BlurEffect(
                                    null,
                                    blurRadiusPx,
                                    blurRadiusPx,
                                    androidx.compose.ui.graphics.TileMode.Decal
                                )
                            } else {
                                null
                            }
                        }
                        .background(Color.Black)
                )
                // 展开内容
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                            alpha = (progress.coerceIn(0f, 1f) * 0.5f + 0.5f)
                        }
                        .clip(RoundedCornerShape(cornerRadius))
                ) {
                    content(::requestExit)
                }
            }
        }
    }
}
