package com.example.rsrpanalyzer.model.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationTracker(private val context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start(onUpdate: (Location) -> Unit) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onUpdate)
            }
        }

        fusedClient.requestLocationUpdates(request, callback!!, context.mainLooper)
    }

    /**
     * 현재 위치 즉시 조회 (수동 측정용)
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocation: (Location?) -> Unit) {
        fusedClient.lastLocation.addOnSuccessListener { location ->
            onLocation(location)
        }.addOnFailureListener {
            onLocation(null)
        }
    }

    fun stop() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
    }
}