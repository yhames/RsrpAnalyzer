package com.example.rsrpanalyzer

import androidx.core.graphics.toColorInt

object SignalStrengthHelper {

    private const val RSRP_EXCELLENT = -80
    private const val RSRP_GOOD = -90
    private const val RSRP_FAIR = -100
    private const val RSRP_POOR = -110
    private const val RSRP_VERY_POOR = -120

    private const val RSRQ_EXCELLENT = -3
    private const val RSRQ_GOOD = -8
    private const val RSRQ_FAIR = -12
    private const val RSRQ_POOR = -16
    private const val RSRQ_VERY_POOR = -19

    enum class SignalLevel(val level: Int, val labelResourceId: Int, val color: Int) {
        EXCELLENT(5, R.string.signal_excellent, "#00C851".toColorInt()),  // Green
        GOOD(4, R.string.signal_good, "#7CB342".toColorInt()),            // Light Green
        FAIR(3, R.string.signal_fair, "#FFB300".toColorInt()),            // Amber
        POOR(2, R.string.signal_poor, "#FF6F00".toColorInt()),            // Orange
        VERY_POOR(1, R.string.signal_very_poor, "#F44336".toColorInt()),  // Red
        NO_SIGNAL(0, R.string.signal_no_signal, "#FF8080".toColorInt())   // Black
    }

    fun getSignalLevel(rsrp: Int, rsrq: Int): SignalLevel {
        val rsrpLevel = getRsrpLevel(rsrp)
        val rsrqLevel = getRsrqLevel(rsrq)
        // 둘 중 낮은 레벨을 선택
        return if (rsrpLevel.level < rsrqLevel.level) rsrpLevel else rsrqLevel
    }

    /**
     * RSRP 값에 따른 신호 강도 레벨 반환
     * RSRP (Reference Signal Received Power) 기준:
     * >= -80 dBm: 매우 좋음
     * >= -90 dBm: 좋음
     * >= -100 dBm: 보통
     * >= -110 dBm: 나쁨
     * < -120 dBm: 매우 나쁨
     */
    fun getRsrpLevel(rsrp: Int): SignalLevel {
        return when {
            rsrp >= RSRP_EXCELLENT -> SignalLevel.EXCELLENT
            rsrp >= RSRP_GOOD -> SignalLevel.GOOD
            rsrp >= RSRP_FAIR -> SignalLevel.FAIR
            rsrp >= RSRP_POOR -> SignalLevel.POOR
            rsrp >= RSRP_VERY_POOR -> SignalLevel.VERY_POOR
            else -> SignalLevel.NO_SIGNAL
        }
    }

    /**
     * RSRQ 값에 따른 신호 강도 레벨 반환
     * RSRQ (Reference Signal Received Quality) 기준:
     * >= -3 dB: 매우 좋음
     * >= -8 dB: 좋음
     * >= -12 dB: 보통
     * >= -16 dB: 나쁨
     * < -19 dB: 매우 나쁨
     */
    fun getRsrqLevel(rsrq: Int): SignalLevel {
        return when {
            rsrq >= RSRQ_EXCELLENT -> SignalLevel.EXCELLENT
            rsrq >= RSRQ_GOOD -> SignalLevel.GOOD
            rsrq >= RSRQ_FAIR -> SignalLevel.FAIR
            rsrq >= RSRQ_POOR -> SignalLevel.POOR
            rsrq >= RSRQ_VERY_POOR -> SignalLevel.VERY_POOR
            else -> SignalLevel.NO_SIGNAL
        }
    }
}
