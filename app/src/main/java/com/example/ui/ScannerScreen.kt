package com.example.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.detector.TrackerRisk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: DetectorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEnvironment()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val blePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshEnvironment()
        viewModel.startScanning()
    }

    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshEnvironment()
        viewModel.startScanning()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshEnvironment()
        if (viewModel.uiState.value.ble.isBluetoothEnabled) {
            viewModel.startScanning()
        }
    }

    fun requestBlePermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        blePermissionsLauncher.launch(perms)
    }

    fun requestLocationPermissions() {
        locationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun launchBluetoothSettings() {
        try {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
    }

    fun openLocationSettings() {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setInfoDialogVisible(true) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.info_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mode_segmented_row")
                    ) {
                        SegmentedButton(
                            selected = state.activeTab == DetectionTab.TRACKER_SCAN,
                            onClick = { viewModel.selectTab(DetectionTab.TRACKER_SCAN) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tab_tracker_scan")
                        ) {
                            Text(text = stringResource(R.string.tab_tracker_scan))
                        }
                        SegmentedButton(
                            selected = state.activeTab == DetectionTab.DEVICE_GPS,
                            onClick = { viewModel.selectTab(DetectionTab.DEVICE_GPS) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tab_device_gps")
                        ) {
                            Text(text = stringResource(R.string.tab_device_gps))
                        }
                    }
                }

                item {
                    HeroRadarCard(
                        state = state,
                        onPrimaryScanClick = {
                            if (state.isScanning) {
                                viewModel.stopScanning()
                            } else if (state.activeTab == DetectionTab.TRACKER_SCAN &&
                                state.ble.isSupported &&
                                !state.ble.hasPermission
                            ) {
                                requestBlePermissions()
                            } else if (state.activeTab == DetectionTab.DEVICE_GPS &&
                                !state.gnss.hasLocationPermission
                            ) {
                                requestLocationPermissions()
                            } else {
                                viewModel.startScanning()
                            }
                        }
                    )
                }

                if (state.activeTab == DetectionTab.TRACKER_SCAN) {
                    item {
                        MagnetometerCard(
                            reading = state.magnetic,
                            onCalibrate = viewModel::calibrateMagnetometer
                        )
                    }
                    item {
                        BleSignalsCard(
                            bleState = state.ble,
                            isScanning = state.isScanning,
                            onRequestBlePermission = ::requestBlePermissions,
                            onEnableBluetooth = ::launchBluetoothSettings
                        )
                    }
                } else {
                    item {
                        DeviceGpsStatusCard(
                            gnss = state.gnss,
                            onOpenLocationSettings = ::openLocationSettings,
                            onRequestLocationPermission = ::requestLocationPermissions
                        )
                    }
                    item {
                        GnssSatellitesCard(
                            gnss = state.gnss,
                            isScanning = state.isScanning
                        )
                    }
                }
            }
        }
    }

    if (state.showInfoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setInfoDialogVisible(false) },
            title = { Text(text = stringResource(R.string.info_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.info_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.setInfoDialogVisible(false) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun HeroRadarCard(
    state: DetectorUiState,
    onPrimaryScanClick: () -> Unit
) {
    val accentColor = verdictColor(state.verdict, state.activeTab)

    val (verdictTitle, verdictDesc) = if (state.activeTab == DetectionTab.TRACKER_SCAN) {
        when (state.verdict) {
            OverallVerdict.IDLE ->
                stringResource(R.string.verdict_idle_title) to stringResource(R.string.verdict_idle_desc)
            OverallVerdict.SCANNING ->
                stringResource(R.string.verdict_scanning_title) to stringResource(R.string.verdict_scanning_desc)
            OverallVerdict.SAFE ->
                stringResource(R.string.verdict_safe_title) to stringResource(R.string.verdict_safe_desc)
            OverallVerdict.ATTENTION ->
                stringResource(R.string.verdict_attention_title) to stringResource(R.string.verdict_attention_desc)
            OverallVerdict.ALERT ->
                stringResource(R.string.verdict_alert_title) to stringResource(R.string.verdict_alert_desc)
        }
    } else {
        when {
            !state.gnss.hasGpsHardware ->
                stringResource(R.string.gps_hw_missing) to "This device does not report a built-in GPS receiver"
            state.gnss.usedCount > 0 ->
                "Device GPS Active" to "Locked onto ${state.gnss.usedCount} satellites (${state.gnss.satellites.size} visible)"
            state.isScanning ->
                "Checking Device GPS…" to "Reading GNSS satellite signal-to-noise ratio and lock status"
            else ->
                "GPS Hardware Ready" to "Tap Start Scan to verify live GPS / GNSS satellite reception"
        }
    }

    val blips = if (state.activeTab == DetectionTab.TRACKER_SCAN) {
        state.ble.devices.map { item ->
            val ratio = ((-item.rssi - 35f) / 65f).coerceIn(0.18f, 0.90f)
            RadarBlip(
                id = item.address,
                distanceRatio = ratio,
                isAlert = item.risk == TrackerRisk.CONFIRMED_TRACKER_TYPE,
                isWarning = item.risk == TrackerRisk.CLOSE_STRONG_SIGNAL
            )
        }
    } else {
        state.gnss.satellites.map { sat ->
            val ratio = (1f - (sat.cn0DbHz / 50f)).coerceIn(0.20f, 0.88f)
            RadarBlip(
                id = "${sat.constellation}_${sat.svid}",
                distanceRatio = ratio,
                isAlert = false,
                isWarning = !sat.usedInFix
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RadarCanvas(
                isScanning = state.isScanning,
                accentColor = accentColor,
                blips = blips
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = verdictTitle,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("verdict_title_text")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = verdictDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPrimaryScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("scan_toggle_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isScanning) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (state.isScanning) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Icon(
                    imageVector = if (state.isScanning) Icons.Rounded.Stop else Icons.Rounded.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isScanning) {
                        "${stringResource(R.string.action_stop_scan)} (${state.remainingSeconds}s)"
                    } else if (state.ble.hasScannedOnce && state.activeTab == DetectionTab.TRACKER_SCAN) {
                        stringResource(R.string.action_rescan)
                    } else {
                        stringResource(R.string.action_start_scan)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun verdictColor(verdict: OverallVerdict, activeTab: DetectionTab): Color {
    if (activeTab == DetectionTab.DEVICE_GPS) {
        return MaterialTheme.colorScheme.primary
    }
    return when (verdict) {
        OverallVerdict.IDLE -> MaterialTheme.colorScheme.secondary
        OverallVerdict.SCANNING -> MaterialTheme.colorScheme.primary
        OverallVerdict.SAFE -> MaterialTheme.colorScheme.primary
        OverallVerdict.ATTENTION -> MaterialTheme.colorScheme.tertiary
        OverallVerdict.ALERT -> MaterialTheme.colorScheme.error
    }
}
