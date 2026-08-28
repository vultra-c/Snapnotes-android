package com.whyy.snapnotes.ui.components.glasense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.shapes.Capsule
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme
import com.whyy.snapnotes.ui.components.glasense.material.MaterialRecipes
import com.whyy.snapnotes.ui.components.glasense.material.rememberMaterialRenderEffectOrNull

/**
 * A simple header for a page, displaying a title.
 *
 * @param title The title to be displayed.
 */
@Composable
fun GlasensePageHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = GlasenseTheme.type.largeTitleEmphasized,
        modifier = modifier
            .statusBarsPadding()
            .height(160.dp)
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
            .padding(start = 12.dp, bottom = 16.dp, end = 12.dp)
    )
}

/**
 * 设计图风格页首：顶部两侧圆形玻璃功能按钮，下方超大标题 + 灰色副标题。
 * 按钮组通过 [leading]（左上）/ [trailing]（右上）提供。
 *
 * 整个 header 区域带磨砂玻璃背景：固定于页面顶部时，滚动内容经过标题区
 * 会被模糊；磨砂层底部渐隐，与页面内容柔和过渡。
 * [collapseProgress] 返回 0..1：1 表示列表已上滑，标题随之缩小上移（iOS 大标题收起）。
 */
@Composable
fun GlasenseHeroHeader(
    title: String,
    subtitle: String?,
    backdrop: Backdrop,
    liquidGlass: Boolean,
    modifier: Modifier = Modifier,
    collapseProgress: () -> Float = { 0f },
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val frostedTint = GlasenseTheme.colors.cardBackground
    val collapse = collapseProgress().coerceIn(0f, 1f)

    val frostedModifier = if (liquidGlass) {
        Modifier
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(24f.dp.toPx(), TileMode.Decal)
                },
                onDrawSurface = {
                    drawRect(frostedTint.copy(alpha = 0.35f))
                }
            )
    } else {
        Modifier.background(frostedTint.copy(alpha = 0.9f))
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // 磨砂层独立成层并做底部渐隐 mask，与页面内容柔和过渡（标题文字不受渐隐影响）。
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = 1f - 0.35f * collapse
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            0.7f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .then(frostedModifier)
        )
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp)
        ) {
            // 为顶部按钮行预留空间（按钮 48dp + 间距）。
            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = title,
                style = GlasenseTheme.type.largeTitleEmphasized,
                modifier = Modifier.graphicsLayer {
                    // 上滑时标题缩小上移（iOS 大标题收起），左下角锚点。
                    val s = 1f - 0.34f * collapse
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0f, 1f)
                    translationY = -collapse * 10.dp.toPx()
                }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = GlasenseTheme.type.subHeadline,
                    color = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - collapse
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (leading != null) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leading()
            }
        }
        if (trailing != null) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .graphicsLayer {
                        translationY = -collapse * 6.dp.toPx()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailing()
            }
        }
    }
}

/**
 * 顶部圆形玻璃按钮（48dp 圆形，blur + lens 液态玻璃采样，Cresto 顶栏同款）。
 * 非液态玻璃时降级为纯色圆形。
 */
@Composable
fun GlasenseHeroIconButton(
    painter: Painter,
    contentDescription: String?,
    backdrop: Backdrop,
    liquidGlass: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = GlasenseTheme.colors.primary,
    onClick: () -> Unit
) {
    val materialEffect = rememberMaterialRenderEffectOrNull(MaterialRecipes.appBar())
    val heroSurfaceTint = GlasenseTheme.colors.cardBackground

    Box(
        modifier = modifier
            .size(48.dp)
            .then(
                if (liquidGlass) {
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            shadow = null,
                            innerShadow = null,
                            highlight = {
                                Highlight.Default.copy(
                                    style = HighlightStyle.Default(angle = 90f)
                                )
                            },
                            effects = {
                                materialEffect?.let { effect(it) }
                                blur(8f.dp.toPx(), TileMode.Decal)
                                lens(16f.dp.toPx(), 48f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(heroSurfaceTint.copy(alpha = 0.3f))
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick
                        )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(GlasenseTheme.colors.cardBackground.copy(alpha = 0.9f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick
                        )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
    }
}

/**
 * Cresto 风格的玻璃容器：drawBackdrop 采样背后内容，叠加 blur + lens + 材质 shader +
 * 高光，最后铺一层半透明卡面色。非液态玻璃时降级为纯色卡面。
 *
 * 长条形卡片（列表行、分组容器）统一用它实现 Cresto 搜索框那种玻璃长条质感。
 */
@Composable
fun GlasenseGlassPanel(
    backdrop: Backdrop,
    shape: Shape,
    liquidGlass: Boolean,
    modifier: Modifier = Modifier,
    surfaceColor: Color = GlasenseTheme.colors.cardBackground,
    content: @Composable () -> Unit
) {
    val materialEffect = rememberMaterialRenderEffectOrNull(MaterialRecipes.appBar())

    val panelModifier = if (liquidGlass) {
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                shadow = null,
                innerShadow = null,
                highlight = {
                    Highlight.Default.copy(
                        style = HighlightStyle.Default(angle = 90f)
                    )
                },
                effects = {
                    materialEffect?.let { effect(it) }
                    blur(8f.dp.toPx(), TileMode.Decal)
                    lens(16f.dp.toPx(), 48f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(surfaceColor, alpha = 0.3f)
                }
            )
    } else {
        modifier
            .clip(shape)
            .background(surfaceColor.copy(alpha = 0.9f), shape)
    }

    Box(modifier = panelModifier) {
        content()
    }
}

@Composable
fun GlasensePageHeaderCompact(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = GlasenseTheme.type.largeTitleEmphasized,
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    )
}
