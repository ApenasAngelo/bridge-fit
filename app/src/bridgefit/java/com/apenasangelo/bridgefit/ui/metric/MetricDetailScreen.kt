package com.apenasangelo.bridgefit.ui.metric

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.apenasangelo.bridgefit.data.Period
import com.apenasangelo.bridgefit.data.PeriodRepository
import com.apenasangelo.bridgefit.data.PeriodSeries
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apenasangelo.bridgefit.data.DaySnapshot
import com.apenasangelo.bridgefit.data.FitGoals
import com.apenasangelo.bridgefit.data.FitRepository
import com.apenasangelo.bridgefit.data.RingDay
import com.apenasangelo.bridgefit.ui.DurationText
import com.apenasangelo.bridgefit.ui.FitViewModel
import com.apenasangelo.bridgefit.ui.Fmt
import com.apenasangelo.bridgefit.ui.Metric
import com.apenasangelo.bridgefit.ui.Upstream
import com.apenasangelo.bridgefit.ui.components.ActivityRings
import com.apenasangelo.bridgefit.ui.components.BarChart
import com.apenasangelo.bridgefit.ui.components.FitCard
import com.apenasangelo.bridgefit.ui.components.Hypnogram
import com.apenasangelo.bridgefit.ui.components.PointChart
import com.apenasangelo.bridgefit.ui.components.RangeChart
import com.apenasangelo.bridgefit.ui.components.SegmentBar
import com.apenasangelo.bridgefit.ui.components.Stat
import com.apenasangelo.bridgefit.ui.components.ValueText
import com.apenasangelo.bridgefit.ui.components.exerciseRing
import com.apenasangelo.bridgefit.ui.components.moveRing
import com.apenasangelo.bridgefit.ui.components.stepsRing
import com.apenasangelo.bridgefit.ui.summary.NoData
import com.apenasangelo.bridgefit.ui.summary.ratio
import com.apenasangelo.bridgefit.ui.summary.stressZone
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MetricDetailScreen(metric: Metric, vm: FitViewModel, onBack: () -> Unit) {
    val day by vm.day.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    val live by vm.liveHeartRate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var period by rememberSaveable { mutableStateOf(Period.DAY) }
    val dataVersion by vm.dataVersion.collectAsStateWithLifecycle()
    val series by produceState<PeriodSeries?>(null, metric, period, day, dataVersion) {
        value = null
        if (period != Period.DAY) value = PeriodRepository.series(metric, period, day)
    }

    CompositionLocalProvider(LocalHero provides metric.color) { DetailScaffold(
        title = stringResource(metric.title),
        glow = metric.color,
        day = day,
        period = period,
        onPeriod = { period = it },
        onBack = onBack,
        onShiftDay = { steps ->
            when (period) {
                Period.DAY -> vm.shiftDay(steps)
                Period.WEEK -> vm.shiftDay(steps * 7)
                Period.MONTH -> vm.selectDay(day.plusMonths(steps))
                Period.YEAR -> vm.selectDay(day.plusYears(steps))
            }
        },
    ) {
        if (period != Period.DAY) {
            periodContent(metric, series)
            val device = deviceFor(metric)
            if (device != null && metric.chart != null) {
                item {
                    CapsuleButton(stringResource(R.string.bf_open_charts), metric.color) {
                        Upstream.charts(context, device, day, metric.chart, metric.title)
                    }
                }
            }
            return@DetailScaffold
        }
        val snap = snapshot
        if (snap == null || snap.day != day) {
            item {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = fit.textSecondary, strokeWidth = 2.dp)
                }
            }
            return@DetailScaffold
        }
        when (metric) {
            Metric.ACTIVITY -> activity(snap, goals, week, vm::selectDay)
            Metric.EXERCISE -> exercise(snap, goals)
            Metric.STEPS -> steps(snap, goals)
            Metric.DISTANCE -> distance(snap, goals)
            Metric.HEART_RATE -> heartRate(snap, live, vm.canMeasureHeartRate, vm::toggleLiveHeartRate)
            Metric.SLEEP -> sleep(snap, goals)
            Metric.SPO2 -> spo2(snap)
            Metric.HRV -> hrv(snap)
            Metric.STRESS -> stress(snap)
            Metric.TEMPERATURE -> temperature(snap)
        }
        val device = deviceFor(metric)
        if (device != null && metric.chart != null) {
            item {
                CapsuleButton(stringResource(R.string.bf_open_charts), metric.color) {
                    Upstream.charts(context, device, day, metric.chart, metric.title)
                }
            }
        }
        metric.about?.let { about ->
            item { AboutCard(stringResource(R.string.bf_about), stringResource(about)) }
        }
    } }
}

/** The color of the hero value: the metric's own color, as in iOS 26 Fitness. */
private val LocalHero = staticCompositionLocalOf { Color.White }

@Composable
private fun sleepLaneNames() = listOf(
    stringResource(R.string.bf_stage_awake),
    stringResource(R.string.bf_stage_rem),
    stringResource(R.string.bf_stage_core),
    stringResource(R.string.bf_stage_deep),
)

private fun deviceFor(metric: Metric): GBDevice? {
    val devices = FitRepository.devices()
    return devices.firstOrNull { dev ->
        val c = dev.deviceCoordinator
        c.supportsCharts(dev) && when (metric) {
            Metric.ACTIVITY, Metric.EXERCISE, Metric.STEPS, Metric.DISTANCE -> c.supportsActivityTracking(dev)
            Metric.HEART_RATE -> c.supportsHeartRateMeasurement(dev)
            Metric.SLEEP -> c.supportsSleepMeasurement(dev)
            Metric.SPO2 -> c.supportsSpo2(dev)
            Metric.HRV -> c.supportsHrvMeasurement(dev)
            Metric.STRESS -> c.supportsStressMeasurement(dev)
            Metric.TEMPERATURE -> c.supportsTemperatureMeasurement(dev)
        }
    } ?: devices.firstOrNull { it.deviceCoordinator.supportsCharts(it) }
}

private fun LazyListScope.noData() = item {
    FitCard { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { NoData() } }
}

// --------------------------------------------------------------------------------------------------------------
// Activity

private fun LazyListScope.activity(snap: DaySnapshot, goals: FitGoals, week: List<RingDay>, onSelectDay: (LocalDate) -> Unit) {
    val move = snap.activeCalories?.active ?: 0
    val exercise = snap.activeTime?.total?.toInt() ?: 0
    val steps = snap.steps?.total ?: 0
    if (week.isNotEmpty()) {
        item { WeekStrip(week, snap.day, goals, onSelectDay) }
    }
    item {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            ActivityRings(
                listOf(
                    moveRing(ratio(move, goals.activeCalories)),
                    exerciseRing(ratio(exercise, goals.activeMinutes)),
                    stepsRing(ratio(steps, goals.steps)),
                ),
                size = 240.dp,
            )
        }
    }
    snap.activeCalories?.let { data ->
        item {
            RingDetailCard(
                stringResource(R.string.bf_move), move, data.goal, stringResource(R.string.bf_unit_kcal), FitColors.move,
                data.hourly, data.hoursStarted,
            )
        }
    }
    snap.activeTime?.let { data ->
        item {
            RingDetailCard(
                stringResource(R.string.bf_exercise), exercise, data.goalMinutes.toInt(), stringResource(R.string.bf_unit_min),
                FitColors.exercise, data.hourly, data.hoursStarted,
            )
        }
    }
    snap.steps?.let { data ->
        item {
            RingDetailCard(
                stringResource(R.string.bf_steps), steps, data.goal, stringResource(R.string.bf_unit_steps), FitColors.steps,
                data.hourly, data.hoursStarted,
            )
        }
    }
    snap.distance?.let { data ->
        item {
            val (value, unit) = Fmt.distance(data.total)
            StatsCard(
                listOf(
                    { m -> Stat(stringResource(R.string.bf_distance), value, unit.uppercase(), FitColors.distance, m) },
                    { m ->
                        Stat(
                            stringResource(R.string.bf_exercise), Fmt.int(exercise), stringResource(R.string.bf_unit_min),
                            FitColors.exercise, m,
                        )
                    },
                ),
            )
        }
    }
}

@Composable
private fun RingDetailCard(
    title: String,
    value: Int,
    goal: Int,
    unit: String,
    color: Color,
    hourly: FloatArray?,
    hoursStarted: Int,
) {
    FitCard {
        Text(title, style = FitType.headline, color = fit.textPrimary)
        ValueText(
            stringResource(R.string.bf_ring_value, Fmt.int(value), Fmt.int(goal)), unit, color,
            style = FitType.title1, unitColor = color,
        )
        hourly?.let {
            Spacer(Modifier.height(12.dp))
            BarChart(it, color, height = 110.dp, activeCount = hoursStarted)
        }
    }
}

@Composable
private fun WeekStrip(week: List<RingDay>, selected: LocalDate, goals: FitGoals, onSelectDay: (LocalDate) -> Unit) {
    FitCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { day ->
                val isSelected = day.day == selected
                Column(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) fit.cardPressed else Color.Transparent)
                        .clickable { onSelectDay(day.day) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        Fmt.weekdayInitial(day.day), style = FitType.captionBold,
                        color = if (day.day == LocalDate.now()) FitColors.accent else fit.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    ActivityRings(
                        listOf(
                            moveRing(ratio(day.activeCalories, goals.activeCalories)),
                            exerciseRing(ratio(day.activeMinutes, goals.activeMinutes)),
                            stepsRing(ratio(day.steps, goals.steps)),
                        ),
                        size = 36.dp,
                        animate = false,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(day.day.dayOfMonth.toString(), style = FitType.caption, color = fit.textSecondary)
                }
            }
        }
    }
}

private fun LazyListScope.exercise(snap: DaySnapshot, goals: FitGoals) {
    val data = snap.activeTime ?: return noData()
    item {
        Hero(stringResource(R.string.bf_total)) {
            ValueText(
                stringResource(R.string.bf_ring_value, Fmt.int(data.total), Fmt.int(goals.activeMinutes)),
                stringResource(R.string.bf_unit_min), LocalHero.current, style = FitType.heroValue,
            )
        }
    }
    data.hourly?.let { hourly ->
        item { ChartBlock { BarChart(hourly, FitColors.exercise, activeCount = data.hoursStarted) } }
    }
}

// --------------------------------------------------------------------------------------------------------------
// Steps and distance

private fun LazyListScope.steps(snap: DaySnapshot, goals: FitGoals) {
    val data = snap.steps ?: return noData()
    item {
        Hero(stringResource(R.string.bf_total)) {
            ValueText(Fmt.int(data.total), stringResource(R.string.bf_unit_steps), LocalHero.current, style = FitType.heroValue)
        }
    }
    data.hourly?.let { hourly ->
        item {
            ChartBlock(stringResource(R.string.bf_hourly)) {
                BarChart(hourly, Metric.STEPS.color, activeCount = data.hoursStarted)
            }
        }
    }
    item {
        StatsCard(
            listOf(
                { m -> Stat(stringResource(R.string.bf_goal), Fmt.int(goals.steps), null, fit.textPrimary, m) },
                { m ->
                    val (value, unit) = Fmt.distance(data.distanceMeters)
                    Stat(stringResource(R.string.bf_distance), value, unit.uppercase(), FitColors.distance, m)
                },
            ),
        )
    }
}

private fun LazyListScope.distance(snap: DaySnapshot, goals: FitGoals) {
    val data = snap.distance ?: return noData()
    item {
        val (value, unit) = Fmt.distance(data.total)
        Hero(stringResource(R.string.bf_total)) {
            ValueText(value, unit.uppercase(), LocalHero.current, style = FitType.heroValue)
        }
    }
    data.hourly?.let { hourly ->
        item {
            ChartBlock(stringResource(R.string.bf_hourly)) {
                BarChart(hourly, Metric.DISTANCE.color, activeCount = data.hoursStarted)
            }
        }
    }
    item {
        val (goal, goalUnit) = Fmt.distance(goals.distanceMeters.toFloat())
        StatsCard(
            listOf(
                { m -> Stat(stringResource(R.string.bf_goal), goal, goalUnit.uppercase(), fit.textPrimary, m) },
                { m -> Stat(stringResource(R.string.bf_steps), Fmt.int(data.steps), null, FitColors.steps, m) },
            ),
        )
    }
}

// --------------------------------------------------------------------------------------------------------------
// Heart rate

private fun LazyListScope.heartRate(snap: DaySnapshot, live: Int?, canMeasure: Boolean, onToggleLive: () -> Unit) {
    val data = snap.heartRate ?: return noData()
    item {
        val context = LocalContext.current
        val secondary = data.latestSeconds.takeIf { it > 0 }?.let {
            stringResource(R.string.bf_latest_at, Fmt.time(context, it))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (live != null) {
                    Hero(stringResource(R.string.bf_live)) {
                        if (live > 0) {
                            ValueText(live.toString(), stringResource(R.string.bf_unit_bpm), FitColors.heart, style = FitType.heroValue)
                        } else {
                            Text(stringResource(R.string.bf_measuring), style = FitType.title1, color = fit.textSecondary)
                        }
                    }
                } else if (data.minimum > 0) {
                    Hero(stringResource(R.string.bf_range_caption), secondary) {
                        ValueText(
                            stringResource(R.string.bf_range, data.minimum.toString(), data.maximum.toString()),
                            stringResource(R.string.bf_unit_bpm), LocalHero.current, style = FitType.heroValue,
                        )
                    }
                } else {
                    Hero(stringResource(R.string.bf_heart_rate), secondary) { NoData() }
                }
            }
            if (canMeasure || live != null) {
                Button(
                    onClick = onToggleLive,
                    colors = ButtonDefaults.buttonColors(containerColor = if (live != null) fit.control else FitColors.heart),
                    shape = CircleShape,
                ) {
                    Text(
                        stringResource(if (live != null) R.string.bf_stop else R.string.bf_measure_now),
                        style = FitType.subheadlineBold,
                        color = if (live != null) fit.textPrimary else Color.White,
                    )
                }
            }
        }
    }
    data.line?.let { line ->
        item { ChartBlock { RangeChart(line, FitColors.heart, height = 180.dp) } }
    }
    item {
        StatsCard(
            listOf(
                { m -> Stat(stringResource(R.string.bf_latest), orDash(data.heartRate), stringResource(R.string.bf_unit_bpm), FitColors.heart, m) },
                { m -> Stat(stringResource(R.string.bf_resting), orDash(data.resting), stringResource(R.string.bf_unit_bpm), fit.textPrimary, m) },
                { m -> Stat(stringResource(R.string.bf_average), orDash(data.average), stringResource(R.string.bf_unit_bpm), fit.textPrimary, m) },
                { m -> Stat(stringResource(R.string.bf_maximum), orDash(data.maximum), stringResource(R.string.bf_unit_bpm), fit.textPrimary, m) },
            ),
        )
    }
}

private fun orDash(value: Int): String = if (value > 0) value.toString() else "–"

// --------------------------------------------------------------------------------------------------------------
// Sleep

private const val STAGE_LIGHT = 0
private const val STAGE_DEEP = 1
private const val STAGE_REM = 2
private const val STAGE_AWAKE = 3

private fun LazyListScope.sleep(snap: DaySnapshot, goals: FitGoals) {
    val data = snap.sleep ?: return noData()
    item {
        Hero(stringResource(R.string.bf_time_asleep)) {
            if (data.total > 0) DurationText(data.total, LocalHero.current, style = FitType.heroValue) else NoData()
        }
    }
    data.night?.takeIf { it.isNotEmpty() }?.let { night ->
        item { ChartBlock { Hypnogram(night, laneNames = sleepLaneNames()) } }
    }
    data.stages?.let { stages ->
        item {
            FitCard {
                val parts = listOf(
                    Triple(R.string.bf_stage_awake, FitColors.sleepAwake, stages[STAGE_AWAKE]),
                    Triple(R.string.bf_stage_rem, FitColors.sleepRem, stages[STAGE_REM]),
                    Triple(R.string.bf_stage_core, FitColors.sleepCore, stages[STAGE_LIGHT]),
                    Triple(R.string.bf_stage_deep, FitColors.sleepDeep, stages[STAGE_DEEP]),
                ).filter { (label, _, _) ->
                    when (label) {
                        R.string.bf_stage_rem -> data.support.rem
                        R.string.bf_stage_awake -> data.support.awake
                        else -> true
                    }
                }
                SegmentBar(parts.map { it.third.toFloat() to it.second })
                Spacer(Modifier.height(8.dp))
                parts.forEach { (label, color, minutes) ->
                    LegendRow(color, stringResource(label), duration(minutes))
                }
            }
        }
    }
    item {
        StatsCard(
            buildList {
                add { m -> Stat(stringResource(R.string.bf_goal), duration(goals.sleepMinutes.toLong()), null, fit.textPrimary, m) }
                if (data.score >= 0) {
                    add { m -> Stat(stringResource(R.string.bf_sleep_score), data.score.toString(), null, FitColors.sleep, m) }
                }
            },
        )
    }
}

private fun duration(minutes: Long): String = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"

// --------------------------------------------------------------------------------------------------------------
// SpO2, HRV, stress, temperature

private fun LazyListScope.spo2(snap: DaySnapshot) {
    val data = snap.spo2 ?: return noData()
    item {
        Hero(stringResource(R.string.bf_latest)) {
            if (data.spo2 > 0) ValueText(data.spo2.toString(), "%", LocalHero.current, style = FitType.heroValue) else NoData()
        }
    }
    data.hourly?.let { hourly ->
        item {
            val dayStart = snap.day.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            val points = hourly.toList().mapIndexedNotNull { hour, v ->
                if (v.isNaN() || v <= 0f) null else (dayStart + hour * 3600L + 1800L) to v
            }
            ChartBlock(stringResource(R.string.bf_hourly)) { PointChart(points, FitColors.spo2, dayStart) }
        }
    }
    item {
        StatsCard(
            listOf(
                { m -> Stat(stringResource(R.string.bf_latest), orDash(data.spo2), "%", FitColors.spo2, m) },
                { m -> Stat(stringResource(R.string.bf_lowest), orDash(data.lowest), "%", fit.textPrimary, m) },
            ),
        )
    }
}

private fun LazyListScope.hrv(snap: DaySnapshot) {
    val data = snap.hrv ?: return noData()
    item {
        Hero(stringResource(R.string.bf_last_night)) {
            if (data.lastNight > 0) {
                ValueText(data.lastNight.toString(), stringResource(R.string.bf_unit_ms), LocalHero.current, style = FitType.heroValue)
            } else {
                NoData()
            }
        }
    }
    data.nights?.let { nights ->
        item {
            ChartBlock {
                val values = FloatArray(nights.size) { if (nights[it].isNaN()) 0f else nights[it] }
                BarChart(values, FitColors.hrv, xLabels = null)
            }
        }
    }
    item {
        StatsCard(
            buildList {
                add { m -> Stat(stringResource(R.string.bf_weekly_average), orDash(data.weeklyAverage), stringResource(R.string.bf_unit_ms), FitColors.hrv, m) }
                if (data.baselineBalancedLower > 0) {
                    add { m ->
                        Stat(
                            stringResource(R.string.bf_baseline),
                            stringResource(R.string.bf_range, data.baselineBalancedLower.toString(), data.baselineBalancedUpper.toString()),
                            stringResource(R.string.bf_unit_ms), fit.textPrimary, m,
                        )
                    }
                }
            },
        )
    }
}

private val stressLabels = listOf(R.string.bf_stress_relaxed, R.string.bf_stress_mild, R.string.bf_stress_moderate, R.string.bf_stress_high)

private fun LazyListScope.stress(snap: DaySnapshot) {
    val data = snap.stress ?: return noData()
    item {
        Hero(stringResource(R.string.bf_average)) {
            if (data.value > 0) {
                val zone = stressZone(data.value, data.ranges)
                Row(verticalAlignment = Alignment.Bottom) {
                    ValueText(data.value.toString(), null, LocalHero.current, style = FitType.heroValue)
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(stressLabels[zone]), style = FitType.title3, color = FitColors.stressZones[zone], modifier = Modifier.padding(bottom = 6.dp))
                }
            } else {
                NoData()
            }
        }
    }
    data.hourly?.let { hourly ->
        item {
            ChartBlock(stringResource(R.string.bf_hourly)) {
                val colors = hourly.map { v -> FitColors.stressZones[stressZone(v.toInt(), data.ranges)] }
                BarChart(hourly, FitColors.stress, activeCount = data.hoursStarted, maxValue = 100f, colors = colors)
            }
        }
    }
    item {
        FitCard {
            Text(stringResource(R.string.bf_time_in_zones), style = FitType.headline, color = fit.textPrimary)
            Spacer(Modifier.height(10.dp))
            // totalTime is indexed by upstream StressType: 0 unknown, then relaxed, mild, moderate, high (seconds).
            val minutes = (1..4).map { (data.totalTime.getOrNull(it) ?: 0) / 60L }
            SegmentBar(minutes.mapIndexed { i, v -> v.toFloat() to FitColors.stressZones[i] })
            Spacer(Modifier.height(8.dp))
            minutes.forEachIndexed { i, v -> LegendRow(FitColors.stressZones[i], stringResource(stressLabels[i]), duration(v)) }
        }
    }
}

private fun LazyListScope.temperature(snap: DaySnapshot) {
    val data = snap.temperature ?: return noData()
    val celsius = "°C"
    item {
        val context = LocalContext.current
        Hero(stringResource(R.string.bf_latest), data.latestSeconds?.let { stringResource(R.string.bf_latest_at, Fmt.time(context, it)) }) {
            val latest = data.latest
            if (latest != null) ValueText(Fmt.decimal(latest), celsius, LocalHero.current, style = FitType.heroValue) else NoData()
        }
    }
    if (data.samples.isNotEmpty()) {
        item {
            val dayStart = snap.day.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            ChartBlock { PointChart(data.samples, FitColors.temperature, dayStart, height = 170.dp) }
        }
        item {
            StatsCard(
                listOf(
                    { m -> Stat(stringResource(R.string.bf_minimum), Fmt.decimal(data.min ?: 0f), celsius, fit.textPrimary, m) },
                    { m -> Stat(stringResource(R.string.bf_maximum), Fmt.decimal(data.max ?: 0f), celsius, fit.textPrimary, m) },
                    { m -> Stat(stringResource(R.string.bf_average), Fmt.decimal(data.average ?: 0f), celsius, FitColors.temperature, m) },
                ),
            )
        }
    }
}
