package com.example.rsrpanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.rsrpanalyzer.model.location.LocationTracker
import com.example.rsrpanalyzer.model.signal.SignalMonitor
import com.example.rsrpanalyzer.model.signal.SignalStrengthHelper
import com.example.rsrpanalyzer.view.map.MapVisualizer
import com.example.rsrpanalyzer.view.navigation.BottomNavBar
import com.example.rsrpanalyzer.view.record.RecordControlFragment
import com.example.rsrpanalyzer.viewmodel.SignalViewModel
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapView
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val signalViewModel: SignalViewModel by viewModels()
    private lateinit var mapView: MapView
    private lateinit var tvRsrp: TextView
    private lateinit var tvRsrq: TextView
    private lateinit var locationTracker: LocationTracker
    private lateinit var signalMonitor: SignalMonitor
    private lateinit var mapVisualizer: MapVisualizer
    private lateinit var bottomNavBar: BottomNavBar
    private var isTracking = AtomicBoolean(false)

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                // Scenario2: Start tracking after all permissions are granted
                startTracking()
            } else {
                Toast.makeText(
                    this, this.getString(R.string.permission_denied), Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map_view)
        tvRsrp = findViewById(R.id.tv_rsrp)
        tvRsrq = findViewById(R.id.tv_rsrq)

        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        mapVisualizer = MapVisualizer(this, mapView)
        mapVisualizer.init()

        locationTracker = LocationTracker(this)
        signalMonitor = SignalMonitor(this)

        bottomNavBar = BottomNavBar(this)
        bottomNavBar.setup()

        observeViewModel()
        requestPermissions()

        supportFragmentManager.beginTransaction()
            .replace(R.id.record_control_container, RecordControlFragment())
            .commit()
    }

    override fun onStart() {
        super.onStart()
        if (hasRequiredPermissions()) {
            // Scenario3: Resume tracking when transitioning from background to foreground
            startTracking()
        }
    }

    override fun onStop() {
        super.onStop()
        stopTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun requestPermissions() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionsLauncher.launch(notGranted.toTypedArray())
        } else {
            // Scenario1: Start tracking when all required permissions are already granted
            startTracking()
        }
    }

    private fun hasRequiredPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startTracking() {
        if (isTracking.compareAndSet(false, true)) {
            locationTracker.start { location ->
                signalViewModel.updateLocation(location)
            }
            signalMonitor.start { rsrp, rsrq ->
                signalViewModel.updateSignal(rsrp, rsrq)
            }
        }
    }

    private fun stopTracking() {
        if (isTracking.compareAndSet(true, false)) {
            locationTracker.stop()
            signalMonitor.stop()
        }
    }

    private fun observeViewModel() {
        signalViewModel.location.observe(this) { loc ->
            mapVisualizer.updateLocation(loc)
        }
        signalViewModel.rsrp.observe(this) { rsrp ->
            val rsrpLabel = this.getString(SignalStrengthHelper.getRsrpLevel(rsrp).labelResourceId)
            tvRsrp.text = getString(R.string.rsrp_value, rsrp, rsrpLabel)
            mapVisualizer.updateSignalStrength(rsrp)
        }
        signalViewModel.rsrq.observe(this) { rsrq ->
            val rsrqLabel = this.getString(SignalStrengthHelper.getRsrqLevel(rsrq).labelResourceId)
            tvRsrq.text = getString(R.string.rsrq_value, rsrq, rsrqLabel)
        }
    }
}
