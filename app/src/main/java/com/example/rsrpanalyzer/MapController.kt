package com.example.rsrpanalyzer

import android.location.Location
import android.util.Log
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import java.lang.Exception

class MapController(private val mapView: MapView) {
    private var kakaoMap: KakaoMap? = null
    private var labelLayer: LabelLayer? = null
    private var positionLabel: Label? = null

    fun init() {
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("MapController", "Map destroyed")
            }

            override fun onMapError(p0: Exception?) {
                Log.e("MapController", "Map error: ${p0?.message}")
            }

        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                labelLayer = map.labelManager?.layer
            }
        })
    }

    fun updateLocation(location: Location) {
        kakaoMap?.let { map ->
            val position = LatLng.from(location.latitude, location.longitude)
            if (positionLabel == null) {
                val options = LabelOptions.from("user", position)
                positionLabel = labelLayer?.addLabel(options)
            } else {
                positionLabel?.moveTo(position)
            }

            val cameraUpdate = CameraUpdateFactory.newCenterPosition(position)
            map.moveCamera(cameraUpdate)
        }
    }
}