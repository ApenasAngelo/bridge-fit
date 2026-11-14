package com.apenasangelo.bridgefit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.widgets.HypnogramSegment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * Hour labels under a 24-hour chart, in the Apple style: 00, 06, 12, 18.
 */
@Composable
fun HourAxis(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp)) {
        listOf("00", "06", "12", "18").forEach {
            Text(it, style = FitType.caption, color = fit.textSecondary, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Vertical dotted guides at 00, 06, 12 and 18 h, and a solid baseline.
 */
private fun DrawScope.hourGuides(color: Color, columns: Int) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))
    for (i in 0 until 4) {
        val x = size.width * i * (columns / 4) / columns
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx(), pathEffect = dash)
    }
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
}

/**
 * Thin rounded bars, one per bucket (usually hours). Buckets past [activeCount] are not drawn.
 * [highlight] tints the bars, and [goalLine] draws a dashed line at that value when it is in range.
 */
@Composable
fun BarChart(
    values: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    activeCount: Int = values.size,
    maxValue: Float? = null,
    goalLine: Float? = null,
    colors: List<Color>? = null,
    showGuides: Boolean = true,
    showAxis: Boolean = true,
) {
    val grid = fit.chartGrid
    val empty = fit.textTertiary.copy(alpha = 0.35f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (showGuides) hourGuides(grid, values.size)
            val top = max(maxValue ?: values.maxOrNull() ?: 0f, goalLine ?: 0f).coerceAtLeast(1f)
            val slot = size.width / values.size
            val barWidth = (slot * 0.55f).coerceAtLeast(1.5f)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            values.forEachIndexed { i, v ->
                if (i >= activeCount) return@forEachIndexed
                val h = (v / top) * size.height
                val x = i * slot + (slot - barWidth) / 2f
                if (v <= 0f) {
                    drawRoundRect(empty, Offset(x, size.height - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()), radius)
                } else {
                    drawRoundRect(
                        colors?.getOrNull(i) ?: color,
                        Offset(x, size.height - h),
                        Size(barWidth, h.coerceAtLeast(barWidth)),
                        radius,
                    )
                }
            }
            goalLine?.takeIf { it > 0 && it <= top }?.let { goal ->
                val y = size.height - goal / top * size.height
                drawLine(
                    color.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
            }
        }
        if (showAxis) HourAxis()
    }
}

/**
 * Small bars for Summary cards: no axis, no guides.
 */
@Composable
fun MiniBars(values: FloatArray, color: Color, modifier: Modifier = Modifier, activeCount: Int = values.size) {
    BarChart(
        values, color, modifier, height = 44.dp, activeCount = activeCount,
        showGuides = false, showAxis = false,
    )
}

/**
 * Hourly ranges (min to max) as capsules, like the Heart Rate chart. NaN buckets are skipped.
 * [line] holds the evenly spaced averages of the day, grouped [perHour] at a time.
 */
@Composable
fun RangeChart(
    line: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
    perHour: Int = 4,
    height: Dp = 160.dp,
    showAxis: Boolean = true,
    showGuides: Boolean = true,
) {
    val hours = line.size / perHour
    val ranges = (0 until hours).map { h ->
        val bucket = (0 until perHour).map { line[h * perHour + it] }.filter { !it.isNaN() && it > 0f }
        if (bucket.isEmpty()) null else bucket.min() to bucket.max()
    }
    val known = ranges.filterNotNull()
    val grid = fit.chartGrid
    val label = fit.textSecondary
    Column(modifier) {
        Box {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                if (showGuides) hourGuides(grid, hours)
                if (known.isEmpty()) return@Canvas
                val low = (known.minOf { it.first } - 5f).coerceAtLeast(0f)
                val high = known.maxOf { it.second } + 5f
                val span = (high - low).coerceAtLeast(1f)
                val slot = size.width / hours
                val w = (slot * 0.5f).coerceAtLeast(2f)
                ranges.forEachIndexed { i, r ->
                    r ?: return@forEachIndexed
                    val yTop = size.height - (r.second - low) / span * size.height
                    val yBottom = size.height - (r.first - low) / span * size.height
                    val x = i * slot + slot / 2f
                    drawLine(
                        color, Offset(x, yTop), Offset(x, max(yBottom, yTop + 0.5f)),
                        strokeWidth = w, cap = StrokeCap.Round,
                    )
                }
            }
            if (known.isNotEmpty() && showGuides) {
                Column(Modifier.fillMaxWidth().height(height)) {
                    Text(
                        known.maxOf { it.second }.toInt().toString(), style = FitType.caption, color = label,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                    )
                    Box(Modifier.weight(1f))
                    Text(
                        known.minOf { it.first }.toInt().toString(), style = FitType.caption, color = label,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                    )
                }
            }
        }
        if (showAxis) HourAxis()
    }
}

/**
 * Points over a time span, joined by a thin line (temperature, single readings).
 */
@Composable
fun PointChart(
    points: List<Pair<Long, Float>>,
    color: Color,
    dayStart: Long,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    showAxis: Boolean = true,
    showGuides: Boolean = true,
) {
    val grid = fit.chartGrid
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (showGuides) hourGuides(grid, 24)
            if (points.isEmpty()) return@Canvas
            val low = points.minOf { it.second } - 0.5f
            val high = points.maxOf { it.second } + 0.5f
            val span = (high - low).coerceAtLeast(0.1f)
            val toOffset = { p: Pair<Long, Float> ->
                Offset(
                    ((p.first - dayStart).toFloat() / 86400f).coerceIn(0f, 1f) * size.width,
                    size.height - (p.second - low) / span * size.height,
                )
            }
            val path = Path()
            points.forEachIndexed { i, p ->
                val o = toOffset(p)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, color.copy(alpha = 0.5f), style = Stroke(1.5.dp.toPx()))
            points.forEach { drawCircle(color, 2.5.dp.toPx(), toOffset(it)) }
        }
        if (showAxis) HourAxis()
    }
}

/**
 * The night as four lanes (Awake, REM, Core, Deep), with the time of falling asleep and waking up below.
 * Lane numbers follow upstream: 0 awake, 1 REM, 2 light (core), 3 deep.
 */
@Composable
fun Hypnogram(
    night: List<HypnogramSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    showLabels: Boolean = true,
) {
    if (night.isEmpty()) return
    val colors = listOf(FitColors.sleepAwake, FitColors.sleepRem, FitColors.sleepCore, FitColors.sleepDeep)
    val start = night.first().start
    val end = night.last().end
    val span = (end - start).coerceAtLeast(1L).toFloat()
    val grid = fit.chartGrid
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val laneHeight = size.height / 4f
            val barHeight = laneHeight * 0.62f
            for (lane in 0..3) {
                val y = lane * laneHeight + laneHeight / 2f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            var previous: HypnogramSegment? = null
            night.forEach { seg ->
                val x0 = (seg.start - start) / span * size.width
                val x1 = (seg.end - start) / span * size.width
                val yCenter = seg.lane * laneHeight + laneHeight / 2f
                previous?.let { prev ->
                    if (prev.lane != seg.lane) {
                        val yPrev = prev.lane * laneHeight + laneHeight / 2f
                        drawLine(
                            colors[seg.lane].copy(alpha = 0.45f), Offset(x0, yPrev), Offset(x0, yCenter),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                drawRoundRect(
                    colors[seg.lane],
                    Offset(x0, yCenter - barHeight / 2f),
                    Size((x1 - x0).coerceAtLeast(2.dp.toPx()), barHeight),
                    CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
                previous = seg
            }
        }
        if (showLabels) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    formatter.format(Instant.ofEpochSecond(start).atZone(zone)), style = FitType.caption,
                    color = fit.textSecondary, modifier = Modifier.weight(1f),
                )
                Text(
                    formatter.format(Instant.ofEpochSecond(end).atZone(zone)), style = FitType.caption,
                    color = fit.textSecondary,
                )
            }
        }
    }
}

/**
 * A horizontal bar split into proportional colored parts (sleep stages, stress levels).
 */
@Composable
fun SegmentBar(parts: List<Pair<Float, Color>>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val track = fit.chartGrid
    Canvas(modifier.fillMaxWidth().height(height)) {
        val total = parts.sumOf { it.first.toDouble() }.toFloat()
        val r = CornerRadius(size.height / 2f, size.height / 2f)
        if (total <= 0f) {
            drawRoundRect(track, cornerRadius = r)
            return@Canvas
        }
        var x = 0f
        val gap = 2.dp.toPx()
        parts.filter { it.first > 0f }.forEach { (value, color) ->
            val w = value / total * size.width
            drawRoundRect(color, Offset(x, 0f), Size((w - gap).coerceAtLeast(1f), size.height), r)
            x += w
        }
    }
}
