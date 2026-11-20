package com.example.detector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class TrackerRisk {
    CONFIRMED_TRACKER_TYPE,
    CLOSE_STRONG_SIGNAL,
    NEARBY_BLE
}

data class BleSignalItem(
    val address: String,
    val displayName: String,
    val trackerLabel: String?,
    val rssi: Int,
    val proximityLabel: String,
    val risk: TrackerRisk,
    val lastSeenMs: Long
)

sealed interface BleScanEvent {
    data class DeviceSeen(val item: BleSignalItem) : BleScanEvent
    data class Error(val message: String) : BleScanEvent
}

class BleTrackerDetector(private val context: Context) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    val isBleSupported: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothManager?.adapter != null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager?.adapter?.isEnabled == true
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun scanBleTrackers(): Flow<BleScanEvent> = callbackFlow {
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter
        if (!isBleSupported || adapter == null) {
            trySend(BleScanEvent.Error("Bluetooth LE is not supported"))
            close()
            return@callbackFlow
        }
        if (!hasScanPermission()) {
            trySend(BleScanEvent.Error("Bluetooth scan permission required"))
            close()
            return@callbackFlow
        }
        if (!adapter.isEnabled) {
            trySend(BleScanEvent.Error("Bluetooth is turned off"))
            close()
            return@callbackFlow
        }

        val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
        if (scanner == null) {
            trySend(BleScanEvent.Error("Bluetooth scanner unavailable"))
            close()
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(BleScanEvent.DeviceSeen(parseScanResult(result)))
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    trySend(BleScanEvent.DeviceSeen(parseScanResult(result)))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                trySend(BleScanEvent.Error("Bluetooth scan failed (code $errorCode)"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, callback)
        } catch (e: SecurityException) {
            trySend(BleScanEvent.Error("Bluetooth scan permission denied"))
            close()
            return@callbackFlow
        } catch (e: IllegalStateException) {
            trySend(BleScanEvent.Error("Bluetooth unavailable; please toggle Bluetooth"))
            close()
            return@callbackFlow
        }

        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (_: SecurityException) {
            } catch (_: IllegalStateException) {
            }
        }
    }

    private fun parseScanResult(result: ScanResult): BleSignalItem {
        val record = result.scanRecord
        // Use ScanRecord.deviceName so BLUETOOTH_CONNECT is not required on Android 12+
        val rawName = record?.deviceName?.trim().orEmpty()
        val displayName = rawName.ifEmpty { "Unknown BLE Device" }
        val rssi = result.rssi
        val address = result.device?.address ?: "UNKNOWN"

        val serviceUuids = record?.serviceUuids.orEmpty().map { it.uuid.toString().lowercase() }
        val appleData = record?.getManufacturerSpecificData(0x004C)

        val trackerTag = identifyTrackerSignature(
            rawName = rawName.lowercase(),
            serviceUuids = serviceUuids,
            appleManufacturerBytes = appleData
        )

        val risk = when {
            trackerTag != null -> TrackerRisk.CONFIRMED_TRACKER_TYPE
            rssi >= -62 -> TrackerRisk.CLOSE_STRONG_SIGNAL
            else -> TrackerRisk.NEARBY_BLE
        }

        val proximityLabel = when {
            rssi >= -55 -> "Very Close (<1m)"
            rssi >= -68 -> "Near (1–3m)"
            rssi >= -82 -> "Medium (3–8m)"
            else -> "Far (>8m)"
        }

        return BleSignalItem(
            address = address,
            displayName = displayName,
            trackerLabel = trackerTag,
            rssi = rssi,
            proximityLabel = proximityLabel,
            risk = risk,
            lastSeenMs = System.currentTimeMillis()
        )
    }

    companion object {
        private const val FIND_MY_UUID = "0000fd6f-0000-1000-8000-00805f9b34fb"
        private const val SMART_TAG_UUID = "0000fd5a-0000-1000-8000-00805f9b34fb"
        private const val TILE_UUID = "0000feed-0000-1000-8000-00805f9b34fb"
        private const val GOOGLE_FIND_UUID = "0000fe2c-0000-1000-8000-00805f9b34fb"
        private const val EDDYSTONE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb"

        fun identifyTrackerSignature(
            rawName: String,
            serviceUuids: List<String>,
            appleManufacturerBytes: ByteArray?
        ): String? {
            if (serviceUuids.any { it == FIND_MY_UUID }) {
                return "Find My / AirTag Beacon"
            }
            if (appleManufacturerBytes != null && appleManufacturerBytes.isNotEmpty()) {
                val typeByte = appleManufacturerBytes[0].toInt() and 0xFF
                if (typeByte == 0x12 || typeByte == 0x07) {
                    return "Apple Find My / AirTag Signal"
                }
            }
            if (serviceUuids.any { it == SMART_TAG_UUID }) {
                return "Samsung SmartTag Tracker"
            }
            if (serviceUuids.any { it == TILE_UUID }) {
                return "Tile Bluetooth Tracker"
            }
            if (serviceUuids.any { it == GOOGLE_FIND_UUID || it == EDDYSTONE_UUID }) {
                return "Tracking / Eddystone Beacon"
            }
            val keywords = listOf("airtag", "smarttag", "tile", "tracker", "gps", "itag", "findmy", "locator")
            if (keywords.any { rawName.contains(it) }) {
                return "Suspected GPS / BLE Tracker"
            }
            return null
        }
    }
}
