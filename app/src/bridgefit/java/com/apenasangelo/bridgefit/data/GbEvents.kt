package com.apenasangelo.bridgefit.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService

/**
 * Gadgetbridge's local broadcasts, as flows.
 */
object GbEvents {
    /**
     * Emits the intent action whenever a device changes state, the device list changes or new data was stored.
     */
    fun changes(context: Context): Flow<String> = broadcasts(
        context,
        GBDevice.ACTION_DEVICE_CHANGED,
        DeviceManager.ACTION_DEVICES_CHANGED,
        GBApplication.ACTION_NEW_DATA,
    ) { it.action.orEmpty() }

    /**
     * Emits each live heart rate sample a device sends while a realtime measurement is running.
     */
    fun realtimeHeartRate(context: Context): Flow<Int> = broadcasts(context, DeviceService.ACTION_REALTIME_SAMPLES) { intent ->
        @Suppress("DEPRECATION")
        val sample = intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE) as? ActivitySample
        sample?.heartRate ?: -1
    }

    private fun <T> broadcasts(context: Context, vararg actions: String, map: (Intent) -> T): Flow<T> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(map(intent))
            }
        }
        val filter = IntentFilter().apply { actions.forEach { addAction(it) } }
        val manager = LocalBroadcastManager.getInstance(context.applicationContext)
        manager.registerReceiver(receiver, filter)
        awaitClose { manager.unregisterReceiver(receiver) }
    }
}
