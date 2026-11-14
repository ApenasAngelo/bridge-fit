package com.apenasangelo.bridgefit.ui.devices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apenasangelo.bridgefit.ui.DeviceUi
import com.apenasangelo.bridgefit.ui.FitViewModel
import com.apenasangelo.bridgefit.ui.Upstream
import com.apenasangelo.bridgefit.ui.components.FitCard
import com.apenasangelo.bridgefit.ui.components.GroupedList
import com.apenasangelo.bridgefit.ui.components.ListRow
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2

@Composable
fun DevicesScreen(vm: FitViewModel, contentPadding: PaddingValues) {
    val devices by vm.devices.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().background(fit.background),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.bf_devices_title), style = FitType.largeTitle, color = fit.textPrimary,
                modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 4.dp),
            )
        }
        items(devices, key = { it.address }) { device -> DeviceCard(device, vm) }
        item {
            FitCard(contentPadding = false) {
                ListRow(
                    title = stringResource(R.string.bf_add_device),
                    icon = Icons.Rounded.Add,
                    iconTint = Color.White,
                    iconBackground = FitColors.accent,
                    onClick = { Upstream.discovery(context) },
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceUi, vm: FitViewModel) {
    val context = LocalContext.current
    var finding by remember { mutableStateOf(false) }
    val coordinator = device.device.deviceCoordinator
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FitCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = FitType.title2, color = fit.textPrimary)
                    Text(device.model, style = FitType.subheadline, color = fit.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape).background(
                                when {
                                    device.initialized -> FitColors.battery
                                    device.connecting || device.connected -> FitColors.temperature
                                    else -> fit.textTertiary
                                },
                            ),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(device.state, style = FitType.footnote, color = fit.textSecondary)
                    }
                    device.firmware?.takeIf { it.isNotBlank() }?.let {
                        Text(stringResource(R.string.bf_firmware, it), style = FitType.footnote, color = fit.textTertiary)
                    }
                }
                if (device.battery in 0..100) {
                    BatteryRing(device.battery, device.charging)
                }
            }
        }
        val rows = buildList<@Composable () -> Unit> {
            if (device.initialized) {
                add {
                    ListRow(
                        stringResource(R.string.bf_sync), icon = Icons.Rounded.Sync, iconBackground = FitColors.accent,
                        subtitle = if (device.busy) stringResource(R.string.bf_syncing) else null,
                        showChevron = false, onClick = { vm.sync(device.device) },
                    )
                }
            }
            if (device.connected || device.connecting) {
                add {
                    ListRow(
                        stringResource(R.string.bf_disconnect), icon = Icons.Rounded.LinkOff, iconBackground = fit.textTertiary,
                        showChevron = false, onClick = { vm.disconnect(device.device) },
                    )
                }
            } else {
                add {
                    ListRow(
                        stringResource(R.string.bf_connect), icon = Icons.Rounded.BluetoothConnected,
                        iconBackground = FitColors.accent, showChevron = false, onClick = { vm.connect(device.device) },
                    )
                }
            }
            if (device.initialized && coordinator.supportsFindDevice(device.device)) {
                add {
                    ListRow(
                        stringResource(if (finding) R.string.bf_stop else R.string.bf_find_device), icon = Icons.Rounded.Search,
                        iconBackground = FitColors.temperature, showChevron = false,
                        onClick = {
                            finding = !finding
                            vm.findDevice(device.device, finding)
                        },
                    )
                }
            }
            add {
                ListRow(
                    stringResource(R.string.bf_device_settings), icon = Icons.Rounded.Settings, iconBackground = Color(0xFF8E8E93),
                    onClick = { Upstream.deviceSettings(context, device.device) },
                )
            }
            add {
                ListRow(
                    stringResource(R.string.bf_device_more),
                    subtitle = stringResource(R.string.bf_settings_classic_subtitle),
                    icon = Icons.Rounded.MoreHoriz, iconBackground = Color(0xFF8E8E93),
                    onClick = { context.startActivity(android.content.Intent(context, ControlCenterv2::class.java)) },
                )
            }
        }
        GroupedList(rows = rows)
    }
}

@Composable
private fun BatteryRing(level: Int, charging: Boolean, size: Dp = 64.dp) {
    val color = when {
        charging -> FitColors.battery
        level <= 15 -> FitColors.batteryLow
        level <= 30 -> FitColors.temperature
        else -> FitColors.battery
    }
    val track = fit.chartGrid
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * level / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$level%", style = FitType.subheadlineBold, color = fit.textPrimary)
            if (charging) Text("⚡", style = FitType.caption, color = color)
        }
    }
}
