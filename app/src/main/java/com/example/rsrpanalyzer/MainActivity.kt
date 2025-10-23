package com.example.rsrpanalyzer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.Label

class MainActivity : AppCompatActivity() {

    private lateinit var locationRequest: LocationRequest
    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null
    private var centerLabel: Label? = null
    private var centerLabelStyles: LabelStyles? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        mapView = findViewById(R.id.map_view)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L).build()

        if (hasLocationPermission()) {
            initMap()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            initMap()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initMap() {
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("KakaoMap", "Map destroyed")
            }

            override fun onMapError(error: Exception?) {
                Log.e("KakaoMap", "Map error: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                locationUpdates()
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {
        if (!hasLocationPermission()) {
            return
        }

        val settingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val settingsClient = LocationServices.getSettingsClient(this)

        settingsClient.checkLocationSettings(settingsRequest)
            .addOnFailureListener { showLocationSettingsAlert() }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { updateMapCamera(it) }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback!!, Looper.getMainLooper()
        )
        Log.d("Location", "위치 업데이트 요청 시작")
    }

    private fun updateMapCamera(location: Location) {
        val position = CameraPosition.from(
            location.latitude, location.longitude, 16, 0.0, 0.0, 0.0
        )
        val update = CameraUpdateFactory.newCameraPosition(position)

        runOnUiThread {
            kakaoMap?.moveCamera(update)
            updateUserLabel(location)
        }
    }

    private fun updateUserLabel(location: Location) {
        val map = kakaoMap ?: return
        val labelManager = map.labelManager ?: return
        val layer = labelManager.layer ?: return

        // Remove previous label (we'll re-add) to ensure position update works
        centerLabel?.let { old ->
            try {
                layer.remove(old)
            } catch (_: Exception) {
            }
            centerLabel = null
        }

        // Ensure styles exist. addLabelStyles returns an object used by LabelOptions.setStyles
        if (centerLabelStyles == null) {
            try {
                // addLabelStyles should return a LabelStyles instance
                centerLabelStyles = labelManager.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(android.R.drawable.ic_menu_mylocation))
                )
            } catch (e: Exception) {
                Log.w("KakaoMap", "Label style creation failed: ${e.message}")
                centerLabelStyles = null
            }
        }

        val optionsBuilder = LabelOptions.from(LatLng.from(location.latitude, location.longitude))
        val options = if (centerLabelStyles != null) {
            optionsBuilder.setStyles(centerLabelStyles!!)
        } else optionsBuilder

        val newLabel = layer.addLabel(options)
        centerLabel = newLabel

        // Start tracking this label so the map keeps it centered (if TrackingManager available)
        try {
            map.trackingManager?.startTracking(centerLabel)
        } catch (e: Exception) {
            Log.w("KakaoMap", "Tracking start failed: ${e.message}")
        }
    }

    private fun showLocationSettingsAlert() {
        AlertDialog.Builder(this).setTitle("위치 서비스 비활성화")
            .setMessage("위치 서비스가 꺼져 있습니다.\n\n1. 설정 → 위치 → 위치 사용 ON\n2. 또는 위치 모드를 '정확도 높음'으로 변경하세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }.setNegativeButton("취소", null).show()
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) locationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
        }
    }
}
