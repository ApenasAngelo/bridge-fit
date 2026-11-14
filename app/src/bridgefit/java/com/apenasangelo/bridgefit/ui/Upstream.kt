package com.apenasangelo.bridgefit.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.activities.AboutActivity
import nodomain.freeyourgadget.gadgetbridge.activities.AboutUserPreferencesActivity
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2
import nodomain.freeyourgadget.gadgetbridge.activities.DataManagementActivity
import nodomain.freeyourgadget.gadgetbridge.activities.PermissionsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.debug.DebugActivityV2
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutListActivity
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Entry points into the upstream Gadgetbridge screens that Bridge Fit links to instead of re-implementing.
 */
object Upstream {
    private inline fun <reified T> start(context: Context) = context.startActivity(Intent(context, T::class.java))

    fun settings(context: Context) = start<SettingsActivity>(context)
    fun userPreferences(context: Context) = start<AboutUserPreferencesActivity>(context)
    fun dataManagement(context: Context) = start<DataManagementActivity>(context)
    fun permissions(context: Context) = start<PermissionsActivity>(context)
    fun debug(context: Context) = start<DebugActivityV2>(context)
    fun about(context: Context) = start<AboutActivity>(context)
    fun discovery(context: Context) = start<DiscoveryActivityV2>(context)
    /**
     * The upstream workout list needs a device: the first one that records workouts, else the first one.
     */
    fun workouts(context: Context) {
        val devices = nodomain.freeyourgadget.gadgetbridge.GBApplication.app().deviceManager.devices
        val device = devices.firstOrNull { it.deviceCoordinator.supportsRecordedActivities(it) } ?: devices.firstOrNull()
            ?: return
        context.startActivity(Intent(context, WorkoutListActivity::class.java).putExtra(GBDevice.EXTRA_DEVICE, device))
    }

    fun classicInterface(context: Context) = context.startActivity(
        Intent(context, ControlCenterv2::class.java).putExtra(com.apenasangelo.bridgefit.BridgeFitBootstrap.EXTRA_CLASSIC, true),
    )

    fun deviceSettings(context: Context, device: GBDevice) = DeviceSettingsActivity.start(context, device)

    /**
     * Opens the upstream charts on [chart] (an id from DefaultChartsProvider, such as "heartrate") for [device],
     * at [day]. Without a [chart], all charts are shown.
     */
    fun charts(context: Context, device: GBDevice, day: LocalDate, chart: String? = null, @StringRes title: Int? = null) {
        val intent = Intent(context, ActivityChartsActivity::class.java)
        intent.putExtra(GBDevice.EXTRA_DEVICE, device)
        val timestamp = day.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toEpochSecond().toInt()
        intent.putExtra(ActivityChartsActivity.EXTRA_TIMESTAMP, timestamp)
        if (chart != null) {
            intent.putExtra(ActivityChartsActivity.EXTRA_SINGLE_FRAGMENT_NAME, chart)
            title?.let { intent.putExtra(ActivityChartsActivity.EXTRA_ACTIONBAR_TITLE, it) }
        }
        context.startActivity(intent)
    }
}
