package com.example

import android.location.GnssStatus
import com.example.billing.PlanPeriod
import com.example.billing.PlayBillingManager
import com.example.detector.BleTrackerDetector
import com.example.detector.GnssHardwareDetector
import com.example.detector.MagneticLevel
import com.example.detector.MagnetometerDetector
import com.example.detector.TrackerSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `magnetometer classifies normal elevated and strong anomaly`() {
        assertEquals(MagneticLevel.NORMAL, MagnetometerDetector.classifyLevel(48f, 45f))
        assertEquals(MagneticLevel.ELEVATED, MagnetometerDetector.classifyLevel(85f, 45f))
        assertEquals(MagneticLevel.STRONG_ANOMALY, MagnetometerDetector.classifyLevel(135f, 45f))
    }

    @Test
    fun `ble tracker signature identifies findmy and smarttag beacons`() {
        val findMy = BleTrackerDetector.identifyTrackerSignature(
            rawName = "",
            serviceUuids = listOf("0000fd6f-0000-1000-8000-00805f9b34fb"),
            appleManufacturerBytes = null
        )
        assertEquals(TrackerSignature.FIND_MY, findMy)

        val appleFind = BleTrackerDetector.identifyTrackerSignature(
            rawName = "",
            serviceUuids = emptyList(),
            appleManufacturerBytes = byteArrayOf(0x12, 0x19, 0x00)
        )
        assertEquals(TrackerSignature.APPLE_FIND_NETWORK, appleFind)

        val benign = BleTrackerDetector.identifyTrackerSignature(
            rawName = "LivingRoom Speaker",
            serviceUuids = listOf("0000180f-0000-1000-8000-00805f9b34fb"),
            appleManufacturerBytes = null
        )
        assertNull(benign)
    }

    @Test
    fun `gnss constellation name maps common systems`() {
        assertEquals("BeiDou", GnssHardwareDetector.constellationName(GnssStatus.CONSTELLATION_BEIDOU))
        assertEquals("GPS", GnssHardwareDetector.constellationName(GnssStatus.CONSTELLATION_GPS))
    }

    @Test
    fun `billing period classifier maps monthly and annual plans`() {
        assertEquals(
            PlanPeriod.MONTHLY,
            PlayBillingManager.classifyPeriod("gps_scanner_pro", "monthly", "P1M")
        )
        assertEquals(
            PlanPeriod.ANNUAL,
            PlayBillingManager.classifyPeriod("gps_scanner_pro", "annual", "P1Y")
        )
        assertEquals(R.string.trial_7d, PlayBillingManager.trialStringRes("P7D"))
    }
}
