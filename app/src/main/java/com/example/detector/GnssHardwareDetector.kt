package com.example.detector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SatelliteInfo(
    val svid: Int,
    val constellation: String,
    val cn0DbHz: Float,
    val usedInFix: Boolean
)

data class GnssSnapshot(
    val hasGpsHardware: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isGpsProviderEnabled: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val satellites: List<SatelliteInfo> = emptyList(),
    val usedCount: Int = 0,
    val averageCn0: Float = 0f,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null
)

class GnssHardwareDetector(private val context: Context) {
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun checkStaticCapabilities(): GnssSnapshot {
        val hasHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        val manager = locationManager
        val isLocationOn = manager != null && LocationManagerCompat.isLocationEnabled(manager)
        val isGpsProviderOn = try {
            manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
        return GnssSnapshot(
            hasGpsHardware = hasHardware,
            isLocationEnabled = isLocationOn,
            isGpsProviderEnabled = isGpsProviderOn,
            hasLocationPermission = hasFineLocationPermission()
        )
    }

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun observeGnssSignals(): Flow<GnssSnapshot> = callbackFlow {
        val staticInfo = checkStaticCapabilities()
        trySend(staticInfo)

        val manager = locationManager
        if (manager == null || !staticInfo.hasLocationPermission || !staticInfo.isLocationEnabled) {
            awaitClose { }
            return@callbackFlow
        }

        var currentSatellites = emptyList<SatelliteInfo>()
        var lastLat: Double? = null
        var lastLon: Double? = null
        var lastAcc: Float? = null

        fun emitSnapshot() {
            val used = currentSatellites.count { it.usedInFix }
            val avg = if (currentSatellites.isNotEmpty()) {
                currentSatellites.map { it.cn0DbHz }.average().toFloat()
            } else {
                0f
            }
            val latestStatic = checkStaticCapabilities()
            trySend(
                latestStatic.copy(
                    satellites = currentSatellites.sortedByDescending { it.cn0DbHz },
                    usedCount = used,
                    averageCn0 = avg,
                    latitude = lastLat,
                    longitude = lastLon,
                    accuracyMeters = lastAcc
                )
            )
        }

        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val list = ArrayList<SatelliteInfo>(status.satelliteCount)
                for (i in 0 until status.satelliteCount) {
                    list.add(
                        SatelliteInfo(
                            svid = status.getSvid(i),
                            constellation = constellationName(status.getConstellationType(i)),
                            cn0DbHz = status.getCn0DbHz(i),
                            usedInFix = status.usedInFix(i)
                        )
                    )
                }
                currentSatellites = list
                emitSnapshot()
            }
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastLat = location.latitude
                lastLon = location.longitude
                lastAcc = if (location.hasAccuracy()) location.accuracy else null
                emitSnapshot()
            }

            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = emitSnapshot()
            override fun onProviderDisabled(provider: String) = emitSnapshot()
        }

        val mainHandler = Handler(Looper.getMainLooper())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.registerGnssStatusCallback(context.mainExecutor, gnssCallback)
            } else {
                @Suppress("DEPRECATION")
                manager.registerGnssStatusCallback(gnssCallback, mainHandler)
            }

            if (manager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1500L,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }

        awaitClose {
            try {
                manager.unregisterGnssStatusCallback(gnssCallback)
                manager.removeUpdates(locationListener)
            } catch (_: SecurityException) {
            }
        }
    }

    companion object {
        fun constellationName(type: Int): String = when (type) {
            GnssStatus.CONSTELLATION_GPS -> "GPS"
            GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
            GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
            GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
            GnssStatus.CONSTELLATION_QZSS -> "QZSS"
            GnssStatus.CONSTELLATION_SBAS -> "SBAS"
            else -> "Other"
        }
    }
}
