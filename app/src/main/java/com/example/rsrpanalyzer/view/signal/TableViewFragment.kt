package com.example.rsrpanalyzer.view.signal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.databinding.FragmentTableViewBinding
import com.example.rsrpanalyzer.viewmodel.RecordStatusViewModel
import com.example.rsrpanalyzer.viewmodel.CurrentSignalViewModel
import com.example.rsrpanalyzer.viewmodel.SessionDataViewModel

class TableViewFragment : Fragment(R.layout.fragment_table_view) {

    private var _binding: FragmentTableViewBinding? = null
    private val binding get() = _binding!!

    private val currentSignalViewModel: CurrentSignalViewModel by activityViewModels()
    private val recordStatusViewModel: RecordStatusViewModel by activityViewModels()
    private val sessionDataViewModel: SessionDataViewModel by activityViewModels()

    private lateinit var signalRecordItemAdapter: SignalRecordItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModels()
    }

    private fun setupRecyclerView() {
        signalRecordItemAdapter = SignalRecordItemAdapter()
        binding.rvSignalHistory.apply {
            adapter = signalRecordItemAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModels() {
        // 히스토리 모드 감지
        sessionDataViewModel.isHistoryMode.observe(viewLifecycleOwner) { isHistoryMode ->
            if (!isHistoryMode) {
                // 실시간 모드로 복귀 시 목록 초기화
                signalRecordItemAdapter.clearRecordItems()

                // 현재 값으로 패널 복원
                currentSignalViewModel.location.value?.let { location ->
                    binding.tvLatitude.text = getString(R.string.latitude_value, location.latitude)
                    binding.tvLongitude.text =
                        getString(R.string.longitude_value, location.longitude)
                }
                currentSignalViewModel.rsrp.value?.let { rsrp ->
                    binding.tvRsrpTable.text = getString(R.string.rsrp_value_simple, rsrp)
                } ?: run {
                    binding.tvRsrpTable.text = getString(R.string.rsrp_placeholder)
                }
                currentSignalViewModel.rsrq.value?.let { rsrq ->
                    binding.tvRsrqTable.text = getString(R.string.rsrq_value_simple, rsrq)
                } ?: run {
                    binding.tvRsrqTable.text = getString(R.string.rsrq_placeholder)
                }
            }
        }

        // 세션 기록 데이터 로드
        sessionDataViewModel.sessionRecords.observe(viewLifecycleOwner) { records ->
            if (sessionDataViewModel.isHistoryMode.value == true && records.isNotEmpty()) {
                displaySessionRecords(records)
            }
        }

        // 현재 값 업데이트 (실시간 모드일 때만)
        currentSignalViewModel.location.observe(viewLifecycleOwner) { location ->
            if (sessionDataViewModel.isHistoryMode.value != true) {
                binding.tvLatitude.text = getString(R.string.latitude_value, location.latitude)
                binding.tvLongitude.text = getString(R.string.longitude_value, location.longitude)

                // 녹화 중일 때만 기록 추가
                if (recordStatusViewModel.isRecording.value == true) {
                    val rsrp = currentSignalViewModel.rsrp.value ?: Int.MIN_VALUE
                    val rsrq = currentSignalViewModel.rsrq.value ?: Int.MIN_VALUE
                    if (rsrp != Int.MIN_VALUE) { // 유효한 신호 값이 있을 때만 기록
                        val recordItem = SignalRecordItem(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            rsrp = rsrp,
                            rsrq = rsrq
                        )
                        signalRecordItemAdapter.addRecordItem(recordItem)
                    }
                }
            }
        }

        currentSignalViewModel.rsrp.observe(viewLifecycleOwner) { rsrp ->
            if (sessionDataViewModel.isHistoryMode.value != true) {
                binding.tvRsrpTable.text = getString(R.string.rsrp_value_simple, rsrp)
            }
        }

        currentSignalViewModel.rsrq.observe(viewLifecycleOwner) { rsrq ->
            if (sessionDataViewModel.isHistoryMode.value != true) {
                binding.tvRsrqTable.text = getString(R.string.rsrq_value_simple, rsrq)
            }
        }

        // 녹화 상태 변경 시 목록 초기화 (실시간 모드일 때만)
        recordStatusViewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            if (sessionDataViewModel.isHistoryMode.value != true) {
                Log.d(
                    "TableViewFragment",
                    "Recording state changed: $isRecording. Clearing records."
                )
                signalRecordItemAdapter.clearRecordItems()
            }
        }
    }

    private fun displaySessionRecords(records: List<com.example.rsrpanalyzer.data.db.SignalRecordEntity>) {
        signalRecordItemAdapter.clearRecordItems()

        // 이전 기록 모드에서는 현재 패널을 placeholder로 표시
        binding.tvLatitude.text = getString(R.string.latitude_placeholder)
        binding.tvLongitude.text = getString(R.string.longitude_placeholder)
        binding.tvRsrpTable.text = getString(R.string.rsrp_placeholder)
        binding.tvRsrqTable.text = getString(R.string.rsrq_placeholder)

        // 모든 기록을 리스트에 추가 (역순)
        records.reversed().forEach { record ->
            val recordItem = SignalRecordItem(
                latitude = record.latitude,
                longitude = record.longitude,
                rsrp = record.rsrp,
                rsrq = record.rsrq
            )
            signalRecordItemAdapter.addRecordItem(recordItem)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
