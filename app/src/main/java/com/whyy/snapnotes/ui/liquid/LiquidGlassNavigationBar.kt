package com.whyy.snapnotes.ui.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class LiquidGlassNavTab(
    val icon: ImageVector,
    val label: String
)

@Composable
fun LiquidGlassNavigationBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    containerColor: Color,
    accentColor: Color,
    shape: Shape
) {
    val config = LocalLiquidGlassConfig.current
    val rootBackdrop = LocalLiquidGlassBackdrop.current
    val useGlass = config.enabled && rootBackdrop != null && isRenderEffectSupported()

    if (useGlass) {
        LiquidGlassNavigationBarGlass(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            tabs = tabs,
            modifier = modifier,
            backdrop = rootBackdrop,
            containerColor = containerColor,
            accentColor = accentColor,
            shape = shape
        )
    } else {
        LiquidGlassNavigationBarPlain(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            tabs = tabs,
            modifier = modifier,
            containerColor = containerColor,
            accentColor = accentColor,
            shape = shape
        )
    }
}

@Composable
private fun LiquidGlassNavigationBarPlain(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    containerColor: Color,
    accentColor: Color,
    shape: Shape
) {
    val textColor = MiuixTheme.colorScheme.onSurface
    val unselectedColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(containerColor, shape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTabIndex() == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) accentColor else unselectedColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) accentColor else unselectedColor
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNavigationBarGlass(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop,
    containerColor: Color,
    accentColor: Color,
    shape: Shape
) {
    val config = LocalLiquidGlassConfig.current
    val tabsBackdrop = rememberLayerBackdrop()
    val tabCount = tabs.size

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    androidx.compose.ui.geometry.Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                // 点击 Tab 直接切换：按 4 个等宽点击区判定，避免受内边距/总宽舍入影响
                .pointerInput(tabCount, tabWidth, isLtr) {
                    detectTapGestures { position ->
                        val pad = with(density) { 4.dp.toPx() }
                        val contentWidth = tabWidth * tabCount
                        val xInContent = (position.x - pad).coerceIn(0f, contentWidth - 0.5f)
                        val rawIndex = (xInContent / tabWidth).toInt().fastCoerceIn(0, tabCount - 1)
                        val tappedIndex = if (isLtr) rawIndex else tabCount - 1 - rawIndex
                        if (tappedIndex != currentIndex) currentIndex = tappedIndex
                    }
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(config.blurRadiusDp.dp.toPx())
                        lens(
                            config.refractionHeightDp.dp.toPx(),
                            config.refractionAmountDp.dp.toPx(),
                            chromaticAberration = config.chromaticAberration
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor.copy(alpha = 0.5f)) }
                )
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabContents(
                tabs = tabs,
                tabWidth = tabWidth,
                accentColor = accentColor,
                selectedIndex = selectedTabIndex()
            )
        }

        androidx.compose.runtime.CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(config.blurRadiusDp.dp.toPx())
                            lens(
                                config.refractionHeightDp.dp.toPx() * progress,
                                config.refractionAmountDp.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.5f)) }
                    )
                    .height(56f.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabContents(
                    tabs = tabs,
                    tabWidth = tabWidth,
                    accentColor = accentColor,
                    selectedIndex = selectedTabIndex()
                )
            }
        }

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(
                    if (config.interactive) {
                        Modifier
                            .then(interactiveHighlight.gestureModifier)
                            .then(dampedDragAnimation.modifier)
                    } else {
                        Modifier
                    }
                )
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { shape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            Color.Black.copy(alpha = 0.03f * progress)
                        )
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabCount)
        )
    }
}

internal val LocalLiquidBottomTabScale = androidx.compose.runtime.staticCompositionLocalOf<() -> Float> {
    { 1f }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabContents(
    tabs: List<LiquidGlassNavTab>,
    tabWidth: Float,
    accentColor: Color,
    selectedIndex: Int
) {
    val textColor = MiuixTheme.colorScheme.onSurface
    val unselectedColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val tabScale = LocalLiquidBottomTabScale.current()
    tabs.forEachIndexed { index, tab ->
        val selected = selectedIndex == index
        Column(
            modifier = Modifier
                .width(with(LocalDensity.current) { tabWidth.toDp() })
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = if (selected) accentColor else unselectedColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = tabScale
                        scaleY = tabScale
                    }
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = tab.label,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) accentColor else textColor
            )
        }
    }
}
