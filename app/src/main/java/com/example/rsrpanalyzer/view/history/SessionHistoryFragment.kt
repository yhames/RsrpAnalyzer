package com.example.rsrpanalyzer.view.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.data.db.DatabaseProvider
import com.example.rsrpanalyzer.databinding.FragmentSessionHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionHistoryFragment : Fragment(R.layout.fragment_session_history) {

    private var _binding: FragmentSessionHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionHistoryAdapter: SessionHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadSessionHistory()
    }

    private fun setupRecyclerView() {
        sessionHistoryAdapter = SessionHistoryAdapter { sessionItem ->
            onSessionClick(sessionItem)
        }
        binding.rvSessionHistory.adapter = sessionHistoryAdapter
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

    private fun onSessionClick(sessionItem: SessionItem) {
        // TODO: Navigate to session detail view
        // 세션 상세 화면으로 이동 (다음 단계에서 구현)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
