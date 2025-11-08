package com.example.rsrpanalyzer.model.signal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import java.lang.reflect.Method

class SignalMonitor(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var callback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var pollingHandler: Handler? = null
    private var pollingRunnable: Runnable? = null

    companion object {
        private const val POLLING_INTERVAL_MS = 1000L // 1초 (LocationTracker와 동기화)
        private const val TAG = "SignalMonitor"
    }

    @SuppressLint("MissingPermission")
    fun start(onSignalUpdate: (Int, Int) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startWithTelephonyCallback(onSignalUpdate)
        } else {
            startWithPhoneStateListener(onSignalUpdate)
        }

        // 주기적 폴링 (모든 버전에서 동작)
        startPeriodicPolling(onSignalUpdate)

        Log.d(TAG, "Started (API ${Build.VERSION.SDK_INT})")
    }

    @SuppressLint("MissingPermission")
    private fun startWithTelephonyCallback(onSignalUpdate: (Int, Int) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                    processSignalInfo(cellInfo, onSignalUpdate)
                }
            }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback!!)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun startWithPhoneStateListener(onSignalUpdate: (Int, Int) -> Unit) {
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                signalStrength?.let {
                    val rsrp = extractRsrp(it)
                    val rsrq = extractRsrq(it)
                    if (rsrp != null && rsrq != null) {
                        onSignalUpdate(rsrp, rsrq)
                    }
                }
            }
        }
        telephonyManager.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
        )
    }

    @SuppressLint("MissingPermission")
    private fun startPeriodicPolling(onSignalUpdate: (Int, Int) -> Unit) {
        pollingHandler = Handler(Looper.getMainLooper())
        pollingRunnable = object : Runnable {
            override fun run() {
                try {
                    val cellInfo = telephonyManager.allCellInfo
                    if (cellInfo != null) {
                        processSignalInfo(cellInfo, onSignalUpdate)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied for allCellInfo", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error polling signal info", e)
                }
                pollingHandler?.postDelayed(this, POLLING_INTERVAL_MS)
            }
        }
        pollingHandler?.post(pollingRunnable!!)
    }

    private fun processSignalInfo(
        cellInfo: List<CellInfo>,
        onSignalUpdate: (Int, Int) -> Unit
    ) {
        val info = cellInfo.filterIsInstance<CellInfoLte>().firstOrNull { it.isRegistered }
        info?.cellSignalStrength?.let { signalStrength ->
            val rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                signalStrength.rsrp
            } else {
                extractRsrpReflection(signalStrength)
            }

            val rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                signalStrength.rsrq
            } else {
                extractRsrqReflection(signalStrength)
            }

            if (rsrp != null && rsrq != null) {
                onSignalUpdate(rsrp, rsrq)
            }
        }
    }

    // API 26 미만을 위한 Reflection 기반 추출
    private fun extractRsrpReflection(signalStrength: Any): Int? {
        return try {
            val method: Method = signalStrength.javaClass.getMethod("getRsrp")
            method.invoke(signalStrength) as? Int
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get RSRP via reflection", e)
            null
        }
    }

    private fun extractRsrqReflection(signalStrength: Any): Int? {
        return try {
            val method: Method = signalStrength.javaClass.getMethod("getRsrq")
            method.invoke(signalStrength) as? Int
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get RSRQ via reflection", e)
            null
        }
    }

    // SignalStrength 객체에서 직접 추출 (Android < S)
    private fun extractRsrp(signalStrength: SignalStrength): Int? {
        return try {
            val method: Method = signalStrength.javaClass.getMethod("getRsrp")
            method.invoke(signalStrength) as? Int
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract RSRP from SignalStrength", e)
            null
        }
    }

    private fun extractRsrq(signalStrength: SignalStrength): Int? {
        return try {
            val method: Method = signalStrength.javaClass.getMethod("getRsrq")
            method.invoke(signalStrength) as? Int
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract RSRQ from SignalStrength", e)
            null
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
            callback = null
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
            phoneStateListener = null
        }

        Log.d(TAG, "Stopped")
    }
}