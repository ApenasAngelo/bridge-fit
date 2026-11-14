package com.apenasangelo.bridgefit.ui.metric

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.data.Period
import com.apenasangelo.bridgefit.data.PeriodSeries
import com.apenasangelo.bridgefit.ui.DurationText
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.Metric
import com.apenasangelo.bridgefit.ui.components.BarChart
import com.apenasangelo.bridgefit.ui.components.RangeBars
import com.apenasangelo.bridgefit.ui.components.ValueText
import com.apenasangelo.bridgefit.ui.summary.NoData
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Metrics drawn as daily low–high ranges instead of bars. */
private val RANGED = setOf(Metric.HEART_RATE, Metric.SPO2, Metric.TEMPERATURE)

/**
 * The W/M/Y content of a detail screen: the daily average over the period and one bar (or range) per day or month.
 */
fun LazyListScope.periodContent(metric: Metric, series: PeriodSeries?) {
    if (series == null) {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = fit.textSecondary, strokeWidth = 2.dp)
            }
        }
        return
    }
    item {
        val ranged = metric in RANGED
        Hero(
            stringResource(if (ranged) R.string.bf_range_caption else R.string.bf_daily_average),
            periodLabel(series),
        ) {
            val color = metric.color
            when {
                series.average == null -> NoData()
                ranged && series.low != null && series.high != null -> ValueText(
                    stringResource(R.string.bf_range, format(metric, series.low), format(metric, series.high)),
                    unit(metric), color, style = FitType.heroValue,
                )
                metric == Metric.SLEEP -> DurationText(series.average.toLong(), color, style = FitType.heroValue)
                else -> {
                    val (value, unit) = formatWithUnit(metric, series.average)
                    ValueText(value, unit, color, style = FitType.heroValue)
                }
            }
        }
    }
    item {
        ChartBlock {
            val labels = xLabels(series)
            if (metric in RANGED) {
                RangeBars(series.bars.map { b -> if (b.low != null && b.high != null) b.low to b.high else null }, metric.color, xLabels = labels)
            } else {
                val values = FloatArray(series.bars.size) { series.bars[it].value ?: 0f }
                val scaled = if (metric == Metric.SLEEP) FloatArray(values.size) { values[it] / 60f } else values
                BarChart(
                    scaled, metric.color, xLabels = labels,
                    barFraction = if (series.period == Period.MONTH) 0.6f else 0.5f,
                    yFormat = { v -> if (metric == Metric.DISTANCE) Fmt.distance(v).first else Fmt.compact(v) },
                )
            }
        }
    }
}

@Composable
private fun periodLabel(series: PeriodSeries): String {
    val locale = Locale.getDefault()
    return when (series.period) {
        Period.WEEK -> {
            val f = DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
            "${f.format(series.from)} – ${f.format(series.to)}"
        }
        Period.MONTH -> DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale)
            .format(series.from).replaceFirstChar { it.titlecase(locale) }
        Period.YEAR -> {
            val f = DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMyyyy"), locale)
            "${f.format(series.from)} – ${f.format(series.to)}"
        }
        Period.DAY -> Fmt.shortDate(series.from)
    }
}

private fun xLabels(series: PeriodSeries): List<String> {
    val locale = Locale.getDefault()
    return when (series.period) {
        Period.WEEK -> series.bars.map { it.start.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) }
        Period.MONTH -> listOf("1", "8", "15", "22", "29")
        Period.YEAR -> series.bars.map { it.start.month.getDisplayName(TextStyle.NARROW, locale) }
        Period.DAY -> emptyList()
    }
}

private fun format(metric: Metric, value: Float): String = when (metric) {
    Metric.TEMPERATURE -> Fmt.decimal(value)
    else -> Fmt.int(value.toInt())
}

@Composable
private fun unit(metric: Metric): String? = when (metric) {
    Metric.HEART_RATE -> stringResource(R.string.bf_unit_bpm)
    Metric.SPO2 -> "%"
    Metric.TEMPERATURE -> stringResource(R.string.bf_unit_celsius)
    else -> null
}

@Composable
private fun formatWithUnit(metric: Metric, value: Float): Pair<String, String?> = when (metric) {
    Metric.STEPS -> Fmt.int(value.toInt()) to stringResource(R.string.bf_unit_steps)
    Metric.ACTIVITY -> Fmt.int(value.toInt()) to stringResource(R.string.bf_unit_kcal)
    Metric.EXERCISE -> Fmt.int(value.toInt()) to stringResource(R.string.bf_unit_min)
    Metric.DISTANCE -> Fmt.distance(value).let { it.first to it.second.uppercase() }
    Metric.HRV -> Fmt.int(value.toInt()) to stringResource(R.string.bf_unit_ms)
    Metric.STRESS -> Fmt.int(value.toInt()) to null
    else -> Fmt.int(value.toInt()) to unit(metric)
}
