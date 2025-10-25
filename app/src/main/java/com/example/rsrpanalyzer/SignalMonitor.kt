package com.example.rsrpanalyzer

import android.content.Context
import android.os.Build
import android.telephony.*
import android.util.Log
import android.widget.Toast

class SignalMonitor(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var callback: TelephonyCallback? = null

    fun start(onSignalUpdate: (Int, Int) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                    val info =
                        cellInfo.filterIsInstance<CellInfoLte>().firstOrNull { it.isRegistered }

                    info?.cellSignalStrength?.let {
                        val lte = it
                        onSignalUpdate(lte.rsrp, lte.rsrq)
                    }
                }
            }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback!!)
        } else {
            Log.w("SignalMonitor", "Requires Android 12 or higher.")
            TODO("Implement fallback for Android < 12")
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        }
    }
}