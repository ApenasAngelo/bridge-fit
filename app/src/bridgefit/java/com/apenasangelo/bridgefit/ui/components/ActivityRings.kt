package com.apenasangelo.bridgefit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.theme.FitColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * One ring: its progress (1 = goal reached, may exceed 1) and its gradient.
 */
data class RingSpec(val progress: Float, val start: Color, val end: Color)

fun moveRing(progress: Float) = RingSpec(progress, FitColors.moveStart, FitColors.moveEnd)
fun exerciseRing(progress: Float) = RingSpec(progress, FitColors.exerciseStart, FitColors.exerciseEnd)
fun stepsRing(progress: Float) = RingSpec(progress, FitColors.stepsStart, FitColors.stepsEnd)

/**
 * Concentric activity rings, outermost first. A ring past its goal keeps winding over itself, and the leading
 * cap casts a small shadow so the overlap stays readable.
 */
@Composable
fun ActivityRings(
    rings: List<RingSpec>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = size / 8.5f,
    gap: Dp = strokeWidth / 8f,
    animate: Boolean = true,
) {
    val anim = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(rings.map { it.progress }) {
        if (animate) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val spacing = gap.toPx()
        rings.forEachIndexed { index, ring ->
            val inset = stroke / 2f + index * (stroke + spacing)
            drawRing(ring, ring.progress.coerceAtLeast(0f) * anim.value, inset, stroke)
        }
    }
}

private fun DrawScope.drawRing(ring: RingSpec, progress: Float, inset: Float, stroke: Float) {
    val diameter = size.minDimension - inset * 2f
    if (diameter <= 0f) return
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val radius = diameter / 2f

    // Track
    drawArc(
        color = ring.start.copy(alpha = 0.22f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(stroke),
    )
    if (progress <= 0.001f) {
        // Apple draws a dot at the top even at zero.
        drawCircle(ring.start, stroke / 2f, Offset(center.x, center.y - radius))
        return
    }

    val sweep = progress * 360f
    if (sweep <= 360f) {
        rotate(-90f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to ring.start,
                    (sweep / 360f).coerceAtLeast(0.01f) to ring.end,
                    1f to ring.end,
                    center = center,
                ),
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        // The round start cap reaches back past 12 o'clock, where the sweep gradient wraps to the end color;
        // repaint it in the start color so there is no seam.
        drawCircle(ring.start, stroke / 2f, Offset(center.x, center.y - radius))
    } else {
        // Full lap in the end color, then the overlapping part on top with a shadow at its head.
        drawArc(ring.end, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        val extra = sweep % 360f
        val headAngle = Math.toRadians((extra - 90f).toDouble())
        val head = Offset(center.x + radius * cos(headAngle).toFloat(), center.y + radius * sin(headAngle).toFloat())
        drawCircle(Color.Black.copy(alpha = 0.35f), stroke / 2f * 1.15f, head + Offset(stroke * 0.12f, stroke * 0.12f))
        rotate(-90f) {
            drawArc(
                brush = Brush.sweepGradient(0f to ring.start, 1f to ring.end, center = center),
                startAngle = 0f,
                sweepAngle = extra,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (extra > 30f) drawCircle(ring.start, stroke / 2f, Offset(center.x, center.y - radius))
        drawCircle(ring.end, stroke / 2f, head)
    }
}
