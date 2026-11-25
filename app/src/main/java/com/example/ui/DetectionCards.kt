package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.detector.BleSignalItem
import com.example.detector.GnssSnapshot
import com.example.detector.MagneticLevel
import com.example.detector.MagneticReading
import com.example.detector.SatelliteInfo
import com.example.detector.TrackerRisk
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun MagnetometerCard(
    reading: MagneticReading,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = when (reading.level) {
        MagneticLevel.NORMAL -> MaterialTheme.colorScheme.primary
        MagneticLevel.ELEVATED -> MaterialTheme.colorScheme.tertiary
        MagneticLevel.STRONG_ANOMALY -> MaterialTheme.colorScheme.error
    }
    val levelLabel = when (reading.level) {
        MagneticLevel.NORMAL -> stringResource(R.string.mag_status_normal)
        MagneticLevel.ELEVATED -> stringResource(R.string.mag_status_elevated)
        MagneticLevel.STRONG_ANOMALY -> stringResource(R.string.mag_status_strong)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (reading.level == MagneticLevel.NORMAL) {
                MaterialTheme.colorScheme.outline
            } else {
                levelColor.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Sensors,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mag_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (reading.supported) {
                    StatusChip(label = levelLabel, color = levelColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!reading.supported) {
                Text(
                    text = stringResource(R.string.mag_not_supported),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = String.format(Locale.US, "%.1f μT", reading.microTesla),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = levelColor,
                            modifier = Modifier.testTag("magnetometer_value_text")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.mag_baseline_fmt,
                                reading.baselineMicroTesla,
                                reading.peakMicroTesla
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onCalibrate,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("calibrate_mag_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.action_calibrate),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val progress = (reading.microTesla / 160f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = levelColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.mag_card_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BleSignalsCard(
    bleState: BleUiState,
    isScanning: Boolean,
    onRequestBlePermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackerCount = bleState.devices.count { it.risk == TrackerRisk.CONFIRMED_TRACKER_TYPE }
    val highlightColor = when {
        trackerCount > 0 -> MaterialTheme.colorScheme.error
        bleState.devices.any { it.risk == TrackerRisk.CLOSE_STRONG_SIGNAL } ->
            MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isScanning) {
                            Icons.AutoMirrored.Rounded.BluetoothSearching
                        } else {
                            Icons.Rounded.Bluetooth
                        },
                        contentDescription = null,
                        tint = highlightColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ble_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (bleState.hasScannedOnce && bleState.devices.isNotEmpty()) {
                    StatusChip(
                        label = stringResource(R.string.ble_found_count_fmt, bleState.devices.size),
                        color = highlightColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                !bleState.isSupported -> {
                    Text(
                        text = stringResource(R.string.ble_not_supported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                !bleState.hasPermission -> {
                    Text(
                        text = stringResource(R.string.ble_need_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestBlePermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("grant_ble_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = stringResource(R.string.action_grant_ble))
                    }
                }

                !bleState.isBluetoothEnabled -> {
                    Text(
                        text = stringResource(R.string.ble_bt_disabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onEnableBluetooth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("enable_bluetooth_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(text = stringResource(R.string.action_enable_bt))
                    }
                }

                bleState.errorMessage != null -> {
                    Text(
                        text = bleState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                bleState.devices.isEmpty() -> {
                    val emptyText = when {
                        isScanning -> stringResource(R.string.ble_empty_scanning)
                        bleState.hasScannedOnce -> stringResource(R.string.ble_empty_none)
                        else -> stringResource(R.string.ble_empty_idle)
                    }
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        text = stringResource(
                            R.string.ble_summary_fmt,
                            bleState.devices.size,
                            trackerCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("ble_device_list")
                    ) {
                        bleState.devices.take(15).forEach { item ->
                            BleDeviceRow(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BleDeviceRow(item: BleSignalItem) {
    val badgeColor = when (item.risk) {
        TrackerRisk.CONFIRMED_TRACKER_TYPE -> MaterialTheme.colorScheme.error
        TrackerRisk.CLOSE_STRONG_SIGNAL -> MaterialTheme.colorScheme.tertiary
        TrackerRisk.NEARBY_BLE -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (item.risk != TrackerRisk.NEARBY_BLE) {
            BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.trackerLabel ?: item.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.risk == TrackerRisk.CONFIRMED_TRACKER_TYPE) {
                        badgeColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.address} · ${item.proximityLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(label = "${item.rssi} dBm", color = badgeColor)
        }
    }
}

@Composable
fun DeviceGpsStatusCard(
    gnss: GnssSnapshot,
    onOpenLocationSettings: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hwColor = if (gnss.hasGpsHardware) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (gnss.hasGpsHardware) {
                        Icons.Rounded.GpsFixed
                    } else {
                        Icons.Rounded.GpsOff
                    },
                    contentDescription = null,
                    tint = hwColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.gps_hw_card_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HardwareCheckRow(
                label = if (gnss.hasGpsHardware) {
                    stringResource(R.string.gps_hw_installed)
                } else {
                    stringResource(R.string.gps_hw_missing)
                },
                isOk = gnss.hasGpsHardware
            )

            Spacer(modifier = Modifier.height(8.dp))

            HardwareCheckRow(
                label = if (gnss.isLocationEnabled && gnss.isGpsProviderEnabled) {
                    stringResource(R.string.gps_provider_on)
                } else {
                    stringResource(R.string.gps_provider_off)
                },
                isOk = gnss.isLocationEnabled && gnss.isGpsProviderEnabled
            )

            if (!gnss.isLocationEnabled || !gnss.isGpsProviderEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenLocationSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_location_settings_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = stringResource(R.string.action_open_location_settings))
                }
            }

            if (!gnss.hasLocationPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.gps_need_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestLocationPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_location_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = stringResource(R.string.action_grant_location))
                }
            }
        }
    }
}

@Composable
fun GnssSatellitesCard(
    gnss: GnssSnapshot,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.gps_sat_card_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                StatusChip(
                    label = stringResource(R.string.gps_in_fix_fmt, gnss.usedCount, gnss.satellites.size),
                    color = if (gnss.usedCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    title = stringResource(R.string.gps_metric_visible),
                    value = "${gnss.satellites.size}",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    title = stringResource(R.string.gps_metric_avg_signal),
                    value = if (gnss.averageCn0 > 0f) {
                        String.format(Locale.US, "%.1f dB", gnss.averageCn0)
                    } else {
                        "--"
                    },
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    title = stringResource(R.string.gps_metric_accuracy),
                    value = gnss.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }

            if (gnss.latitude != null && gnss.longitude != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.gps_coordinates_fmt,
                            gnss.latitude,
                            gnss.longitude
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (gnss.satellites.isEmpty()) {
                Text(
                    text = if (isScanning) {
                        stringResource(R.string.gps_searching)
                    } else {
                        stringResource(R.string.gps_no_satellites)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("satellite_list")
                ) {
                    gnss.satellites.take(12).forEach { sat ->
                        SatelliteRow(sat = sat)
                    }
                }
            }
        }
    }
}

@Composable
private fun SatelliteRow(sat: SatelliteInfo) {
    val barColor = when {
        sat.usedInFix -> MaterialTheme.colorScheme.primary
        sat.cn0DbHz >= 25f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${sat.constellation} #${sat.svid}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(126.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LinearProgressIndicator(
            progress = { (sat.cn0DbHz / 50f).coerceIn(0.05f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format(Locale.US, "%.0f dB", sat.cn0DbHz),
            style = MaterialTheme.typography.labelMedium,
            color = barColor,
            modifier = Modifier.width(46.dp)
        )
    }
}

@Composable
private fun HardwareCheckRow(label: String, isOk: Boolean) {
    val color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetricMiniBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
