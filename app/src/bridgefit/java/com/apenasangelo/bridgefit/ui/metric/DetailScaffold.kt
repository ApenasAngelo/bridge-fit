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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import com.apenasangelo.bridgefit.ui.components.FitCard
import com.apenasangelo.bridgefit.ui.components.ListRow
import nodomain.freeyourgadget.gadgetbridge.R
import java.time.LocalDate

/**
 * The iOS-style navigation bar of a pushed screen: a back chevron with the previous title, and a centered title.
 */
@Composable
fun NavBar(title: String, backLabel: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(fit.background)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            Row(
                Modifier.align(Alignment.CenterStart).clip(CircleShape).clickable(onClick = onBack)
                    .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.ChevronLeft, null, tint = FitColors.accent, modifier = Modifier.size(30.dp))
                Text(backLabel, style = FitType.body, color = FitColors.accent)
            }
            Text(
                title, style = FitType.headline, color = fit.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 110.dp), maxLines = 1,
            )
        }
    }
}

/**
 * A pushed detail screen: nav bar, a day switcher, then [content] in a scrolling column of cards.
 */
@Composable
fun DetailScaffold(
    title: String,
    backLabel: String,
    day: LocalDate,
    onBack: () -> Unit,
    onShiftDay: (Long) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(fit.background)) {
        NavBar(title, backLabel, onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { DaySwitcher(day, onShiftDay) }
            content()
            item { Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
fun DaySwitcher(day: LocalDate, onShiftDay: (Long) -> Unit) {
    val context = LocalContext.current
    val canGoForward = day.isBefore(LocalDate.now())
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(fit.control).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onShiftDay(-1) }) {
            Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.bf_previous_day), tint = fit.textPrimary)
        }
        Text(
            Fmt.dayTitle(context, day), style = FitType.subheadlineBold, color = fit.textPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onShiftDay(1) }, enabled = canGoForward) {
            Icon(
                Icons.Rounded.ChevronRight, stringResource(R.string.bf_next_day),
                tint = if (canGoForward) fit.textPrimary else fit.textTertiary,
            )
        }
    }
}

/**
 * The big value block at the top of a detail screen: a small caption, the value, and a secondary line.
 */
@Composable
fun Hero(caption: String, secondary: String? = null, value: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(caption.uppercase(), style = FitType.footnote, color = fit.textSecondary)
        value()
        secondary?.let { Text(it, style = FitType.subheadline, color = fit.textSecondary) }
    }
}

/**
 * A card with a title and a chart or any other content.
 */
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
 * A row of statistics in one card, split evenly.
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

@Composable
fun OpenChartsCard(color: Color, onClick: () -> Unit) {
    FitCard(contentPadding = false) {
        ListRow(
            title = stringResource(R.string.bf_open_charts),
            subtitle = stringResource(R.string.bf_open_charts_subtitle),
            icon = Icons.AutoMirrored.Rounded.ShowChart,
            iconTint = Color.White,
            iconBackground = color,
            onClick = onClick,
        )
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
