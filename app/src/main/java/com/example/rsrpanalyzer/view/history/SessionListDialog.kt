package com.example.rsrpanalyzer.view.history

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.data.db.DatabaseProvider
import com.example.rsrpanalyzer.data.db.SignalSessionEntity
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
        sessionHistoryAdapter = SessionHistoryAdapter(
            onSessionClick = { sessionItem ->
                onSessionSelected?.invoke(sessionItem)
                dismiss()
            },
            onSessionEdit = { sessionItem ->
                showEditSessionDialog(sessionItem)
            },
            onSessionDelete = { sessionItem ->
                showDeleteConfirmDialog(sessionItem)
            }
        )
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

    private fun showEditSessionDialog(sessionItem: SessionItem) {
        val editText = EditText(requireContext()).apply {
            setText(sessionItem.sessionName)
            setSingleLine()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_edit_title)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.session_name_empty_error,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    updateSessionName(sessionItem, newName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmDialog(sessionItem: SessionItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_delete_confirm_title)
            .setMessage(getString(R.string.session_delete_confirm_message, sessionItem.sessionName))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                deleteSession(sessionItem)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateSessionName(sessionItem: SessionItem, newName: String) {
        lifecycleScope.launch {
            val database = DatabaseProvider.getDatabase(requireContext())
            
            // 중복 이름 체크
            val existingSession = withContext(Dispatchers.IO) {
                database.signalSessionDao().findSessionByName(newName)
            }
            
            if (existingSession != null && existingSession.id != sessionItem.id) {
                Toast.makeText(
                    requireContext(),
                    R.string.session_name_duplicate_error,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // 세션 업데이트
            withContext(Dispatchers.IO) {
                val updatedSession = SignalSessionEntity(
                    id = sessionItem.id,
                    sessionName = newName,
                    createdAt = sessionItem.createdAt
                )
                database.signalSessionDao().update(updatedSession)
            }

            // 목록 새로고침
            loadSessionHistory()
        }
    }

    private fun deleteSession(sessionItem: SessionItem) {
        lifecycleScope.launch {
            val database = DatabaseProvider.getDatabase(requireContext())
            
            withContext(Dispatchers.IO) {
                database.signalSessionDao().deleteSessionById(sessionItem.id)
            }

            // 목록 새로고침
            loadSessionHistory()
        }
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
