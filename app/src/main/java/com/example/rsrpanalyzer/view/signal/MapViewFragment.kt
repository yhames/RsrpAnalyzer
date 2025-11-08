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
import com.example.rsrpanalyzer.viewmodel.RecordStatusViewModel
import com.example.rsrpanalyzer.viewmodel.CurrentSignalViewModel
import com.example.rsrpanalyzer.viewmodel.SessionDataViewModel
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

    private val currentSignalViewModel: CurrentSignalViewModel by activityViewModels()
    private val recordStatusViewModel: RecordStatusViewModel by activityViewModels()
    private val sessionDataViewModel: SessionDataViewModel by activityViewModels()

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
        // 히스토리 모드 감지
        sessionDataViewModel.isHistoryMode.observe(viewLifecycleOwner) { isHistoryMode ->
            if (isHistoryMode) {
                // 실시간 위치 라벨 숨김
                positionLabel?.let { labelLayer?.remove(it) }
                positionLabel = null
                // 기존 녹화 라벨 제거
                if (recordLabels.isNotEmpty()) {
                    labelLayer?.remove(*recordLabels.toTypedArray())
                    recordLabels.clear()
                }
            } else {
                // 실시간 모드로 복귀
                binding.tvStatus.text = getString(R.string.signal_measuring_status)

                // 히스토리 라벨 제거
                if (recordLabels.isNotEmpty()) {
                    labelLayer?.remove(*recordLabels.toTypedArray())
                    recordLabels.clear()
                }

                // 현재 신호 값 복원
                currentSignalViewModel.rsrp.value?.let { rsrp ->
                    val rsrpLabel =
                        getString(SignalStrengthHelper.getRsrpLevel(rsrp).labelResourceId)
                    binding.tvRsrp.text = getString(R.string.rsrp_value, rsrp, rsrpLabel)
                }
                currentSignalViewModel.rsrq.value?.let { rsrq ->
                    val rsrqLabel =
                        getString(SignalStrengthHelper.getRsrqLevel(rsrq).labelResourceId)
                    binding.tvRsrq.text = getString(R.string.rsrq_value, rsrq, rsrqLabel)
                }
            }
        }

        // 세션 기록 데이터 로드
        sessionDataViewModel.sessionRecords.observe(viewLifecycleOwner) { records ->
            if (sessionDataViewModel.isHistoryMode.value == true && records.isNotEmpty()) {
                displaySessionRecords(records)
            }
        }

        recordStatusViewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            // 녹화 상태가 변경될 때마다, 이전에 기록된 원들을 항상 삭제합니다.
            if (recordLabels.isNotEmpty()) {
                labelLayer?.remove(*recordLabels.toTypedArray())
                recordLabels.clear()
            }
        }

        currentSignalViewModel.location.observe(viewLifecycleOwner) { loc ->
            // 히스토리 모드가 아닐 때만 실시간 데이터 표시
            if (sessionDataViewModel.isHistoryMode.value != true) {
                if (recordStatusViewModel.isRecording.value == true) {
                    addRecordedLocation(loc)
                } else {
                    updateCurrentLocation(loc)
                }
            }
        }
        currentSignalViewModel.rsrp.observe(viewLifecycleOwner) { rsrp ->
            // 히스토리 모드가 아닐 때만 실시간 데이터 표시
            if (sessionDataViewModel.isHistoryMode.value != true) {
                updateRsrp(rsrp)
            }
        }
        currentSignalViewModel.rsrq.observe(viewLifecycleOwner) { rsrq ->
            // 히스토리 모드가 아닐 때만 실시간 데이터 표시
            if (sessionDataViewModel.isHistoryMode.value != true) {
                updateRsrq(rsrq)
            }
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
        if (recordStatusViewModel.isRecording.value != true) {
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
        val baseColor = requireContext().getColor(rsrpLevel.color)

        // 반투명 색상으로 변환 (알파값 35%)
        val alpha = (255 * 0.35).toInt()
        val transparentColor = (alpha shl 24) or (baseColor and 0x00FFFFFF)

        val bitmap = bitmapCache.getOrPut(transparentColor) {
            createColoredCircleBitmap(transparentColor, 50, 1.0f)
        }
        return LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
    }

    private fun createColoredCircleBitmap(
        color: Int,
        size: Int = 50,
        factor: Float = 1.0f
    ): Bitmap {
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

    private fun displaySessionRecords(records: List<com.example.rsrpanalyzer.data.db.SignalRecordEntity>) {
        if (!isAdded) return
        val map = kakaoMap ?: return

        // 기존 라벨 제거
        if (recordLabels.isNotEmpty()) {
            labelLayer?.remove(*recordLabels.toTypedArray())
            recordLabels.clear()
        }

        // 히트맵 스타일로 원을 그림 (크고 반투명한 원)
        drawHeatmapCircles(records, map)

        // 신호 패널 업데이트 - 이전 기록 모드이므로 placeholder 표시
        binding.tvStatus.text = getString(R.string.session_history_viewing)
        binding.tvRsrp.text = getString(R.string.rsrp_value_placeholder)
        binding.tvRsrq.text = getString(R.string.rsrq_value_placeholder)
    }

    private fun drawHeatmapCircles(
        records: List<com.example.rsrpanalyzer.data.db.SignalRecordEntity>,
        map: KakaoMap
    ) {
        val layer = labelLayer ?: return
        val manager = labelManager ?: return

        records.forEach { record ->
            val position = LatLng.from(record.latitude, record.longitude)

            // RSRP 기준으로 색상 결정
            val rsrpLevel = SignalStrengthHelper.getRsrpLevel(record.rsrp)
            val baseColor = requireContext().getColor(rsrpLevel.color)

            // 반투명 색상으로 변환 (알파값 35%)
            val alpha = (255 * 0.35).toInt() // 0-255 범위
            val transparentColor = (alpha shl 24) or (baseColor and 0x00FFFFFF)

            // 큰 원 생성 (히트맵 효과)
            val largeBitmap = bitmapCache.getOrPut(transparentColor) {
                createColoredCircleBitmap(transparentColor, 50, 1.0f) // 크기 80, factor 1.0
            }

            val labelStyle = LabelStyle.from(largeBitmap).setAnchorPoint(0.5f, 0.5f)
            val styles = manager.addLabelStyles(LabelStyles.from(labelStyle))
            val options = LabelOptions.from(position).setStyles(styles)
            val label = layer.addLabel(options)
            recordLabels.add(label)
        }

        // 첫 번째 기록 위치로 카메라 이동
        if (records.isNotEmpty()) {
            val firstRecord = records.first()
            val position = LatLng.from(firstRecord.latitude, firstRecord.longitude)
            val cameraUpdate = CameraUpdateFactory.newCenterPosition(position)
            map.moveCamera(cameraUpdate)
        }
    }
}
