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
import com.example.rsrpanalyzer.data.model.SignalRecord
import com.example.rsrpanalyzer.databinding.FragmentTableViewBinding
import com.example.rsrpanalyzer.viewmodel.RecordViewModel
import com.example.rsrpanalyzer.viewmodel.SignalViewModel

class TableViewFragment : Fragment(R.layout.fragment_table_view) {

    private var _binding: FragmentTableViewBinding? = null
    private val binding get() = _binding!!

    private val signalViewModel: SignalViewModel by activityViewModels()
    private val recordViewModel: RecordViewModel by activityViewModels()

    private lateinit var signalRecordAdapter: SignalRecordAdapter

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
        signalRecordAdapter = SignalRecordAdapter()
        binding.rvSignalHistory.apply {
            adapter = signalRecordAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModels() {
        // 현재 값 업데이트
        signalViewModel.location.observe(viewLifecycleOwner) { location ->
            binding.tvLatitude.text = getString(R.string.latitude_value, location.latitude)
            binding.tvLongitude.text = getString(R.string.longitude_value, location.longitude)

            // 녹화 중일 때만 기록 추가
            if (recordViewModel.isRecording.value == true) {
                val rsrp = signalViewModel.rsrp.value ?: Int.MIN_VALUE
                val rsrq = signalViewModel.rsrq.value ?: Int.MIN_VALUE
                if (rsrp != Int.MIN_VALUE) { // 유효한 신호 값이 있을 때만 기록
                    val record = SignalRecord(
                        sessionId = 0, // 임시 ID
                        latitude = location.latitude,
                        longitude = location.longitude,
                        rsrp = rsrp,
                        rsrq = rsrq
                    )
                    signalRecordAdapter.addRecord(record)
                }
            }
        }

        signalViewModel.rsrp.observe(viewLifecycleOwner) { rsrp ->
            binding.tvRsrpTable.text = getString(R.string.rsrp_value_simple, rsrp)
        }

        signalViewModel.rsrq.observe(viewLifecycleOwner) { rsrq ->
            binding.tvRsrqTable.text = getString(R.string.rsrq_value_simple, rsrq)
        }

        // 녹화 상태 변경 시 목록 초기화
        recordViewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            Log.d("TableViewFragment", "Recording state changed: $isRecording. Clearing records.")
            signalRecordAdapter.clearRecords()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
