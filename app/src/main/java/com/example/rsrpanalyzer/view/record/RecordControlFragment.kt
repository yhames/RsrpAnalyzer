package com.example.rsrpanalyzer.view.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.persistence.db.DatabaseProvider
import com.example.rsrpanalyzer.persistence.model.SignalRecord
import com.example.rsrpanalyzer.persistence.repository.SignalRepository
import com.example.rsrpanalyzer.databinding.FragmentRecordControlBinding
import com.example.rsrpanalyzer.model.record.RecordManager
import com.example.rsrpanalyzer.viewmodel.RecordStatusViewModel
import com.example.rsrpanalyzer.viewmodel.CurrentSignalViewModel

class RecordControlFragment : Fragment() {
    private var _binding: FragmentRecordControlBinding? = null
    private val binding get() = _binding!!

    private val recordStatusViewModel: RecordStatusViewModel by activityViewModels()
    private val currentSignalViewModel: CurrentSignalViewModel by activityViewModels()
    private lateinit var recordManager: RecordManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())
        val repository = SignalRepository(db.signalSessionDao(), db.signalRecordDao())
        recordManager = RecordManager(repository)

        setupRecordManager()
        observeSignalsForRecording()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecordManager() {
        recordStatusViewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            if (isRecording) {
                binding.btnRecord.text = this.getString(R.string.session_stop_recording)
            } else {
                binding.btnRecord.text = this.getString(R.string.session_start_recording)
            }
        }

        binding.btnRecord.setOnClickListener {
            if (recordStatusViewModel.isRecording.value == true) {
                recordManager.stopRecording()
                recordStatusViewModel.updateRecordingStatus(false)
            } else {
                showSessionNameDialog()
            }
        }
    }

    private fun showSessionNameDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter session name")

        val input = EditText(requireContext())
        input.hint = "Session Name"
        builder.setView(input)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val sessionName = input.text.toString()
                .ifBlank { "Session_${System.currentTimeMillis()}" }
            recordManager.startRecording(sessionName)
            recordStatusViewModel.updateSessionName(sessionName)
            recordStatusViewModel.updateRecordingStatus(true)
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun observeSignalsForRecording() {
        currentSignalViewModel.location.observe(viewLifecycleOwner) { location ->

            val rsrp = currentSignalViewModel.rsrp.value ?: run {
                Log.d("RecordControlFragment", "No rsrp value")
                return@observe
            }
            val rsrq = currentSignalViewModel.rsrq.value ?: run {
                Log.d("RecordControlFragment", "No rsrq value")
                return@observe
            }
            if (recordStatusViewModel.isRecording()) {
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