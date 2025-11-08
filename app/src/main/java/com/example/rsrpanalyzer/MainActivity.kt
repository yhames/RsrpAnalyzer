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
import androidx.fragment.app.Fragment
import com.example.rsrpanalyzer.databinding.ActivityMainBinding
import com.example.rsrpanalyzer.model.location.LocationTracker
import com.example.rsrpanalyzer.model.signal.SignalMonitor
import com.example.rsrpanalyzer.view.history.SessionHistoryFragment
import com.example.rsrpanalyzer.view.record.RecordControlFragment
import com.example.rsrpanalyzer.view.signal.MapViewFragment
import com.example.rsrpanalyzer.view.signal.TableViewFragment
import com.example.rsrpanalyzer.viewmodel.RecordStatusViewModel
import com.example.rsrpanalyzer.viewmodel.CurrentSignalViewModel
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val currentSignalViewModel: CurrentSignalViewModel by viewModels()
    private val recordStatusViewModel: RecordStatusViewModel by viewModels()

    private lateinit var locationTracker: LocationTracker
    private lateinit var signalMonitor: SignalMonitor
    private var isTracking = AtomicBoolean(false)

    private val mapViewFragment by lazy { MapViewFragment() }
    private val tableViewFragment by lazy { TableViewFragment() }
    private val sessionHistoryFragment by lazy { SessionHistoryFragment() }
    private var activeFragment: Fragment = mapViewFragment
    private var isRealtimeMode = true // true: 실시간 보기, false: 이전 기록 보기

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
            supportFragmentManager.beginTransaction().apply {
                add(R.id.main_fragment_container, tableViewFragment).hide(tableViewFragment)
                add(R.id.main_fragment_container, mapViewFragment)
            }.commit()
            activeFragment = mapViewFragment

            supportFragmentManager.beginTransaction()
                .replace(R.id.record_control_container, RecordControlFragment())
                .commit()
        }

        locationTracker = LocationTracker(this)
        signalMonitor = SignalMonitor(this)

        setupBottomNavigation()
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
                currentSignalViewModel.updateLocation(location)
            }
            signalMonitor.start { rsrp, rsrq ->
                currentSignalViewModel.updateSignal(rsrp, rsrq)
            }
        }
    }

    private fun stopTracking() {
        if (isTracking.compareAndSet(true, false)) {
            locationTracker.stop()
            signalMonitor.stop()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_map -> {
                    if (isRealtimeMode) switchFragment(mapViewFragment)
                }
                R.id.navigation_data -> {
                    if (isRealtimeMode) switchFragment(tableViewFragment)
                }
                R.id.navigation_realtime -> {
                    if (!isRealtimeMode) {
                        isRealtimeMode = true
                        showRealtimeMode()
                    }
                }
                R.id.navigation_history -> {
                    if (isRealtimeMode) {
                        isRealtimeMode = false
                        showHistoryMode()
                    }
                }
            }
            true
        }
        
        // 기본적으로 실시간 보기 선택
        binding.bottomNavigation.selectedItemId = R.id.navigation_realtime
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }

    private fun showRealtimeMode() {
        // RecordControl 표시
        binding.recordControlContainer.visibility = View.VISIBLE
        
        // 현재 Fragment 상태에 따라 Map 또는 Table로 전환
        val targetFragment = when (binding.bottomNavigation.selectedItemId) {
            R.id.navigation_data -> tableViewFragment
            else -> mapViewFragment
        }
        
        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            if (!targetFragment.isAdded) {
                add(R.id.main_fragment_container, targetFragment)
            }
            show(targetFragment)
        }.commit()
        activeFragment = targetFragment
    }

    private fun showHistoryMode() {
        // RecordControl 숨김
        binding.recordControlContainer.visibility = View.GONE
        
        // SessionHistoryFragment로 전환
        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            if (!sessionHistoryFragment.isAdded) {
                add(R.id.main_fragment_container, sessionHistoryFragment)
            }
            show(sessionHistoryFragment)
        }.commit()
        activeFragment = sessionHistoryFragment
    }

    private fun observeViewModel() {
        recordStatusViewModel.isRecording.observe(this) { isRecording ->
            binding.tvRecordingStatus.visibility = if (isRecording) View.VISIBLE else View.GONE
        }
        recordStatusViewModel.sessionName.observe(this) { sessionName ->
            binding.tvRecordingStatus.text = getString(R.string.session_recording_status, sessionName)
        }
    }
}
