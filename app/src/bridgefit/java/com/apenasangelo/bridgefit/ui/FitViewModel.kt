package com.apenasangelo.bridgefit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apenasangelo.bridgefit.data.DaySnapshot
import com.apenasangelo.bridgefit.data.FitGoals
import com.apenasangelo.bridgefit.data.FitRepository
import com.apenasangelo.bridgefit.data.GbEvents
import com.apenasangelo.bridgefit.data.RingDay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes
import java.time.LocalDate

/**
 * A paired device as the UI shows it. [device] is kept for actions and upstream screens.
 */
data class DeviceUi(
    val device: GBDevice,
    val address: String,
    val name: String,
    val model: String,
    val state: String,
    val connected: Boolean,
    val initialized: Boolean,
    val connecting: Boolean,
    val busy: Boolean,
    val battery: Int,
    val charging: Boolean,
    val firmware: String?,
) {
    companion object {
        fun of(app: Application, device: GBDevice): DeviceUi {
            val batteryState = device.getBatteryState(0)
            return DeviceUi(
                device = device,
                address = device.address,
                name = device.aliasOrName,
                model = device.model ?: device.type.name,
                state = device.getStateString(app),
                connected = device.isConnected,
                initialized = device.isInitialized,
                connecting = device.isConnecting,
                busy = device.isBusy,
                battery = device.getBatteryLevel(0),
                charging = batteryState == nodomain.freeyourgadget.gadgetbridge.model.BatteryState.BATTERY_CHARGING ||
                    batteryState == nodomain.freeyourgadget.gadgetbridge.model.BatteryState.BATTERY_CHARGING_FULL,
                firmware = device.firmwareVersion,
            )
        }
    }
}

@OptIn(FlowPreview::class)
class FitViewModel(app: Application) : AndroidViewModel(app) {
    private val _day = MutableStateFlow(LocalDate.now())
    val day: StateFlow<LocalDate> = _day.asStateFlow()

    private val _snapshot = MutableStateFlow<DaySnapshot?>(null)
    val snapshot: StateFlow<DaySnapshot?> = _snapshot.asStateFlow()

    private val _week = MutableStateFlow<List<RingDay>>(emptyList())
    val week: StateFlow<List<RingDay>> = _week.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceUi>>(emptyList())
    val devices: StateFlow<List<DeviceUi>> = _devices.asStateFlow()

    private val _goals = MutableStateFlow(FitGoals.current())
    val goals: StateFlow<FitGoals> = _goals.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _liveHeartRate = MutableStateFlow<Int?>(null)
    val liveHeartRate: StateFlow<Int?> = _liveHeartRate.asStateFlow()

    private var loadJob: Job? = null
    private var measuringDevice: GBDevice? = null

    init {
        refreshDevices()
        reload()
        val events = GbEvents.changes(app)
        events.onEach { refreshDevices() }.launchIn(viewModelScope)
        // New data arrives in bursts while syncing; reload once it settles.
        events.filter { it == GBApplication.ACTION_NEW_DATA }
            .debounce(1500)
            .onEach { reload() }
            .launchIn(viewModelScope)
        GbEvents.realtimeHeartRate(app)
            .filter { it in 1..250 }
            .onEach { if (measuringDevice != null) _liveHeartRate.value = it }
            .launchIn(viewModelScope)
    }

    fun selectDay(day: LocalDate) {
        val clamped = if (day.isAfter(LocalDate.now())) LocalDate.now() else day
        if (clamped == _day.value) return
        _day.value = clamped
        _snapshot.value = null
        reload()
    }

    fun shiftDay(days: Long) = selectDay(_day.value.plusDays(days))

    /**
     * Reloads the selected day from the database. Call when coming back to the app, as goals may have changed.
     */
    fun reload() {
        _goals.value = FitGoals.current()
        loadJob?.cancel()
        val day = _day.value
        loadJob = viewModelScope.launch {
            _snapshot.value = FitRepository.loadDay(day)
            _week.value = FitRepository.loadRingsHistory(day, 7)
        }
    }

    fun refreshDevices() {
        _devices.value = FitRepository.devices().map { DeviceUi.of(getApplication(), it) }
    }

    /**
     * Pull to refresh: fetches the recorded data from every connected device, and connects the others.
     */
    fun syncAll() {
        _refreshing.value = true
        FitRepository.devices().forEach { device ->
            if (device.isInitialized) {
                GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC)
            } else if (!device.isConnected && !device.isConnecting) {
                GBApplication.deviceService(device).connect()
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _refreshing.value = false
            reload()
        }
    }

    fun sync(device: GBDevice) {
        GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC)
    }

    fun connect(device: GBDevice) = GBApplication.deviceService(device).connect()

    fun disconnect(device: GBDevice) = GBApplication.deviceService(device).disconnect()

    fun findDevice(device: GBDevice, start: Boolean) = GBApplication.deviceService(device).onFindDevice(start)

    /**
     * Starts or stops a live heart rate measurement on the first connected device that supports it.
     */
    fun toggleLiveHeartRate() {
        val current = measuringDevice
        if (current != null) {
            GBApplication.deviceService(current).onEnableRealtimeHeartRateMeasurement(false)
            measuringDevice = null
            _liveHeartRate.value = null
            return
        }
        val device = FitRepository.devices().firstOrNull {
            it.isInitialized && it.deviceCoordinator.supportsRealtimeData(it) &&
                it.deviceCoordinator.supportsHeartRateMeasurement(it)
        } ?: return
        measuringDevice = device
        _liveHeartRate.value = 0
        GBApplication.deviceService(device).onEnableRealtimeHeartRateMeasurement(true)
    }

    val canMeasureHeartRate: Boolean
        get() = FitRepository.devices().any {
            it.isInitialized && it.deviceCoordinator.supportsRealtimeData(it) &&
                it.deviceCoordinator.supportsHeartRateMeasurement(it)
        }

    override fun onCleared() {
        measuringDevice?.let { GBApplication.deviceService(it).onEnableRealtimeHeartRateMeasurement(false) }
        super.onCleared()
    }
}
