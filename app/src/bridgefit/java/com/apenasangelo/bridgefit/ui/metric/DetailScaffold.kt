package com.apenasangelo.bridgefit.ui.metric

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.components.FitCard
import com.apenasangelo.bridgefit.ui.glass.FloatingTopBar
import com.apenasangelo.bridgefit.ui.glass.GlassHost
import com.apenasangelo.bridgefit.ui.glass.TopBarHeight
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import java.time.LocalDate

/**
 * A soft glow of [color] behind the top of a screen, like the colored haze behind iOS 26 Fitness detail screens.
 */
fun Modifier.ambientGlow(color: Color, isDark: Boolean): Modifier = drawBehind {
    val alpha = if (isDark) 0.34f else 0.22f
    val radius = size.width * 0.95f
    drawRect(
        Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            0.55f to color.copy(alpha = alpha * 0.35f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.5f, -radius * 0.2f),
            radius = radius,
        ),
    )
}

/**
 * A pushed screen: a floating glass top bar over a large title, then [content].
 * The small title in the bar fades in once the large title scrolls under it.
 */
@Composable
fun PushedScreen(
    title: String,
    onBack: () -> Unit,
    glow: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val titleAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                with(density) { (listState.firstVisibleItemScrollOffset / 40.dp.toPx()).coerceIn(0f, 1f) }
            }
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isDark = fit.isDark
    GlassHost(
        Modifier.fillMaxSize(),
        content = {
            Box(
                Modifier.fillMaxSize().background(fit.background)
                    .then(if (glow != null) Modifier.ambientGlow(glow, isDark) else Modifier),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + TopBarHeight, bottom = bottom + 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            title, style = FitType.largeTitle, color = fit.textPrimary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                        )
                    }
                    content()
                }
            }
        },
        overlay = {
            FloatingTopBar(title, titleAlpha, onBack, stringResource(R.string.bf_back), trailing)
        },
    )
}

/**
 * A pushed detail screen with a day switcher under the large title.
 */
@Composable
fun DetailScaffold(
    title: String,
    day: LocalDate,
    glow: Color,
    onBack: () -> Unit,
    onShiftDay: (Long) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    PushedScreen(title, onBack, glow) {
        item { DaySwitcher(day, onShiftDay) }
        content()
    }
}

@Composable
fun DaySwitcher(day: LocalDate, onShiftDay: (Long) -> Unit) {
    val context = LocalContext.current
    val canGoForward = day.isBefore(LocalDate.now())
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(fit.control.copy(alpha = 0.7f)).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable { onShiftDay(-1) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.bf_previous_day), tint = fit.textPrimary) }
        Text(
            Fmt.dayTitle(context, day), style = FitType.subheadlineBold, color = fit.textPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(enabled = canGoForward) { onShiftDay(1) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ChevronRight, stringResource(R.string.bf_next_day),
                tint = if (canGoForward) fit.textPrimary else fit.textTertiary,
            )
        }
    }
}

/**
 * The value block of a detail screen, with no card: caption, the value, and a date or secondary line.
 */
@Composable
fun Hero(caption: String, secondary: String? = null, value: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(caption.uppercase(), style = FitType.footnote.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = fit.textSecondary)
        value()
        secondary?.let { Text(it, style = FitType.subheadlineBold, color = fit.textSecondary) }
    }
}

/**
 * A chart straight on the background, with an optional headline. The iOS 26 detail screens drop the card.
 */
@Composable
fun ChartBlock(title: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        title?.let {
            Text(it, style = FitType.headline, color = fit.textPrimary)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/** Kept for screens that still group a chart with other content. */
@Composable
fun ChartCard(title: String? = null, content: @Composable () -> Unit) {
    FitCard {
        title?.let {
            Text(it, style = FitType.headline, color = fit.textPrimary)
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/**
 * Statistics in one card, two per row.
 */
@Composable
fun StatsCard(stats: List<@Composable (Modifier) -> Unit>) {
    FitCard {
        stats.chunked(2).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { it(Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AboutCard(title: String, text: String) {
    FitCard {
        Text(title, style = FitType.headline, color = fit.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(text, style = FitType.subheadline, color = fit.textSecondary)
    }
}

/**
 * A full-width capsule button with colored text, like "View All Steps Metrics".
 */
@Composable
fun CapsuleButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(CircleShape).background(fit.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = FitType.headline, color = color)
    }
}

/**
 * A legend row: a colored dot, a label and a value on the right.
 */
@Composable
fun LegendRow(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(10.dp))
        Text(label, style = FitType.body, color = fit.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = FitType.body, color = fit.textSecondary)
    }
}
