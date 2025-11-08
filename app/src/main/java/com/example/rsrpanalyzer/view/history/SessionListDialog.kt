package com.example.rsrpanalyzer.view.history

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.rsrpanalyzer.data.db.DatabaseProvider
import com.example.rsrpanalyzer.databinding.DialogSessionListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionListDialog : DialogFragment() {

    private var _binding: DialogSessionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionHistoryAdapter: SessionHistoryAdapter
    private var onSessionSelected: ((SessionItem) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSessionListBinding.inflate(layoutInflater)
        
        setupRecyclerView()
        loadSessionHistory()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    private fun setupRecyclerView() {
        sessionHistoryAdapter = SessionHistoryAdapter { sessionItem ->
            onSessionSelected?.invoke(sessionItem)
            dismiss()
        }
        binding.rvSessionList.adapter = sessionHistoryAdapter
    }

    private fun loadSessionHistory() {
        lifecycleScope.launch {
            val database = DatabaseProvider.getDatabase(requireContext())
            val sessions = withContext(Dispatchers.IO) {
                database.signalSessionDao().findAll()
            }

            val sessionItems = sessions.map { session ->
                val recordCount = withContext(Dispatchers.IO) {
                    database.signalRecordDao().findAllBySessionId(session.id).size
                }
                SessionItem(
                    id = session.id,
                    sessionName = session.sessionName,
                    createdAt = session.createdAt,
                    recordCount = recordCount
                )
            }

            sessionHistoryAdapter.submitList(sessionItems)
            binding.tvEmptyMessage.isVisible = sessionItems.isEmpty()
        }
    }

    fun setOnSessionSelectedListener(listener: (SessionItem) -> Unit) {
        onSessionSelected = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SessionListDialog"
        
        fun newInstance(onSessionSelected: (SessionItem) -> Unit): SessionListDialog {
            return SessionListDialog().apply {
                setOnSessionSelectedListener(onSessionSelected)
            }
        }
    }
}
