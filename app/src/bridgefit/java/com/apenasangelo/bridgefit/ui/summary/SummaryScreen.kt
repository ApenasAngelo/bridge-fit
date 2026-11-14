package com.apenasangelo.bridgefit.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apenasangelo.bridgefit.data.DaySnapshot
import com.apenasangelo.bridgefit.data.FitGoals
import com.apenasangelo.bridgefit.ui.DeviceUi
import com.apenasangelo.bridgefit.ui.DurationText
import com.apenasangelo.bridgefit.ui.FitViewModel
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.Metric
import com.apenasangelo.bridgefit.ui.Upstream
import com.apenasangelo.bridgefit.ui.Routes
import com.apenasangelo.bridgefit.ui.components.ActivityRings
import com.apenasangelo.bridgefit.ui.components.FitCard
import com.apenasangelo.bridgefit.ui.components.Hypnogram
import com.apenasangelo.bridgefit.ui.components.ListRow
import com.apenasangelo.bridgefit.ui.components.MetricCard
import com.apenasangelo.bridgefit.ui.components.MiniBars
import com.apenasangelo.bridgefit.ui.components.RangeChart
import com.apenasangelo.bridgefit.ui.components.SectionTitle
import com.apenasangelo.bridgefit.ui.components.ValueText
import com.apenasangelo.bridgefit.ui.components.exerciseRing
import com.apenasangelo.bridgefit.ui.components.moveRing
import com.apenasangelo.bridgefit.ui.components.stepsRing
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    vm: FitViewModel,
    contentPadding: PaddingValues,
    onOpenMetric: (Metric) -> Unit,
) {
    val day by vm.day.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val trends by vm.trends.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navigator = com.apenasangelo.bridgefit.ui.LocalNavigator.current

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = vm::syncAll,
        modifier = Modifier.fillMaxSize().background(fit.background),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SummaryHeader(
                    day = day,
                    onPrevious = { vm.shiftDay(-1) },
                    onNext = { vm.shiftDay(1) },
                    onProfile = { navigator(Routes.PROFILE) },
                )
            }
            if (devices.isNotEmpty()) {
                item { DeviceStrip(devices) }
            }
            if (devices.isEmpty()) {
                item { NoDevicesCard { Upstream.discovery(context) } }
                return@LazyColumn
            }
            val snap = snapshot
            if (snap == null || snap.day != day) {
                item {
                    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = fit.textSecondary, strokeWidth = 2.dp)
                    }
                }
                return@LazyColumn
            }
            if (snap.hasActivity) {
                item { ActivityCard(snap, goals) { onOpenMetric(Metric.ACTIVITY) } }
            }
            if (snap.steps != null || snap.distance != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        snap.steps?.let { steps ->
                            MetricCard(
                                stringResource(R.string.bf_steps), Metric.STEPS.icon, Metric.STEPS.color,
                                onClick = { onOpenMetric(Metric.STEPS) }, modifier = Modifier.weight(1f),
                            ) {
                                ValueText(Fmt.int(steps.total), null, fit.textPrimary)
                                steps.hourly?.let {
                                    Spacer(Modifier.height(8.dp))
                                    MiniBars(it, Metric.STEPS.color, activeCount = steps.hoursStarted)
                                }
                            }
                        }
                        snap.distance?.let { distance ->
                            val (value, unit) = Fmt.distance(distance.total)
                            MetricCard(
                                stringResource(R.string.bf_distance), Metric.DISTANCE.icon, Metric.DISTANCE.color,
                                onClick = { onOpenMetric(Metric.DISTANCE) }, modifier = Modifier.weight(1f),
                            ) {
                                ValueText(value, unit.uppercase(), fit.textPrimary)
                                distance.hourly?.let {
                                    Spacer(Modifier.height(8.dp))
                                    MiniBars(it, Metric.DISTANCE.color, activeCount = distance.hoursStarted)
                                }
                            }
                        }
                    }
                }
            }
            item {
                FitCard(contentPadding = false) {
                    ListRow(
                        title = stringResource(R.string.bf_workouts),
                        subtitle = stringResource(R.string.bf_workouts_subtitle),
                        icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                        iconTint = Color.Black,
                        iconBackground = FitColors.workouts,
                        onClick = { Upstream.workouts(context) },
                    )
                }
            }
            item { TrendsCard(trends, onOpenMetric) }
            if (hasHealth(snap)) {
                item { SectionTitle(stringResource(R.string.bf_health_metrics)) }
            }
            snap.heartRate?.let { hr ->
                item {
                    MetricCard(
                        stringResource(R.string.bf_heart_rate), Metric.HEART_RATE.icon, Metric.HEART_RATE.color,
                        onClick = { onOpenMetric(Metric.HEART_RATE) },
                        trailing = hr.latestSeconds.takeIf { it > 0 }?.let { Fmt.time(context, it) },
                        chart = if (hr.line != null) {
                            { RangeChart(hr.line!!, Metric.HEART_RATE.color, Modifier.width(150.dp), height = 56.dp, showAxis = false, showGuides = false) }
                        } else {
                            null
                        },
                    ) {
                        if (hr.heartRate > 0) {
                            Text(stringResource(R.string.bf_latest), style = FitType.footnote, color = fit.textSecondary)
                            ValueText(hr.heartRate.toString(), stringResource(R.string.bf_unit_bpm), fit.textPrimary)
                        } else {
                            NoData()
                        }
                        if (hr.minimum > 0) {
                            Text(
                                stringResource(R.string.bf_range, hr.minimum.toString(), hr.maximum.toString()) + " " +
                                    stringResource(R.string.bf_unit_bpm),
                                style = FitType.footnote, color = fit.textSecondary,
                            )
                        }
                    }
                }
            }
            snap.sleep?.let { sleep ->
                item {
                    MetricCard(
                        stringResource(R.string.bf_sleep), Metric.SLEEP.icon, Metric.SLEEP.color,
                        onClick = { onOpenMetric(Metric.SLEEP) },
                        chart = if (!sleep.night.isNullOrEmpty()) {
                            { Hypnogram(sleep.night!!, Modifier.width(150.dp), height = 56.dp, showLabels = false) }
                        } else {
                            null
                        },
                    ) {
                        Text(stringResource(R.string.bf_time_asleep), style = FitType.footnote, color = fit.textSecondary)
                        if (sleep.total > 0) DurationText(sleep.total, fit.textPrimary) else NoData()
                    }
                }
            }
            val pairs = buildList<@Composable (Modifier) -> Unit> {
                snap.spo2?.let { spo2 ->
                    add { m ->
                        SmallMetric(Metric.SPO2, m, { onOpenMetric(Metric.SPO2) }) {
                            if (spo2.spo2 > 0) ValueText(spo2.spo2.toString(), "%", fit.textPrimary) else NoData()
                        }
                    }
                }
                if (snap.hrvSupported) {
                    add { m ->
                        SmallMetric(Metric.HRV, m, { onOpenMetric(Metric.HRV) }) {
                            val hrv = snap.hrv
                            val value = hrv?.lastNight?.takeIf { it > 0 } ?: hrv?.weeklyAverage?.takeIf { it > 0 }
                            if (value != null) ValueText(value.toString(), stringResource(R.string.bf_unit_ms), fit.textPrimary) else NoData()
                        }
                    }
                }
                if (snap.stressSupported) {
                    add { m ->
                        SmallMetric(Metric.STRESS, m, { onOpenMetric(Metric.STRESS) }) {
                            val stress = snap.stress
                            if (stress != null && stress.value > 0) {
                                ValueText(stress.value.toString(), null, fit.textPrimary)
                                Text(stressLabel(stress.value, stress.ranges), style = FitType.footnote, color = fit.textSecondary)
                            } else {
                                NoData()
                            }
                        }
                    }
                }
                snap.temperature?.let { temperature ->
                    add { m ->
                        SmallMetric(Metric.TEMPERATURE, m, { onOpenMetric(Metric.TEMPERATURE) }) {
                            val latest = temperature.latest
                            if (latest != null) ValueText(Fmt.decimal(latest), stringResource(R.string.bf_unit_celsius), fit.textPrimary) else NoData()
                        }
                    }
                }
            }
            pairs.chunked(2).forEach { row ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { it(Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun hasHealth(snap: DaySnapshot) = snap.heartRate != null || snap.sleep != null || snap.spo2 != null ||
    snap.hrvSupported || snap.stressSupported || snap.temperature != null

@Composable
private fun stressLabel(value: Int, ranges: IntArray): String {
    val labels = listOf(R.string.bf_stress_relaxed, R.string.bf_stress_mild, R.string.bf_stress_moderate, R.string.bf_stress_high)
    val index = stressZone(value, ranges)
    return stringResource(labels[index])
}

/**
 * The zone (0 relaxed … 3 high) of [value]. [ranges] holds the lower bound of each zone, as upstream defines it.
 */
fun stressZone(value: Int, ranges: IntArray): Int {
    var zone = 0
    ranges.forEachIndexed { i, lower -> if (value >= lower) zone = i }
    return zone.coerceIn(0, 3)
}

@Composable
private fun SmallMetric(metric: Metric, modifier: Modifier, onClick: () -> Unit, value: @Composable () -> Unit) {
    MetricCard(stringResource(metric.shortTitle), metric.icon, metric.color, onClick = onClick, modifier = modifier.height(118.dp)) {
        Spacer(Modifier.weight(1f))
        value()
    }
}

@Composable
fun NoData() {
    Text(stringResource(R.string.bf_no_data), style = FitType.title3, color = fit.textTertiary)
}

@Composable
private fun SummaryHeader(day: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onProfile: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Fmt.dateCaption(day), style = FitType.footnote.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = fit.textSecondary, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.bf_previous_day), tint = FitColors.accent)
            }
            val canGoForward = day.isBefore(LocalDate.now())
            IconButton(onClick = onNext, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.ChevronRight, stringResource(R.string.bf_next_day),
                    tint = if (canGoForward) FitColors.accent else fit.textTertiary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (day == LocalDate.now()) stringResource(R.string.bf_summary_title) else Fmt.dayTitle(context, day),
                style = FitType.largeTitle, color = fit.textPrimary, modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(fit.control).clickable(onClick = onProfile),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Person, null, tint = fit.textSecondary, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun DeviceStrip(devices: List<DeviceUi>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        devices.take(3).forEach { device ->
            Row(
                Modifier.clip(CircleShape).background(fit.card).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(7.dp).clip(CircleShape).background(
                        when {
                            device.initialized -> FitColors.battery
                            device.connecting || device.connected -> FitColors.temperature
                            else -> fit.textTertiary
                        },
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Text(device.name, style = FitType.footnote, color = fit.textPrimary, maxLines = 1)
                if (device.battery in 0..100) {
                    Spacer(Modifier.width(6.dp))
                    if (device.charging) {
                        Icon(Icons.Rounded.BatteryChargingFull, null, tint = FitColors.battery, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        "${device.battery}%", style = FitType.footnote,
                        color = if (device.battery <= 15) FitColors.batteryLow else fit.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(snap: DaySnapshot, goals: FitGoals, onClick: () -> Unit) {
    val move = snap.activeCalories?.active ?: 0
    val exercise = snap.activeTime?.total?.toInt() ?: 0
    val steps = snap.steps?.total ?: 0
    FitCard(onClick = onClick) {
        com.apenasangelo.bridgefit.ui.components.CardHeader(
            stringResource(R.string.bf_activity), Metric.ACTIVITY.icon, fit.textPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RingLegend(stringResource(R.string.bf_move), move, goals.activeCalories, stringResource(R.string.bf_unit_kcal), FitColors.move)
                RingLegend(stringResource(R.string.bf_exercise), exercise, goals.activeMinutes, stringResource(R.string.bf_unit_min), FitColors.exercise)
                RingLegend(stringResource(R.string.bf_steps), steps, goals.steps, stringResource(R.string.bf_unit_steps), FitColors.steps)
            }
            ActivityRings(
                listOf(
                    moveRing(ratio(move, goals.activeCalories)),
                    exerciseRing(ratio(exercise, goals.activeMinutes)),
                    stepsRing(ratio(steps, goals.steps)),
                ),
                size = 132.dp,
            )
        }
    }
}

fun ratio(value: Int, goal: Int): Float = if (goal > 0) value / goal.toFloat() else 0f

@Composable
private fun RingLegend(label: String, value: Int, goal: Int, unit: String, color: Color) {
    Column {
        Text(label, style = FitType.subheadline, color = fit.textPrimary)
        ValueText(
            stringResource(R.string.bf_ring_value, Fmt.int(value), Fmt.int(goal)), unit, color,
            style = FitType.title2, unitColor = color,
        )
    }
}

@Composable
private fun NoDevicesCard(onPair: () -> Unit) {
    FitCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            ActivityRings(listOf(moveRing(0f), exerciseRing(0f), stepsRing(0f)), size = 110.dp, animate = false)
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Rounded.Watch, null, tint = fit.textSecondary)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.bf_no_devices_title), style = FitType.title3, color = fit.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.bf_no_devices_text), style = FitType.subheadline, color = fit.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPair, colors = ButtonDefaults.buttonColors(containerColor = FitColors.accent)) {
                Text(stringResource(R.string.bf_pair_device), style = FitType.headline, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
