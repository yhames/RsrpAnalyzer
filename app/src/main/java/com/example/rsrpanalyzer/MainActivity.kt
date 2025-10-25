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
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapView

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var mapView: MapView
    private lateinit var tvRsrp: TextView
    private lateinit var tvRsrq: TextView

    private lateinit var locationTracker: LocationTracker
    private lateinit var signalMonitor: SignalMonitor
    private lateinit var mapController: MapController

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!permissions.values.all { it }) {
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

        mapController = MapController(mapView)
        locationTracker = LocationTracker(this)
        signalMonitor = SignalMonitor(this)


        observeViewModel()
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionsLauncher.launch(notGranted.toTypedArray())
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        mapController.init()

        locationTracker.start { location ->
            viewModel.updateLocation(location)
        }

        signalMonitor.start { rsrp, rsrq ->
            viewModel.updateSignal(rsrp, rsrq)
        }
    }

    private fun observeViewModel() {
        viewModel.location.observe(this) { loc ->
            mapController.updateLocation(loc)
        }
        viewModel.rsrp.observe(this) { rsrp ->
            val rsrpLabel = this.getString(SignalStrengthHelper.getRsrpLevel(rsrp).labelResourceId)
            tvRsrp.text = getString(R.string.rsrp_value, rsrp, rsrpLabel)
            mapController.updateSignalStrength(rsrp)
        }
        viewModel.rsrq.observe(this) { rsrq ->
            val rsrqLabel = this.getString(SignalStrengthHelper.getRsrqLevel(rsrq).labelResourceId)
            tvRsrq.text = getString(R.string.rsrq_value, rsrq, rsrqLabel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stop()
        signalMonitor.stop()
    }
}
