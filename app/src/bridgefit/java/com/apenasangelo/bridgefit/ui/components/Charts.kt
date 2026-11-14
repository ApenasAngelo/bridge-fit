package com.apenasangelo.bridgefit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.widgets.HypnogramSegment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/** Width reserved on the right of a chart for the y-axis labels. */
private val YAxisWidth = 40.dp

/**
 * A lighter tint of the color for the top of a bar, Apple-style: the gradient is subtle but visible.
 */
fun Color.barTop(): Color = lerp(this, Color.White, 0.28f)

private fun barBrush(color: Color, top: Float, bottom: Float) =
    Brush.verticalGradient(listOf(color.barTop(), color), startY = top, endY = bottom)

/**
 * Rounds [value] up to a "nice" axis maximum (1, 2, 2.5 or 5 times a power of ten).
 */
fun niceMax(value: Float): Float {
    if (value <= 0f) return 1f
    val exponent = 10f.pow(ceil(log10(value)) - 1)
    val fraction = value / exponent
    val nice = when {
        fraction <= 1f -> 1f
        fraction <= 2f -> 2f
        fraction <= 2.5f -> 2.5f
        fraction <= 5f -> 5f
        else -> 10f
    }
    return nice * exponent
}

/**
 * Labels under a chart, spread evenly ("00", "06", "12", "18" for a day).
 */
@Composable
fun XAxis(labels: List<String>, modifier: Modifier = Modifier, reserveYAxis: Boolean = false) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.weight(1f)) {
            labels.forEach {
                Text(it, style = FitType.caption, color = fit.textSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
        if (reserveYAxis) Box(Modifier.width(YAxisWidth))
    }
}

val HourLabels = listOf("00", "06", "12", "18")

/**
 * Dotted verticals at each label slot, solid horizontals at 0, half and the top.
 */
private fun DrawScope.chartGrid(color: Color, verticals: Int, horizontals: Boolean) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 3.dp.toPx()))
    for (i in 0..verticals) {
        val x = size.width * i / verticals
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx(), pathEffect = dash)
    }
    if (horizontals) {
        listOf(0f, 0.5f).forEach { f ->
            drawLine(color, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 0.75.dp.toPx())
        }
    }
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
}

/**
 * The y-axis labels on the right of a chart: top, middle and zero.
 */
@Composable
private fun YAxisLabels(top: Float, height: Dp, format: (Float) -> String) {
    Column(Modifier.width(YAxisWidth).height(height).padding(start = 6.dp)) {
        Text(format(top), style = FitType.caption, color = fit.textSecondary, maxLines = 1)
        Box(Modifier.weight(1f))
        Text(format(top / 2f), style = FitType.caption, color = fit.textSecondary, maxLines = 1)
        Box(Modifier.weight(1f))
        Text(format(0f), style = FitType.caption, color = fit.textSecondary, maxLines = 1)
    }
}

/**
 * Thin rounded bars with a soft vertical gradient, one per bucket. Buckets past [activeCount] are not drawn.
 * [goalLine] draws a dashed line at that value. With [showYAxis], nice y labels are shown on the right.
 */
@Composable
fun BarChart(
    values: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    activeCount: Int = values.size,
    maxValue: Float? = null,
    goalLine: Float? = null,
    colors: List<Color>? = null,
    xLabels: List<String>? = HourLabels,
    showGrid: Boolean = true,
    showYAxis: Boolean = true,
    barFraction: Float = 0.55f,
    yFormat: (Float) -> String = { Fmt.int(it.toInt()) },
) {
    val grid = fit.chartGrid
    val empty = fit.textTertiary.copy(alpha = 0.35f)
    val top = maxValue ?: niceMax(max(values.maxOrNull() ?: 0f, goalLine ?: 0f))
    Column(modifier) {
        Row {
            Canvas(Modifier.weight(1f).height(height)) {
                if (showGrid) chartGrid(grid, xLabels?.size ?: 4, showYAxis)
                val slot = size.width / values.size
                val barWidth = (slot * barFraction).coerceAtLeast(1.5f)
                val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                values.forEachIndexed { i, v ->
                    if (i >= activeCount) return@forEachIndexed
                    val h = (v / top).coerceIn(0f, 1f) * size.height
                    val x = i * slot + (slot - barWidth) / 2f
                    if (v <= 0f) {
                        drawRoundRect(empty, Offset(x, size.height - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()), radius)
                    } else {
                        val barColor = colors?.getOrNull(i) ?: color
                        val y = size.height - h.coerceAtLeast(barWidth)
                        drawRoundRect(barBrush(barColor, y, size.height), Offset(x, y), Size(barWidth, size.height - y), radius)
                    }
                }
                goalLine?.takeIf { it > 0 && it <= top }?.let { goal ->
                    val y = size.height - goal / top * size.height
                    drawLine(
                        color.copy(alpha = 0.85f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    )
                }
            }
            if (showYAxis) YAxisLabels(top, height, yFormat)
        }
        xLabels?.let { XAxis(it, reserveYAxis = showYAxis) }
    }
}

/**
 * Small bars for Summary cards: no axes, no grid.
 */
@Composable
fun MiniBars(values: FloatArray, color: Color, modifier: Modifier = Modifier, activeCount: Int = values.size) {
    BarChart(
        values, color, modifier, height = 44.dp, activeCount = activeCount,
        xLabels = null, showGrid = false, showYAxis = false,
    )
}

/**
 * Hourly ranges (min to max) as gradient capsules, like the Heart Rate chart. NaN buckets are skipped.
 * [line] holds the evenly spaced averages of the day, grouped [perHour] at a time.
 */
@Composable
fun RangeChart(
    line: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
    perHour: Int = 4,
    height: Dp = 180.dp,
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
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (showGuides) chartGrid(grid, 4, horizontals = false)
            if (known.isEmpty()) return@Canvas
            val low = (known.minOf { it.first } - 5f).coerceAtLeast(0f)
            val high = known.maxOf { it.second } + 5f
            val span = (high - low).coerceAtLeast(1f)
            val slot = size.width / hours
            val w = (slot * 0.5f).coerceAtLeast(2f)
            ranges.forEachIndexed { i, r ->
                r ?: return@forEachIndexed
                val yTop = size.height - (r.second - low) / span * size.height
                val yBottom = max(size.height - (r.first - low) / span * size.height, yTop + 0.5f)
                val x = i * slot + slot / 2f
                drawLine(
                    barBrush(color, yTop - w / 2f, yBottom + w / 2f), Offset(x, yTop), Offset(x, yBottom),
                    strokeWidth = w, cap = StrokeCap.Round,
                )
            }
        }
        if (showAxis) XAxis(HourLabels)
    }
}

/**
 * Points over a day, joined by a soft line (temperature, single readings).
 */
@Composable
fun PointChart(
    points: List<Pair<Long, Float>>,
    color: Color,
    dayStart: Long,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    showAxis: Boolean = true,
    showGuides: Boolean = true,
) {
    val grid = fit.chartGrid
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (showGuides) chartGrid(grid, 4, horizontals = false)
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
            drawPath(path, color.copy(alpha = 0.45f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            points.forEach {
                drawCircle(color.barTop(), 3.5.dp.toPx(), toOffset(it))
                drawCircle(color, 2.5.dp.toPx(), toOffset(it))
            }
        }
        if (showAxis) XAxis(HourLabels)
    }
}

/**
 * The night in four lanes (Awake, REM, Core, Deep), Apple-style: rounded blocks with a soft glow, joined by
 * translucent gradient ribbons where the stage changes. Lane numbers follow upstream: 0 awake, 1 REM, 2 core, 3 deep.
 */
@Composable
fun Hypnogram(
    night: List<HypnogramSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    showLabels: Boolean = true,
    laneNames: List<String>? = null,
) {
    if (night.isEmpty()) return
    val colors = listOf(FitColors.sleepAwake, FitColors.sleepRem, FitColors.sleepCore, FitColors.sleepDeep)
    val start = night.first().start
    val end = night.last().end
    val span = (end - start).coerceAtLeast(1L).toFloat()
    val grid = fit.chartGrid
    val labelColor = fit.textSecondary
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    Column(modifier) {
        Box {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                val laneHeight = size.height / 4f
                val block = laneHeight * if (laneNames != null) 0.42f else 0.6f
                val blockOffset = if (laneNames != null) laneHeight * 0.18f else 0f
                val radius = CornerRadius(block * 0.32f, block * 0.32f)
                for (lane in 0..4) {
                    val y = lane * laneHeight
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.75.dp.toPx())
                }
                fun centerY(lane: Int) = lane * laneHeight + laneHeight / 2f + blockOffset
                val ribbon = (block * 0.55f).coerceAtLeast(3.dp.toPx())
                // Ribbons first, so the blocks sit on top of them.
                night.zipWithNext().forEach { (a, b) ->
                    if (a.lane == b.lane) return@forEach
                    val x = (b.start - start) / span * size.width
                    val y1 = centerY(a.lane)
                    val y2 = centerY(b.lane)
                    val top = minOf(y1, y2)
                    val bottom = maxOf(y1, y2)
                    val (cTop, cBottom) = if (y1 < y2) colors[a.lane] to colors[b.lane] else colors[b.lane] to colors[a.lane]
                    drawRoundRect(
                        Brush.verticalGradient(listOf(cTop.copy(alpha = 0.35f), cBottom.copy(alpha = 0.35f)), top, bottom),
                        Offset(x - ribbon / 2f, top),
                        Size(ribbon, bottom - top),
                        CornerRadius(ribbon / 2f, ribbon / 2f),
                    )
                }
                night.forEach { seg ->
                    val x0 = (seg.start - start) / span * size.width
                    val w = ((seg.end - seg.start) / span * size.width).coerceAtLeast(2.dp.toPx())
                    val y = centerY(seg.lane) - block / 2f
                    val color = colors[seg.lane]
                    val glow = 3.dp.toPx()
                    drawRoundRect(
                        color.copy(alpha = 0.22f), Offset(x0 - glow, y - glow), Size(w + glow * 2, block + glow * 2),
                        CornerRadius(radius.x + glow, radius.y + glow),
                    )
                    drawRoundRect(barBrush(color, y, y + block), Offset(x0, y), Size(w, block), radius)
                }
            }
            if (laneNames != null) {
                Column(Modifier.fillMaxWidth().height(height)) {
                    laneNames.forEach { name ->
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            Text(name, style = FitType.caption, color = labelColor, modifier = Modifier.padding(start = 2.dp, top = 2.dp))
                        }
                    }
                }
            }
        }
        if (showLabels) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    formatter.format(Instant.ofEpochSecond(start).atZone(zone)), style = FitType.caption,
                    color = fit.textSecondary, modifier = Modifier.weight(1f),
                )
                Text(
                    formatter.format(Instant.ofEpochSecond(end).atZone(zone)), style = FitType.caption,
                    color = fit.textSecondary, textAlign = TextAlign.End,
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
            drawRoundRect(
                Brush.horizontalGradient(listOf(color.barTop(), color), x, x + w),
                Offset(x, 0f), Size((w - gap).coerceAtLeast(1f), size.height), r,
            )
            x += w
        }
    }
}
