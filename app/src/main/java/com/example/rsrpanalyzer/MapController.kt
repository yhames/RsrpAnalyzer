package com.example.rsrpanalyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.util.Log
import androidx.core.graphics.createBitmap
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.util.concurrent.atomic.AtomicInteger

class MapController(private val context: Context, private val mapView: MapView) {
    private var kakaoMap: KakaoMap? = null
    private var labelManager: LabelManager? = null
    private var labelLayer: LabelLayer? = null
    private var positionLabel: Label? = null
    private val currentRsrp = AtomicInteger(Int.MIN_VALUE)

    fun init() {
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("MapController", "Map destroyed")
            }

            override fun onMapError(error: Exception?) {
                Log.e("MapController", "Map error: ${error?.message}")
            }

        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                labelManager = map.labelManager
                labelLayer = map.labelManager?.layer
                Log.d("MapController", "Map ready")
            }
        })
    }

    /**
     * 위치 정보를 업데이트합니다.
     *
     * @param location 위치 정보
     */
    fun updateLocation(location: Location) {
        val map = kakaoMap ?: return
        val layer = labelLayer ?: return
        val manager = labelManager ?: return

        // Create or update positionLabel
        val position = LatLng.from(location.latitude, location.longitude)
        val styles = manager.addLabelStyles(LabelStyles.from(createRsrpLabelStyle()))
        if (positionLabel == null) {
            val options = LabelOptions.from("user", position).setStyles(styles)
            positionLabel = layer.addLabel(options)
            Log.d("MapController", "positionLabel created: position=$position")
        } else {
            positionLabel?.changeStyles(styles)
            positionLabel?.moveTo(position)
            Log.d("MapController", "positionLabel updated: position=$position")
        }

        val cameraUpdate = CameraUpdateFactory.newCenterPosition(position)
        map.moveCamera(cameraUpdate)
        Log.d("MapController", "Camera moved to $position")
    }


    /**
     * RSRP 값을 업데이트합니다.
     *
     * @param rsrp RSRP 값
     */
    fun updateSignalStrength(rsrp: Int) {
        currentRsrp.set(rsrp)

        positionLabel?.let { label ->
            try {
                val styles = LabelStyles.from(createRsrpLabelStyle())
                label.changeStyles(styles)
                Log.d("MapController", "Signal strength updated: RSRP=$rsrp")
            } catch (e: Exception) {
                Log.e("MapController", "Error updating signal strength", e)
            }
        }
    }

    /**
     * @return currentRsrp에 해당하는 LabelStyle
     */
    private fun createRsrpLabelStyle(): LabelStyle {
        val rsrpLevel = SignalStrengthHelper.getRsrpLevel(currentRsrp.get())
        val color = context.getColor(rsrpLevel.color)
        val bitmap = createColoredCircleBitmap(color, 40)
        return LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
    }

    /**
     * @param color 색상 값
     * @param size 크기 (픽셀)
     * @param factor 내부 원의 밝기 조절 비율
     * @return 원 모양의 Bitmap
     */
    private fun createColoredCircleBitmap(color: Int, size: Int, factor: Float = 1.3f): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 외곽선
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

        // 내부 원 (더 밝게)
        paint.color = adjustColorBrightness(color, factor)
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

        return bitmap
    }

    /**
     * @param color 색상 값
     * @param factor 밝기 조절 비율
     * @return 조절된 색상 값
     */
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xff) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xff) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xff) * factor).toInt().coerceIn(0, 255)
        val a = color shr 24 and 0xff
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}