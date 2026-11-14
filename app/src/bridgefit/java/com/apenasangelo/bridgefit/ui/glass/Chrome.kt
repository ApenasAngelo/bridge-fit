package com.apenasangelo.bridgefit.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit

/** Height of the floating top bar, below the status bar. */
val TopBarHeight = 56.dp

/** Height of the floating tab bar, and the gap under it. */
val TabBarHeight = 64.dp
val TabBarGap = 10.dp

/**
 * The "scroll edge effect": a band at the top of the screen where content fades into a blurred glass,
 * so it stays legible under the floating buttons and the status bar.
 */
@Composable
fun BoxScope.ScrollEdge(height: Dp) {
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, 0.55f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            }
            .glass(RoundedCornerShape(0.dp), refraction = false, shadow = false),
    )
}

/**
 * The bottom scroll edge, behind the floating tab bar.
 */
@Composable
fun BoxScope.BottomScrollEdge(height: Dp) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.6f to Color.Black, 1f to Color.Black),
                    blendMode = BlendMode.DstIn,
                )
            }
            .glass(RoundedCornerShape(0.dp), refraction = false, shadow = false),
    )
}

/**
 * A circular glass button, as the iOS 26 back and toolbar buttons.
 */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = fit.textPrimary,
) {
    Box(
        modifier
            .size(size)
            .glass(CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
fun GlassBackButton(onClick: () -> Unit, contentDescription: String?) {
    GlassCircleButton(
        Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription, onClick,
        // The iOS chevron glyph sits slightly right of center.
        modifier = Modifier,
    )
}

/**
 * Floating top bar of a pushed screen: a circular back button, and a title that fades in once the large title
 * has scrolled away ([titleAlpha] from 0 to 1).
 */
@Composable
fun BoxScope.FloatingTopBar(
    title: String,
    titleAlpha: Float,
    onBack: () -> Unit,
    backDescription: String?,
    trailing: (@Composable () -> Unit)? = null,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    ScrollEdge(top + TopBarHeight + 18.dp)
    Box(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = top).height(TopBarHeight)
            .padding(horizontal = 16.dp),
    ) {
        Box(Modifier.align(Alignment.CenterStart)) { GlassBackButton(onBack, backDescription) }
        Text(
            title, style = FitType.headline, color = fit.textPrimary.copy(alpha = titleAlpha),
            textAlign = TextAlign.Center, maxLines = 1,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
        )
        trailing?.let { Box(Modifier.align(Alignment.CenterEnd)) { it() } }
    }
}

/** The tab bar's selection color: the Fitness neon green, darker in light mode for contrast. */
val tabAccent: Color
    @Composable get() = if (fit.isDark) Color(0xFFB8F53D) else Color(0xFF2F9A12)

data class TabItem(val label: String, val icon: ImageVector)

/**
 * The floating pill tab bar, on glass. The selected tab sits in its own lighter capsule.
 */
@Composable
fun BoxScope.GlassTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BottomScrollEdge(bottom + TabBarHeight + TabBarGap + 24.dp)
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 20.dp, end = 20.dp, bottom = bottom + TabBarGap)
            .fillMaxWidth()
            .height(TabBarHeight)
            .glass(RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val active = index == selected
            val color by animateColorAsState(if (active) tabAccent else fit.textPrimary, label = "tabColor")
            val pill by animateFloatAsState(if (active) 1f else 0f, label = "tabPill")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(fit.textPrimary.copy(alpha = 0.09f * pill))
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(item.icon, null, tint = color, modifier = Modifier.size(25.dp))
                Text(item.label, style = FitType.captionBold, color = color, maxLines = 1)
            }
        }
    }
}

/** Space that scrolling content must leave at the bottom for the floating tab bar. */
@Composable
fun tabBarClearance(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarHeight + TabBarGap + 16.dp
