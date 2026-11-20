package com.example.detector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class MagneticLevel {
    NORMAL,
    ELEVATED,
    STRONG_ANOMALY
}

data class MagneticReading(
    val supported: Boolean = true,
    val microTesla: Float = 0f,
    val baselineMicroTesla: Float = 45f,
    val peakMicroTesla: Float = 0f,
    val deltaMicroTesla: Float = 0f,
    val level: MagneticLevel = MagneticLevel.NORMAL
)

class MagnetometerDetector(context: Context) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val magneticSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isSupported: Boolean
        get() = magneticSensor != null

    fun observeMagneticField(): Flow<Float> = callbackFlow {
        val sensor = magneticSensor
        val manager = sensorManager
        if (sensor == null || manager == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD && event.values.size >= 3) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)
                    trySend(magnitude)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose {
            manager.unregisterListener(listener)
        }
    }

    companion object {
        fun classifyLevel(current: Float, baseline: Float): MagneticLevel {
            val delta = abs(current - baseline)
            return when {
                current >= 120f || delta >= 55f -> MagneticLevel.STRONG_ANOMALY
                current >= 80f || delta >= 25f -> MagneticLevel.ELEVATED
                else -> MagneticLevel.NORMAL
            }
        }
    }
}
