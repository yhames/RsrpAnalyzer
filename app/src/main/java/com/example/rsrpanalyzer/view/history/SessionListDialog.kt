package com.example.rsrpanalyzer.view.history

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.data.db.DatabaseProvider
import com.example.rsrpanalyzer.data.db.SignalSessionEntity
import com.example.rsrpanalyzer.databinding.DialogSessionListBinding
import com.example.rsrpanalyzer.util.CsvHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionListDialog : DialogFragment() {

    private var _binding: DialogSessionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionHistoryAdapter: SessionHistoryAdapter
    private var onSessionSelected: ((SessionItem) -> Unit)? = null
    private var currentExportSessionId: Long? = null

    // CSV 내보내기를 위한 파일 생성 launcher
    private val createCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { exportSessionToCsv(it) }
    }

    // CSV 불러오기를 위한 파일 선택 launcher
    private val pickCsvLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importSessionFromCsv(it) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSessionListBinding.inflate(layoutInflater)

        setupRecyclerView()
        setupImportButton()
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
            onSessionDownload = { sessionItem ->
                startExportSession(sessionItem)
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

    private fun setupImportButton() {
        binding.btnImportCsv.setOnClickListener {
            pickCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
        }
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

    private fun startExportSession(sessionItem: SessionItem) {
        currentExportSessionId = sessionItem.id
        val fileName =
            "${sessionItem.sessionName}_${formatDateForFilename(sessionItem.createdAt)}.csv"
        createCsvLauncher.launch(fileName)
    }

    private fun exportSessionToCsv(uri: Uri) {
        val sessionId = currentExportSessionId ?: return

        lifecycleScope.launch {
            try {
                val database = DatabaseProvider.getDatabase(requireContext())
                val records = withContext(Dispatchers.IO) {
                    database.signalRecordDao().findAllBySessionId(sessionId)
                }

                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    CsvHelper.exportToCsv(records, outputStream)
                }

                // 파일명 추출
                val fileName = getFileNameFromUri(uri) ?: "파일"

                Toast.makeText(
                    requireContext(),
                    getString(R.string.session_export_success, fileName),
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.session_export_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                currentExportSessionId = null
            }
        }
    }

    private fun importSessionFromCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        CsvHelper.importFromCsv(inputStream)
                    } ?: CsvHelper.CsvImportResult.Error("파일을 열 수 없습니다.")
                }

                when (result) {
                    is CsvHelper.CsvImportResult.Success -> {
                        showSessionNameInputDialog(result.records)
                    }

                    is CsvHelper.CsvImportResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.session_import_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSessionNameInputDialog(records: List<com.example.rsrpanalyzer.data.db.SignalRecordEntity>) {
        val defaultName = getString(
            R.string.session_import_default_name,
            formatDateForFilename(System.currentTimeMillis())
        )
        val editText = EditText(requireContext()).apply {
            setText(defaultName)
            setSingleLine()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_import_session_name)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val sessionName = editText.text.toString().trim()
                if (sessionName.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.session_name_empty_error,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    saveImportedSession(sessionName, records)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveImportedSession(
        sessionName: String,
        records: List<com.example.rsrpanalyzer.data.db.SignalRecordEntity>
    ) {
        lifecycleScope.launch {
            try {
                val database = DatabaseProvider.getDatabase(requireContext())

                // 중복 이름 체크
                val existingSession = withContext(Dispatchers.IO) {
                    database.signalSessionDao().findSessionByName(sessionName)
                }

                if (existingSession != null) {
                    Toast.makeText(
                        requireContext(),
                        R.string.session_name_duplicate_error,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // 새 세션 생성
                val sessionId = withContext(Dispatchers.IO) {
                    val newSession = SignalSessionEntity(
                        sessionName = sessionName,
                        createdAt = System.currentTimeMillis()
                    )
                    database.signalSessionDao().save(newSession)
                }

                // 기록 저장
                withContext(Dispatchers.IO) {
                    records.forEach { record ->
                        database.signalRecordDao().save(record.copy(sessionId = sessionId))
                    }
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.session_import_success, records.size),
                    Toast.LENGTH_SHORT
                ).show()

                // 목록 새로고침
                loadSessionHistory()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.session_import_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun formatDateForFilename(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
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
