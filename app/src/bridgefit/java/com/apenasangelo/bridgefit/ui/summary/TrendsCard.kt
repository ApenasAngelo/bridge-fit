package com.apenasangelo.bridgefit.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.Metric
import com.apenasangelo.bridgefit.ui.Trend
import com.apenasangelo.bridgefit.ui.components.CardHeader
import com.apenasangelo.bridgefit.ui.components.FitCard
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R

/**
 * The Trends card of the Summary: for each metric, an arrow (up in the metric color, down in gray) and the recent
 * daily average. Windows: last 30 days against the last 90 (Apple uses 90 against 365).
 */
@Composable
fun TrendsCard(trends: List<Trend>?, onOpen: (Metric) -> Unit) {
    if (trends == null) return
    FitCard {
        CardHeader(
            stringResource(R.string.bf_trends), androidx.compose.material.icons.Icons.Rounded.ArrowUpward,
            fit.textPrimary, showChevron = false,
        )
        Text(stringResource(R.string.bf_trends_subtitle), style = FitType.footnote, color = fit.textSecondary)
        Spacer(Modifier.height(12.dp))
        if (trends.isEmpty()) {
            Text(stringResource(R.string.bf_trends_empty), style = FitType.subheadline, color = fit.textSecondary)
            return@FitCard
        }
        trends.chunked(2).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { TrendItem(it, Modifier.weight(1f)) { onOpen(it.metric) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrendItem(trend: Trend, modifier: Modifier, onClick: () -> Unit) {
    val color = if (trend.isUp) trend.metric.color else fit.textSecondary
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (trend.isUp) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null,
                tint = color, modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(stringResource(trend.metric.shortTitle), style = FitType.subheadline, color = fit.textPrimary, maxLines = 1)
            Text(
                value(trend) + if (trend.metric == Metric.HEART_RATE) "" else stringResource(R.string.bf_per_day),
                style = FitType.subheadlineBold, color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun value(trend: Trend): String = when (trend.metric) {
    Metric.ACTIVITY -> "${Fmt.int(trend.recent.toInt())} ${stringResource(R.string.bf_unit_kcal)}"
    Metric.EXERCISE -> "${Fmt.int(trend.recent.toInt())} ${stringResource(R.string.bf_unit_min)}"
    Metric.STEPS -> Fmt.int(trend.recent.toInt())
    Metric.DISTANCE -> Fmt.distance(trend.recent).let { "${it.first} ${it.second.uppercase()}" }
    Metric.SLEEP -> "${trend.recent.toInt() / 60} h ${trend.recent.toInt() % 60} min"
    Metric.HEART_RATE -> "${trend.recent.toInt()} ${stringResource(R.string.bf_unit_bpm)}"
    else -> Fmt.int(trend.recent.toInt())
}
