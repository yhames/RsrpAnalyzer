package com.example.rsrpanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.rsrpanalyzer.databinding.ActivityMainBinding
import com.example.rsrpanalyzer.model.location.LocationTracker
import com.example.rsrpanalyzer.model.signal.SignalMonitor
import com.example.rsrpanalyzer.view.navigation.BottomNavBar
import com.example.rsrpanalyzer.view.record.RecordControlFragment
import com.example.rsrpanalyzer.view.signal.MapViewFragment
import com.example.rsrpanalyzer.viewmodel.RecordViewModel
import com.example.rsrpanalyzer.viewmodel.SignalViewModel
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val signalViewModel: SignalViewModel by viewModels()
    private val recordViewModel: RecordViewModel by viewModels()

    private lateinit var locationTracker: LocationTracker
    private lateinit var signalMonitor: SignalMonitor
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
                startTracking()
            } else {
                Toast.makeText(
                    this, this.getString(R.string.permission_denied), Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, MapViewFragment())
                .commit()
            supportFragmentManager.beginTransaction()
                .replace(R.id.record_control_container, RecordControlFragment())
                .commit()
        }

        locationTracker = LocationTracker(this)
        signalMonitor = SignalMonitor(this)

        bottomNavBar = BottomNavBar(this)
        bottomNavBar.setup()

        observeViewModel()
        requestPermissions()
    }

    override fun onStart() {
        super.onStart()
        if (hasRequiredPermissions()) {
            startTracking()
        }
    }

    override fun onStop() {
        super.onStop()
        stopTracking()
    }

    private fun requestPermissions() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionsLauncher.launch(notGranted.toTypedArray())
        } else {
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
        recordViewModel.isRecording.observe(this) { isRecording ->
            binding.tvRecordingStatus.visibility = if (isRecording) View.VISIBLE else View.GONE
        }
        recordViewModel.sessionName.observe(this) { sessionName ->
            binding.tvRecordingStatus.text =
                getString(R.string.session_recording_status, sessionName)
        }
    }
}
