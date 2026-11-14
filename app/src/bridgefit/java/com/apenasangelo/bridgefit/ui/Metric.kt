package com.apenasangelo.bridgefit.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.apenasangelo.bridgefit.ui.theme.FitColors
import nodomain.freeyourgadget.gadgetbridge.R

/**
 * The metrics that have a detail screen. [chart] is the upstream chart id that shows their full history.
 */
enum class Metric(
    val route: String,
    @StringRes val title: Int,
    val icon: ImageVector,
    val color: Color,
    val chart: String?,
    @StringRes val about: Int? = null,
    /** A shorter title for the half-width Summary cards. */
    @StringRes val shortTitle: Int = title,
) {
    ACTIVITY("activity", R.string.bf_activity, Icons.Rounded.LocalFireDepartment, FitColors.move, "activity", shortTitle = R.string.bf_move),
    EXERCISE("exercise", R.string.bf_exercise, Icons.Rounded.Timer, FitColors.exercise, "activity"),
    STEPS("steps", R.string.bf_steps, Icons.AutoMirrored.Rounded.DirectionsWalk, FitColors.steps, "stepsweek"),
    DISTANCE("distance", R.string.bf_distance, Icons.Rounded.Route, FitColors.distance, "stepsweek"),
    HEART_RATE("heart", R.string.bf_heart_rate, Icons.Rounded.Favorite, FitColors.heart, "heartrate", R.string.bf_about_metric_heart),
    SLEEP("sleep", R.string.bf_sleep, Icons.Rounded.Bedtime, FitColors.sleep, "sleep", R.string.bf_about_metric_sleep),
    SPO2("spo2", R.string.bf_spo2, Icons.Rounded.WaterDrop, FitColors.spo2, "spo2", R.string.bf_about_metric_spo2),
    HRV("hrv", R.string.bf_hrv, Icons.Rounded.MonitorHeart, FitColors.hrv, "hrvstatus", R.string.bf_about_metric_hrv, R.string.bf_hrv_short),
    STRESS("stress", R.string.bf_stress, Icons.Rounded.SelfImprovement, FitColors.stress, "stress", R.string.bf_about_metric_stress),
    TEMPERATURE(
        "temperature", R.string.bf_temperature, Icons.Rounded.Thermostat, FitColors.temperature, "temperature",
        R.string.bf_about_metric_temperature, R.string.bf_temperature_short,
    ),
    ;

    companion object {
        fun fromRoute(route: String?): Metric = entries.firstOrNull { it.route == route } ?: ACTIVITY
    }
}
