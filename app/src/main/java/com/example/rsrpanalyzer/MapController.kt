package com.example.rsrpanalyzer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.util.Log
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.lang.Exception
import androidx.core.graphics.createBitmap

class MapController(private val mapView: MapView) {
    private var kakaoMap: KakaoMap? = null
    private var labelLayer: LabelLayer? = null
    private var positionLabel: Label? = null
    
    // Map 준비 전에 들어온 데이터 임시 저장
    private var pendingLocation: Location? = null
    private var pendingRsrp: Int? = null
    private var isMapReady = false

    fun init() {
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("MapController", "Map destroyed")
                isMapReady = false
            }

            override fun onMapError(p0: Exception?) {
                Log.e("MapController", "Map error: ${p0?.message}")
                isMapReady = false
            }

        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                labelLayer = map.labelManager?.layer
                isMapReady = true
                
                Log.d("MapController", "Map ready - applying pending data")

                // Map 준비 완료 후 대기 중인 데이터 적용
                pendingLocation?.let { location ->
                    applyLocation(location)
                }
                pendingRsrp?.let { rsrp ->
                    applySignalStrength(rsrp)
                }
            }
        })
    }

    fun updateLocation(location: Location) {
        if (!isMapReady) {
            Log.d("MapController", "Map not ready - storing location")
            pendingLocation = location
            return
        }
        applyLocation(location)
    }

    private fun applyLocation(location: Location) {
        kakaoMap?.let { map ->
            labelLayer?.let { layer ->
                val position = LatLng.from(location.latitude, location.longitude)
                
                if (positionLabel == null) {
                    Log.d("MapController", "Creating new position label")
                    
                    // 기본 스타일로 Label 생성 (RSRP 있으면 해당 색상, 없으면 기본 회색)
                    val defaultColor = pendingRsrp?.let { SignalStrengthHelper.getColorForRsrp(it) } 
                        ?: 0xFF808080.toInt() // 기본 회색
                    val bitmap = createColoredCircleBitmap(defaultColor, 40)
                    val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
                    val styles = map.labelManager?.addLabelStyles(LabelStyles.from(style))
                    
                    val options = LabelOptions.from("user", position).setStyles(styles)
                    positionLabel = layer.addLabel(options)
                    
                    Log.d("MapController", "Label created with color: ${String.format("#%08X", defaultColor)}")
                    
                    // Label 생성 직후 RSRP가 있으면 바로 적용
                    pendingRsrp?.let { rsrp ->
                        applySignalStrength(rsrp)
                    }
                } else {
                    positionLabel?.moveTo(position)
                }

                val cameraUpdate = CameraUpdateFactory.newCenterPosition(position)
                map.moveCamera(cameraUpdate)
            } ?: Log.w("MapController", "LabelLayer not available")
        } ?: Log.w("MapController", "KakaoMap not available")
    }

    fun updateSignalStrength(rsrp: Int) {
        pendingRsrp = rsrp
        
        if (!isMapReady) {
            Log.d("MapController", "Map not ready - storing RSRP: $rsrp")
            return
        }
        applySignalStrength(rsrp)
    }

    private fun applySignalStrength(rsrp: Int) {
        positionLabel?.let { label ->
            kakaoMap?.labelManager?.let { labelManager ->
                try {
                    val color = SignalStrengthHelper.getColorForRsrp(rsrp)
                    val bitmap = createColoredCircleBitmap(color, 40)
                    val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
                    val styles = labelManager.addLabelStyles(LabelStyles.from(style))
                    
                    label.styles = styles
                    Log.d("MapController", "Signal strength updated: RSRP=$rsrp, Color=${String.format("#%08X", color)}")
                } catch (e: Exception) {
                    Log.e("MapController", "Failed to update signal strength: ${e.message}")
                }
            } ?: Log.w("MapController", "LabelManager not available")
        } ?: Log.w("MapController", "Position label not created yet")
    }

    private fun createColoredCircleBitmap(color: Int, size: Int): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 외곽선
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

        // 내부 원 (더 밝게)
        paint.color = adjustColorBrightness(color, 1.3f)
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

        return bitmap
    }

    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xff) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xff) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xff) * factor).toInt().coerceIn(0, 255)
        val a = color shr 24 and 0xff
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}