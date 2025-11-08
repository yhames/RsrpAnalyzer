package com.example.rsrpanalyzer.model.signal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

class SignalMonitor(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var callback: TelephonyCallback? = null
    private var pollingHandler: Handler? = null
    private var pollingRunnable: Runnable? = null
    
    companion object {
        private const val POLLING_INTERVAL_MS = 1000L // 1초 (LocationTracker와 동기화)
    }

    @SuppressLint("MissingPermission")
    fun start(onSignalUpdate: (Int, Int) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 이벤트 기반 + 주기적 폴링 하이브리드 방식
            
            // 1. 이벤트 기반 리스너 (즉각적인 변화 감지)
            callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                    processSignalInfo(cellInfo, onSignalUpdate)
                }
            }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback!!)
            
            // 2. 주기적 폴링 (1초마다 강제 측정, 위치 측정과 동기화)
            pollingHandler = Handler(Looper.getMainLooper())
            pollingRunnable = object : Runnable {
                override fun run() {
                    try {
                        val cellInfo = telephonyManager.allCellInfo
                        if (cellInfo != null) {
                            processSignalInfo(cellInfo, onSignalUpdate)
                        }
                    } catch (e: SecurityException) {
                        Log.e("SignalMonitor", "Permission denied for allCellInfo", e)
                    }
                    pollingHandler?.postDelayed(this, POLLING_INTERVAL_MS)
                }
            }
            pollingHandler?.post(pollingRunnable!!)
            
            Log.d("SignalMonitor", "Started with hybrid mode (event + 1s polling)")
        } else {
            Log.w("SignalMonitor", "Requires Android 12 or higher.")
            TODO("Implement fallback for Android < 12")
        }
    }

    private fun processSignalInfo(
        cellInfo: List<CellInfo>,
        onSignalUpdate: (Int, Int) -> Unit
    ) {
        val info = cellInfo.filterIsInstance<CellInfoLte>().firstOrNull { it.isRegistered }
        info?.cellSignalStrength?.let {
            val lte = it
            onSignalUpdate(lte.rsrp, lte.rsrq)
        }
    }

    fun stop() {
        // 폴링 중지
        pollingRunnable?.let { pollingHandler?.removeCallbacks(it) }
        pollingHandler = null
        pollingRunnable = null
        
        // 이벤트 리스너 해제
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        }
        
        Log.d("SignalMonitor", "Stopped")
    }
}