package com.apenasangelo.bridgefit.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Fmt {
    fun int(value: Number): String = NumberFormat.getIntegerInstance().format(value)

    /** Short axis labels: "950", "2.5k", "20k" (localized: "20 mil" in Portuguese). */
    fun compact(value: Float): String =
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.icu.number.NumberFormatter.withLocale(Locale.getDefault())
                .notation(android.icu.number.Notation.compactShort())
                .format(value.toDouble()).toString()
        } else if (value >= 1000f) {
            decimal(value / 1000f, if (value % 1000f == 0f) 0 else 1) + "k"
        } else {
            int(value.toInt())
        }

    fun decimal(value: Float, digits: Int = 1): String = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(value)

    /** "4,82 km" split into its number and unit, following the user's unit system. */
    fun distance(meters: Float): Pair<String, String> {
        val label = FormatUtils.getFormattedDistanceLabel(meters.toDouble())
        // The label is "4.5km" or "4.5 km" depending on the locale; split at the first letter.
        val cut = label.indexOfFirst { it.isLetter() }
        return if (cut > 0) label.substring(0, cut).trim() to label.substring(cut).trim() else label to ""
    }

    fun time(context: Context, epochSeconds: Long): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()))
    }

    /** "Today", "Yesterday", or a medium date such as "Thursday, 12 Nov". */
    fun dayTitle(context: Context, day: LocalDate): String {
        val today = LocalDate.now()
        return when (day) {
            today -> context.getString(R.string.bf_today)
            today.minusDays(1) -> context.getString(R.string.bf_yesterday)
            else -> DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMM"),
            ).format(day)
        }
    }

    /** The date line above the Summary title, in caps: "SATURDAY, 14 NOV". */
    fun dateCaption(day: LocalDate): String = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"),
    ).format(day).uppercase(Locale.getDefault())

    fun shortDate(day: LocalDate): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(day)

    fun weekdayInitial(day: LocalDate): String =
        DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(day).uppercase(Locale.getDefault())
}

/**
 * A duration as "7 h 32 min", with the numbers in [style] and the units smaller and gray.
 */
@Composable
fun DurationText(
    minutes: Long,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = FitType.value,
) {
    val h = minutes / 60
    val m = minutes % 60
    val unitStyle = SpanStyle(color = fit.textSecondary, fontSize = FitType.unit.fontSize, fontWeight = FitType.unit.fontWeight)
    Text(
        buildAnnotatedString {
            if (h > 0) {
                withStyle(SpanStyle(color = color)) { append(h.toString()) }
                withStyle(unitStyle) { append(" h  ") }
            }
            withStyle(SpanStyle(color = color)) { append(m.toString()) }
            withStyle(unitStyle) { append(" min") }
        },
        style = style,
        modifier = modifier,
        maxLines = 1,
    )
}
