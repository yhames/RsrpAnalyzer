package com.example.rsrpanalyzer.view.signal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.rsrpanalyzer.BuildConfig
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.databinding.FragmentMapViewBinding
import com.example.rsrpanalyzer.model.signal.SignalStrengthHelper
import com.example.rsrpanalyzer.viewmodel.RecordViewModel
import com.example.rsrpanalyzer.viewmodel.SignalViewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.util.concurrent.atomic.AtomicInteger

class MapViewFragment : Fragment(R.layout.fragment_map_view) {

    private var _binding: FragmentMapViewBinding? = null
    private val binding get() = _binding!!

    private val signalViewModel: SignalViewModel by activityViewModels()
    private val recordViewModel: RecordViewModel by activityViewModels()

    private var kakaoMap: KakaoMap? = null
    private var labelManager: LabelManager? = null
    private var labelLayer: LabelLayer? = null
    private var positionLabel: Label? = null
    private val recordLabels = mutableListOf<Label>()

    private val currentRsrp = AtomicInteger(Int.MIN_VALUE)
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewBinding.inflate(inflater, container, false)
        KakaoMapSdk.init(requireContext(), BuildConfig.KAKAO_NATIVE_APP_KEY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.start(object : MapLifeCycleCallback() {
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
                observeViewModels()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModels() {
        recordViewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            // 녹화 상태가 변경될 때마다, 이전에 기록된 원들을 항상 삭제합니다.
            if (recordLabels.isNotEmpty()) {
                labelLayer?.remove(*recordLabels.toTypedArray())
                recordLabels.clear()
            }
        }

        signalViewModel.location.observe(viewLifecycleOwner) { loc ->
            if (recordViewModel.isRecording.value == true) {
                addRecordedLocation(loc)
            } else {
                updateCurrentLocation(loc)
            }
        }
        signalViewModel.rsrp.observe(viewLifecycleOwner) { rsrp ->
            updateRsrp(rsrp)
        }
        signalViewModel.rsrq.observe(viewLifecycleOwner) { rsrq ->
            updateRsrq(rsrq)
        }
    }

    private fun addRecordedLocation(location: Location) {
        if (!isAdded) return
        val layer = labelLayer ?: return
        val manager = labelManager ?: return
        val map = kakaoMap ?: return

        val position = LatLng.from(location.latitude, location.longitude)
        val styles = manager.addLabelStyles(LabelStyles.from(createRsrpLabelStyle()))
        val options = LabelOptions.from(position).setStyles(styles)
        val newLabel = layer.addLabel(options)
        recordLabels.add(newLabel)

        val cameraUpdate = CameraUpdateFactory.newCenterPosition(position)
        map.moveCamera(cameraUpdate)
    }

    private fun updateCurrentLocation(location: Location) {
        if (!isAdded) return
        val map = kakaoMap ?: return
        val layer = labelLayer ?: return
        val manager = labelManager ?: return

        val position = LatLng.from(location.latitude, location.longitude)
        val styles = manager.addLabelStyles(LabelStyles.from(createRsrpLabelStyle()))
        if (positionLabel == null) {
            val options = LabelOptions.from("user", position).setStyles(styles)
            positionLabel = layer.addLabel(options)
        } else {
            positionLabel?.changeStyles(styles)
            positionLabel?.moveTo(position)
        }

        val cameraUpdate = CameraUpdateFactory.newCenterPosition(position)
        map.moveCamera(cameraUpdate)
    }

    private fun updateRsrp(rsrp: Int) {
        if (!isAdded) return
        currentRsrp.set(rsrp)
        val rsrpLabel = getString(SignalStrengthHelper.getRsrpLevel(rsrp).labelResourceId)
        binding.tvRsrp.text = getString(R.string.rsrp_value, rsrp, rsrpLabel)

        // 녹화 중이 아닐 때만 실시간 라벨 색상 업데이트
        if (recordViewModel.isRecording.value != true) {
            positionLabel?.let { label ->
                try {
                    val styles =
                        labelManager?.addLabelStyles(LabelStyles.from(createRsrpLabelStyle()))
                    styles?.let { label.changeStyles(it) }
                } catch (e: Exception) {
                    Log.e("MapController", "Error updating signal strength", e)
                }
            }
        }
    }

    private fun updateRsrq(rsrq: Int) {
        if (!isAdded) return
        val rsrqLabel = getString(SignalStrengthHelper.getRsrqLevel(rsrq).labelResourceId)
        binding.tvRsrq.text = getString(R.string.rsrq_value, rsrq, rsrqLabel)
    }

    private fun createRsrpLabelStyle(): LabelStyle {
        val rsrpLevel = SignalStrengthHelper.getRsrpLevel(currentRsrp.get())
        val color = requireContext().getColor(rsrpLevel.color)
        val bitmap = bitmapCache.getOrPut(color) {
            createColoredCircleBitmap(color, 40)
        }
        return LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
    }

    private fun createColoredCircleBitmap(color: Int, size: Int, factor: Float = 1.3f): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

        paint.color = adjustColorBrightness(color, factor)
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
