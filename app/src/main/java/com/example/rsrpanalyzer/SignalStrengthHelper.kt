package com.example.rsrpanalyzer

import androidx.core.graphics.toColorInt

object SignalStrengthHelper {

    enum class SignalLevel(val label: String, val color: Int) {
        EXCELLENT("매우 좋음", "#00C851".toColorInt()),  // Green
        GOOD("좋음", "#7CB342".toColorInt()),           // Light Green
        FAIR("보통", "#FFB300".toColorInt()),           // Amber
        POOR("나쁨", "#FF6F00".toColorInt()),           // Orange
        VERY_POOR("매우 나쁨", "#F44336".toColorInt()),  // Red
        NO_SIGNAL("신호 없음", "#FF8080".toColorInt())   // Black
    }

    /**
     * RSRP 값에 따른 신호 강도 레벨 반환
     * RSRP (Reference Signal Received Power) 기준:
     * >= -80 dBm: 매우 좋음
     * >= -90 dBm: 좋음
     * >= -100 dBm: 보통
     * >= -110 dBm: 나쁨
     * < -110 dBm: 매우 나쁨
     */
    fun getSignalLevel(rsrp: Int): SignalLevel {
        return when {
            rsrp >= -80 -> SignalLevel.EXCELLENT
            rsrp >= -90 -> SignalLevel.GOOD
            rsrp >= -100 -> SignalLevel.FAIR
            rsrp >= -110 -> SignalLevel.POOR
            rsrp >= -140 -> SignalLevel.VERY_POOR
            else -> SignalLevel.NO_SIGNAL
        }
    }

    /**
     * RSRP 값에 따른 색상 반환
     */
    fun getColorForRsrp(rsrp: Int): Int {
        return getSignalLevel(rsrp).color
    }

    /**
     * RSRP 값에 따른 레벨 설명 반환
     */
    fun getLabelForRsrp(rsrp: Int): String {
        return getSignalLevel(rsrp).label
    }
}
