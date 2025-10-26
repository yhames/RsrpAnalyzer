package com.example.rsrpanalyzer.view.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.data.db.DatabaseProvider
import com.example.rsrpanalyzer.data.model.SignalRecord
import com.example.rsrpanalyzer.data.repository.SignalRepository
import com.example.rsrpanalyzer.model.record.RecordManager
import com.example.rsrpanalyzer.viewmodel.RecordViewModel
import com.example.rsrpanalyzer.viewmodel.SignalViewModel

class RecordControlFragment : Fragment() {
    private val recordViewModel: RecordViewModel by activityViewModels()
    private val signalViewModel: SignalViewModel by activityViewModels()
    private lateinit var recordManager: RecordManager
    private lateinit var btnRecord: Button
    private lateinit var etSessionName: EditText
    private lateinit var tvStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_record_control, container, false)

        btnRecord = view.findViewById(R.id.btn_record)
        etSessionName = view.findViewById(R.id.et_session_name)
        tvStatus = view.findViewById(R.id.tv_status)


        val db = DatabaseProvider.getDatabase(requireContext())
        val repository = SignalRepository(db.signalSessionDao(), db.signalRecordDao())
        recordManager = RecordManager(repository)

        setupRecordManager()
        observeSignalsForRecording()

        return view
    }

    private fun setupRecordManager() {
        recordViewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            if (isRecording) {
                btnRecord.text = this.getString(R.string.session_stop_recording)
                tvStatus.text = this.getString(R.string.session_tv_status_ongoing)
            } else {
                btnRecord.text = this.getString(R.string.session_start_recording)
                tvStatus.text = this.getString(R.string.session_tv_status_idle)
            }
        }

        btnRecord.setOnClickListener {
            if (recordViewModel.isRecording.value == true) {
                recordManager.stopRecording()
                recordViewModel.updateRecordingStatus(false)
            } else {
                val sessionName = etSessionName.text.toString()
                    .ifBlank { "Session_${System.currentTimeMillis()}" }
                recordManager.startRecording(sessionName)
                recordViewModel.updateRecordingStatus(true)
            }
        }
    }

    private fun observeSignalsForRecording() {
        signalViewModel.location.observe(viewLifecycleOwner) { location ->

            val rsrp = signalViewModel.rsrp.value ?: run {
                Log.d("RecordControlFragment", "No rsrp value")
                return@observe
            }
            val rsrq = signalViewModel.rsrq.value ?: run {
                Log.d("RecordControlFragment", "No rsrq value")
                return@observe
            }
            if (recordViewModel.isRecording()) {
                val sessionId = recordManager.getCurrentSessionId() ?: run {
                    Log.w("RecordControlFragment", "No active recording session")
                    return@observe
                }
                val record = SignalRecord(
                    sessionId = sessionId,
                    longitude = location.longitude,
                    latitude = location.latitude,
                    rsrp = rsrp,
                    rsrq = rsrq
                )
                recordManager.recordSignal(record)
            }
        }
    }
}