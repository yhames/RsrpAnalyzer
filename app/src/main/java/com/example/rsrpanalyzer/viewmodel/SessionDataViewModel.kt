package com.example.rsrpanalyzer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rsrpanalyzer.data.db.DatabaseProvider
import com.example.rsrpanalyzer.data.db.SignalRecordEntity
import com.example.rsrpanalyzer.view.history.SessionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionDataViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = DatabaseProvider.getDatabase(application)
    
    private val _selectedSession = MutableLiveData<SessionItem?>()
    val selectedSession: LiveData<SessionItem?> = _selectedSession
    
    private val _sessionRecords = MutableLiveData<List<SignalRecordEntity>>()
    val sessionRecords: LiveData<List<SignalRecordEntity>> = _sessionRecords
    
    private val _isHistoryMode = MutableLiveData<Boolean>(false)
    val isHistoryMode: LiveData<Boolean> = _isHistoryMode
    
    fun selectSession(sessionItem: SessionItem) {
        _selectedSession.value = sessionItem
        loadSessionRecords(sessionItem.id)
    }
    
    fun clearSession() {
        _selectedSession.value = null
        _sessionRecords.value = emptyList()
    }
    
    fun setHistoryMode(isHistory: Boolean) {
        _isHistoryMode.value = isHistory
        if (!isHistory) {
            clearSession()
        }
    }
    
    private fun loadSessionRecords(sessionId: Long) {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) {
                database.signalRecordDao().findAllBySessionId(sessionId)
            }
            _sessionRecords.value = records
        }
    }
}
