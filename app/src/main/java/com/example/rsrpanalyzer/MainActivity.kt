package com.example.rsrpanalyzer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.telephony.CellInfoLte
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
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

    // 현재 위치 표시용 Label (하나만 유지)
    private var currentLocationLabel: Label? = null
    private var currentLocationStyles: LabelStyles? = null

    // 측정 지점들을 저장하는 리스트
    private val measurementLabels = mutableListOf<Label>()

    // 카메라 초기 이동 여부 (한 번만 이동)
    private var isInitialCameraMove = true

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // TelephonyManager for RSRP/RSRQ measurement
    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: TelephonyCallback? = null

    // UI Components
    private lateinit var tvRsrp: TextView
    private lateinit var tvRsrq: TextView
    private lateinit var tvStatus: TextView

    // Current signal values
    private var currentRsrp: Int = 0
    private var currentRsrq: Int = 0

    // Current location
    private var currentLocation: Location? = null

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRsrp = findViewById(R.id.tv_rsrp)
        tvRsrq = findViewById(R.id.tv_rsrq)
        tvStatus = findViewById(R.id.tv_status)
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        mapView = findViewById(R.id.map_view)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L).build()

        if (!hasAllPermissions()) {
            requestPermissions()
        } else {
            initMap()
            startRsrpMonitoring()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasRetrieveStatePermission(): Boolean {
        val state = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        )
        return state == PackageManager.PERMISSION_GRANTED
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

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, requiredPermissions, LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            initMap()
            if (hasRetrieveStatePermission()) {
                startRsrpMonitoring()
            }
        } else {
            Toast.makeText(this, "필요한 권한이 없습니다.", Toast.LENGTH_LONG).show()
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
                result.lastLocation?.let {
                    currentLocation = it
                    updateCurrentLocation(it)

                    // 초기에만 카메라 이동
                    if (isInitialCameraMove) {
                        moveCamera(it)
                        isInitialCameraMove = false
                    }
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback!!, Looper.getMainLooper()
        )
        Log.d("Location", "위치 업데이트 요청 시작")
    }

    private fun moveCamera(location: Location) {
        val position = CameraPosition.from(
            location.latitude, location.longitude, 16, 0.0, 0.0, 0.0
        )
        val update = CameraUpdateFactory.newCameraPosition(position)

        runOnUiThread {
            kakaoMap?.moveCamera(update)
        }
    }

    private fun updateCurrentLocation(location: Location) {
        val map = kakaoMap ?: return
        val labelManager = map.labelManager ?: return
        val layer = labelManager.layer ?: return

        runOnUiThread {
            // 기존 현재 위치 Label이 있으면 위치만 업데이트
            if (currentLocationLabel != null) {
                try {
                    val newPosition = LatLng.from(location.latitude, location.longitude)
                    currentLocationLabel?.moveTo(newPosition)
                } catch (e: Exception) {
                    Log.w("KakaoMap", "Label move failed: ${e.message}, recreating label")
                    // moveTo 실패 시 Label 재생성
                    currentLocationLabel?.let { layer.remove(it) }
                    currentLocationLabel = null
                }
            }

            // Label이 없으면 새로 생성
            if (currentLocationLabel == null) {
                // 스타일 생성 (한 번만)
                if (currentLocationStyles == null) {
                    try {
                        currentLocationStyles = labelManager.addLabelStyles(
                            LabelStyles.from(LabelStyle.from(android.R.drawable.ic_menu_mylocation))
                        )
                    } catch (e: Exception) {
                        Log.w("KakaoMap", "Label style creation failed: ${e.message}")
                    }
                }

                // Label 생성
                val optionsBuilder =
                    LabelOptions.from(LatLng.from(location.latitude, location.longitude))
                val options = if (currentLocationStyles != null) {
                    optionsBuilder.setStyles(currentLocationStyles!!)
                } else {
                    optionsBuilder
                }

                currentLocationLabel = layer.addLabel(options)
                Log.d("KakaoMap", "Current location label created")
            }
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
        if (hasLocationPermission()) {
            locationUpdates()
        }
        if (hasRetrieveStatePermission()) {
            startRsrpMonitoring()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        stopRsrpMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopRsrpMonitoring()
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRsrpMonitoring() {
        if (!hasRetrieveStatePermission()) {
            Log.w("RSRP", "Permission not granted")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) 이상
            registerTelephonyCallbackForS()
        } else {
            // Android 11 이하
            measureRsrpLegacy()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    private fun registerTelephonyCallbackForS() {
        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
            override fun onCellInfoChanged(cellInfo: MutableList<android.telephony.CellInfo>) {
                processCellInfo(cellInfo)
            }
        }
        telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback!!)
        Log.d("RSRP", "TelephonyCallback registered (API 31+)")
    }

    @SuppressLint("MissingPermission")
    private fun measureRsrpLegacy() {
        // Android 11 이하에서는 직접 CellInfo를 가져옴
        val cellInfoList = telephonyManager.allCellInfo
        if (cellInfoList != null) {
            processCellInfo(cellInfoList)
        }

        // 주기적으로 측정하기 위해 Handler 사용 가능
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (hasRetrieveStatePermission()) {
                measureRsrpLegacy()
            }
        }, 2000) // 2초마다 측정
    }

    private fun processCellInfo(cellInfoList: List<android.telephony.CellInfo>) {
        var rsrp: Int? = null
        var rsrq: Int? = null

        for (cellInfo in cellInfoList) {
            if (cellInfo is CellInfoLte && cellInfo.isRegistered) {
                val signalStrength = cellInfo.cellSignalStrength
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    rsrp = signalStrength.rsrp
                }  // dBm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    rsrq = signalStrength.rsrq
                }  // dB

                Log.d("RSRP", "LTE Signal - RSRP: $rsrp dBm, RSRQ: $rsrq dB")
                break
            }
        }

        // UI 업데이트
        if (rsrp != null && rsrq != null) {
            currentRsrp = rsrp
            currentRsrq = rsrq
            updateSignalUI(rsrp, rsrq)
        } else {
            runOnUiThread {
                "LTE 신호를 찾을 수 없습니다".also { tvStatus.text = it }
            }
        }
    }

    private fun updateSignalUI(rsrp: Int, rsrq: Int) {
        runOnUiThread {
            "RSRP: $rsrp dBm".also { tvRsrp.text = it }
            "RSRQ: $rsrq dB".also { tvRsrq.text = it }

            // 신호 품질 판정
            val quality = when {
                rsrp >= -80 -> "우수"
                rsrp >= -90 -> "양호"
                rsrp >= -100 -> "보통"
                rsrp >= -110 -> "약함"
                else -> "매우 약함"
            }
            "신호 상태: $quality".also { tvStatus.text = it }
        }
    }

    private fun stopRsrpMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        }
        telephonyCallback = null
    }
}
