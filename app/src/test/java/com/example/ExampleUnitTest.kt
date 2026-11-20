package com.example

import android.location.GnssStatus
import com.example.detector.BleTrackerDetector
import com.example.detector.GnssHardwareDetector
import com.example.detector.MagneticLevel
import com.example.detector.MagnetometerDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertNotNull(findMy)

        val appleFind = BleTrackerDetector.identifyTrackerSignature(
            rawName = "",
            serviceUuids = emptyList(),
            appleManufacturerBytes = byteArrayOf(0x12, 0x19, 0x00)
        )
        assertNotNull(appleFind)

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
}
