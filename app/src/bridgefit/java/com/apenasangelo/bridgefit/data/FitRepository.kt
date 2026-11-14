/*  Copyright (C) 2026 Bridge Fit contributors

    This file is part of Bridge Fit, a fork of Gadgetbridge.

    Bridge Fit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Bridge Fit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package com.apenasangelo.bridgefit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import nodomain.freeyourgadget.gadgetbridge.widgets.DashboardQuery
import nodomain.freeyourgadget.gadgetbridge.widgets.EmptySharedPreferences
import nodomain.freeyourgadget.gadgetbridge.widgets.GBWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetDataScope
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetInstance
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetVariant
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.ActiveCaloriesWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.ActiveTimeWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.DistanceWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.HeartRateWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.HrvWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.SleepWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.Spo2Widget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.StepsWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.StressData
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.StressWidget
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId

/**
 * Everything the Summary and the detail screens show for one day.
 *
 * Each field is null when no paired device supports that metric. The values come from the upstream dashboard
 * widgets' [GBWidget.loadData], loaded in their largest variant so that the hourly history is included.
 */
data class DaySnapshot(
    val day: LocalDate,
    val steps: StepsWidget.Data?,
    val distance: DistanceWidget.Data?,
    val activeCalories: ActiveCaloriesWidget.Data?,
    val activeTime: ActiveTimeWidget.Data?,
    val heartRate: HeartRateWidget.Data?,
    val sleep: SleepWidget.Data?,
    val spo2: Spo2Widget.Data?,
    val hrv: HrvWidget.Data?,
    val hrvSupported: Boolean,
    val stress: StressData?,
    val stressSupported: Boolean,
    val temperature: TemperatureData?,
) {
    val hasActivity: Boolean get() = steps != null || activeCalories != null || activeTime != null
}

/**
 * Skin temperature samples of one day. [samples] holds epoch seconds and degrees Celsius.
 */
data class TemperatureData(
    val samples: List<Pair<Long, Float>>,
) {
    val latest: Float? get() = samples.lastOrNull()?.second
    val latestSeconds: Long? get() = samples.lastOrNull()?.first
    val min: Float? get() = samples.minOfOrNull { it.second }
    val max: Float? get() = samples.maxOfOrNull { it.second }
    val average: Float? get() = if (samples.isEmpty()) null else samples.map { it.second }.average().toFloat()
}

/**
 * The three ring totals of one day.
 */
data class RingDay(
    val day: LocalDate,
    val activeCalories: Int,
    val activeMinutes: Int,
    val steps: Int,
)

/**
 * The goals the rings are measured against, from the upstream user settings.
 */
data class FitGoals(
    val activeCalories: Int,
    val activeMinutes: Int,
    val steps: Int,
    val distanceMeters: Int,
    val sleepMinutes: Int,
) {
    companion object {
        fun current(): FitGoals {
            val user = ActivityUser()
            return FitGoals(
                activeCalories = user.caloriesBurntGoal,
                activeMinutes = user.activeTimeGoalMinutes,
                steps = user.stepsGoal,
                distanceMeters = user.distanceGoalMeters,
                sleepMinutes = user.sleepDurationGoal,
            )
        }
    }
}

object FitRepository {
    private val LOG = LoggerFactory.getLogger(FitRepository::class.java)

    fun devices(): List<GBDevice> = GBApplication.app().deviceManager.devices

    suspend fun loadDay(day: LocalDate): DaySnapshot = coroutineScope {
        val steps = async { load(StepsWidget, day) }
        val distance = async { load(DistanceWidget, day) }
        val activeCalories = async { load(ActiveCaloriesWidget, day) }
        val activeTime = async { load(ActiveTimeWidget, day) }
        val heartRate = async { load(HeartRateWidget, day) }
        val sleep = async { load(SleepWidget, day) }
        val spo2 = async { load(Spo2Widget, day) }
        val hrv = async { load(HrvWidget, day) }
        val stress = async { load(StressWidget, day) }
        val temperature = async { loadTemperature(day) }
        DaySnapshot(
            day = day,
            steps = steps.await()?.value,
            distance = distance.await()?.value,
            activeCalories = activeCalories.await()?.value,
            activeTime = activeTime.await()?.value,
            heartRate = heartRate.await()?.value,
            sleep = sleep.await()?.value,
            spo2 = spo2.await()?.value,
            hrv = hrv.await()?.value,
            hrvSupported = hrv.await() != null,
            stress = stress.await()?.value,
            stressSupported = stress.await() != null,
            temperature = temperature.await(),
        )
    }

    /**
     * The ring values of each of the [count] days that end on [lastDay], oldest first. Only the totals are loaded.
     */
    suspend fun loadRingsHistory(lastDay: LocalDate, count: Int): List<RingDay> = coroutineScope {
        (count - 1 downTo 0).map { offset ->
            val day = lastDay.minusDays(offset.toLong())
            async {
                RingDay(
                    day = day,
                    activeCalories = load(ActiveCaloriesWidget, day, WidgetVariant.GAUGE)?.value?.active ?: 0,
                    activeMinutes = load(ActiveTimeWidget, day, WidgetVariant.GAUGE)?.value?.total?.toInt() ?: 0,
                    steps = load(StepsWidget, day, WidgetVariant.GAUGE)?.value?.total ?: 0,
                )
            }
        }.map { it.await() }
    }

    /**
     * A loaded value; the wrapper tells "no supported device" (null wrapper) apart from "no data" (null value).
     */
    private class Loaded<D>(val value: D)

    private suspend fun <D> load(
        widget: GBWidget<D>,
        day: LocalDate,
        variant: WidgetVariant = WidgetVariant.METRIC_LARGE,
    ): Loaded<D>? = withContext(Dispatchers.IO) {
        val config = WidgetConfig(
            instance = WidgetInstance("bridgefit-${widget.id}", widget.id),
            prefs = Prefs(EmptySharedPreferences),
            dashboardShowAllDevices = true,
            dashboardDeviceList = emptySet(),
            variant = variant,
        )
        val devices = config.resolveDevices(widget)
        if (devices.isEmpty()) {
            return@withContext null
        }
        try {
            Loaded(widget.loadData(WidgetDataScope(DashboardQuery.forDay(day), devices), config))
        } catch (e: Exception) {
            LOG.error("Could not load {} for {}", widget.id, day, e)
            null
        }
    }

    private suspend fun loadTemperature(day: LocalDate): TemperatureData? = withContext(Dispatchers.IO) {
        val devices = devices().filter { it.deviceCoordinator.supportsTemperatureMeasurement(it) }
        if (devices.isEmpty()) {
            return@withContext null
        }
        val zone = ZoneId.systemDefault()
        val from = day.atStartOfDay(zone).toEpochSecond() * 1000L
        val to = day.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L - 1
        try {
            GBApplication.acquireDbReadOnly().use { db ->
                val samples = devices.flatMap { dev ->
                    dev.deviceCoordinator.getTemperatureSampleProvider(dev, db.daoSession)
                        ?.getAllSamples(from, to)
                        .orEmpty()
                        .filter { it.temperature > 0f }
                        .map { it.timestamp / 1000L to it.temperature }
                }.sortedBy { it.first }
                TemperatureData(samples)
            }
        } catch (e: Exception) {
            LOG.error("Could not load temperature for {}", day, e)
            null
        }
    }
}
