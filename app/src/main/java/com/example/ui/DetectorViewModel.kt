package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.PlayBillingManager
import com.example.billing.SubscriptionPlanOffer
import com.example.billing.SubscriptionUiState
import com.example.detector.BleScanEvent
import com.example.detector.BleSignalItem
import com.example.detector.BleTrackerDetector
import com.example.detector.GnssHardwareDetector
import com.example.detector.GnssSnapshot
import com.example.detector.MagneticLevel
import com.example.detector.MagneticReading
import com.example.detector.MagnetometerDetector
import com.example.detector.TrackerRisk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetectionTab {
    TRACKER_SCAN,
    DEVICE_GPS
}

enum class OverallVerdict {
    IDLE,
    SCANNING,
    SAFE,
    ATTENTION,
    ALERT
}

data class BleUiState(
    val isSupported: Boolean = true,
    val hasPermission: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasScannedOnce: Boolean = false,
    val devices: List<BleSignalItem> = emptyList(),
    val errorMessage: String? = null
)

data class DetectorUiState(
    val activeTab: DetectionTab = DetectionTab.TRACKER_SCAN,
    val isScanning: Boolean = false,
    val remainingSeconds: Int = 0,
    val magnetic: MagneticReading = MagneticReading(),
    val ble: BleUiState = BleUiState(),
    val gnss: GnssSnapshot = GnssSnapshot(),
    val verdict: OverallVerdict = OverallVerdict.IDLE,
    val showInfoDialog: Boolean = false,
    val showSubscriptionSheet: Boolean = false,
    val subscription: SubscriptionUiState = SubscriptionUiState()
)

class DetectorViewModel(application: Application) : AndroidViewModel(application) {
    private val magnetometer = MagnetometerDetector(application)
    private val bleDetector = BleTrackerDetector(application)
    private val gnssDetector = GnssHardwareDetector(application)
    private val billingManager = PlayBillingManager(application)

    private val _uiState = MutableStateFlow(DetectorUiState())
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    private var magnetometerJob: Job? = null
    private var bleScanJob: Job? = null
    private var gnssJob: Job? = null
    private var scanTimerJob: Job? = null

    private val baselineSamples = mutableListOf<Float>()
    private var manualBaseline: Float? = null
    private val discoveredDevices = linkedMapOf<String, BleSignalItem>()

    init {
        billingManager.start()
        viewModelScope.launch {
            billingManager.state.collect { subState ->
                _uiState.update { it.copy(subscription = subState) }
            }
        }
        refreshEnvironment()
        startMagnetometer()
    }

    fun refreshEnvironment() {
        val gnssStatic = gnssDetector.checkStaticCapabilities()
        _uiState.update { state ->
            state.copy(
                magnetic = state.magnetic.copy(supported = magnetometer.isSupported),
                ble = state.ble.copy(
                    isSupported = bleDetector.isBleSupported,
                    hasPermission = bleDetector.hasScanPermission(),
                    isBluetoothEnabled = bleDetector.isBluetoothEnabled()
                ),
                gnss = state.gnss.copy(
                    hasGpsHardware = gnssStatic.hasGpsHardware,
                    isLocationEnabled = gnssStatic.isLocationEnabled,
                    isGpsProviderEnabled = gnssStatic.isGpsProviderEnabled,
                    hasLocationPermission = gnssStatic.hasLocationPermission
                )
            )
        }
        recomputeVerdict()
    }

    fun onResume() {
        refreshEnvironment()
        billingManager.connectAndSync()
    }

    fun selectTab(tab: DetectionTab) {
        if (_uiState.value.activeTab == tab) return
        stopScanning()
        _uiState.update { it.copy(activeTab = tab) }
        refreshEnvironment()
        if (tab == DetectionTab.DEVICE_GPS && gnssDetector.hasFineLocationPermission()) {
            startGnssObservation()
        }
    }

    fun setInfoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showInfoDialog = visible) }
    }

    fun setSubscriptionSheetVisible(visible: Boolean) {
        if (visible) {
            billingManager.clearStatusMessage()
        }
        _uiState.update { it.copy(showSubscriptionSheet = visible) }
    }

    fun purchaseSubscription(activity: Activity, offer: SubscriptionPlanOffer) {
        billingManager.launchPurchase(activity, offer)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun retryStoreSync() {
        billingManager.connectAndSync()
    }

    fun calibrateMagnetometer() {
        val current = _uiState.value.magnetic.microTesla
        if (current > 0f) {
            manualBaseline = current
            _uiState.update { state ->
                val level = MagnetometerDetector.classifyLevel(current, current)
                state.copy(
                    magnetic = state.magnetic.copy(
                        baselineMicroTesla = current,
                        peakMicroTesla = current,
                        deltaMicroTesla = 0f,
                        level = level
                    )
                )
            }
            recomputeVerdict()
        }
    }

    fun startScanning() {
        refreshEnvironment()
        val tab = _uiState.value.activeTab
        val isPro = _uiState.value.subscription.isProActive

        scanTimerJob?.cancel()
        bleScanJob?.cancel()
        gnssJob?.cancel()

        if (tab == DetectionTab.TRACKER_SCAN) {
            discoveredDevices.clear()
            _uiState.update {
                it.copy(
                    isScanning = true,
                    remainingSeconds = if (isPro) 0 else SCAN_DURATION_SECONDS,
                    ble = it.ble.copy(
                        hasScannedOnce = true,
                        devices = emptyList(),
                        errorMessage = null
                    )
                )
            }
            if (bleDetector.isBleSupported && bleDetector.hasScanPermission() && bleDetector.isBluetoothEnabled()) {
                startBleScan()
            }
        } else {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    remainingSeconds = if (isPro) 0 else SCAN_DURATION_SECONDS
                )
            }
            startGnssObservation()
        }

        recomputeVerdict()

        // Pro members scan continuously without a 20s timeout
        if (!isPro) {
            scanTimerJob = viewModelScope.launch {
                for (sec in SCAN_DURATION_SECONDS downTo 1) {
                    _uiState.update { it.copy(remainingSeconds = sec) }
                    delay(1000L)
                }
                stopScanning()
            }
        }
    }

    fun stopScanning() {
        scanTimerJob?.cancel()
        scanTimerJob = null
        bleScanJob?.cancel()
        bleScanJob = null
        if (_uiState.value.activeTab == DetectionTab.TRACKER_SCAN) {
            gnssJob?.cancel()
            gnssJob = null
        }
        _uiState.update { it.copy(isScanning = false, remainingSeconds = 0) }
        recomputeVerdict()
    }

    private fun startMagnetometer() {
        if (!magnetometer.isSupported) {
            _uiState.update { it.copy(magnetic = it.magnetic.copy(supported = false)) }
            return
        }
        magnetometerJob?.cancel()
        magnetometerJob = viewModelScope.launch {
            magnetometer.observeMagneticField().collect { value ->
                val baseline = manualBaseline ?: run {
                    if (baselineSamples.size < 6) {
                        baselineSamples.add(value)
                    }
                    baselineSamples.average().toFloat().coerceAtLeast(25f)
                }
                val level = MagnetometerDetector.classifyLevel(value, baseline)
                _uiState.update { state ->
                    val peak = maxOf(state.magnetic.peakMicroTesla, value)
                    state.copy(
                        magnetic = MagneticReading(
                            supported = true,
                            microTesla = value,
                            baselineMicroTesla = baseline,
                            peakMicroTesla = peak,
                            deltaMicroTesla = value - baseline,
                            level = level
                        )
                    )
                }
                recomputeVerdict()
            }
        }
    }

    private fun startBleScan() {
        bleScanJob?.cancel()
        bleScanJob = viewModelScope.launch {
            bleDetector.scanBleTrackers().collect { event ->
                when (event) {
                    is BleScanEvent.DeviceSeen -> {
                        discoveredDevices[event.item.address] = event.item
                        val sorted = discoveredDevices.values.sortedWith(
                            compareBy<BleSignalItem> { it.risk.ordinal }
                                .thenByDescending { it.rssi }
                        )
                        _uiState.update { state ->
                            state.copy(
                                ble = state.ble.copy(
                                    devices = sorted,
                                    errorMessage = null
                                )
                            )
                        }
                        recomputeVerdict()
                    }
                    is BleScanEvent.Error -> {
                        _uiState.update { state ->
                            state.copy(ble = state.ble.copy(errorMessage = event.message))
                        }
                    }
                }
            }
        }
    }

    private fun startGnssObservation() {
        gnssJob?.cancel()
        gnssJob = viewModelScope.launch {
            gnssDetector.observeGnssSignals().collect { snapshot ->
                _uiState.update { it.copy(gnss = snapshot) }
                recomputeVerdict()
            }
        }
    }

    private fun recomputeVerdict() {
        val state = _uiState.value
        val hasTracker = state.ble.devices.any { it.risk == TrackerRisk.CONFIRMED_TRACKER_TYPE }
        val hasStrongCloseBle = state.ble.devices.any { it.risk == TrackerRisk.CLOSE_STRONG_SIGNAL }
        val magLevel = state.magnetic.level

        val verdict = when {
            state.isScanning && !hasTracker && magLevel != MagneticLevel.STRONG_ANOMALY ->
                OverallVerdict.SCANNING
            hasTracker || magLevel == MagneticLevel.STRONG_ANOMALY ->
                OverallVerdict.ALERT
            hasStrongCloseBle || magLevel == MagneticLevel.ELEVATED ->
                OverallVerdict.ATTENTION
            state.ble.hasScannedOnce || state.magnetic.peakMicroTesla > 0f ->
                OverallVerdict.SAFE
            else ->
                OverallVerdict.IDLE
        }
        _uiState.update { it.copy(verdict = verdict) }
    }

    override fun onCleared() {
        super.onCleared()
        magnetometerJob?.cancel()
        bleScanJob?.cancel()
        gnssJob?.cancel()
        scanTimerJob?.cancel()
        billingManager.release()
    }

    companion object {
        const val SCAN_DURATION_SECONDS = 20
    }
}
