package com.example.rsrpanalyzer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecordViewModel : ViewModel() {
    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> get() = _isRecording

    private val _sessionName = MutableLiveData<String?>()
    val sessionName: LiveData<String?> get() = _sessionName

    fun updateRecordingStatus(value: Boolean) {
        _isRecording.value = value
        if (!value) {
            clearSessionName()
        }
    }

    fun updateSessionName(name: String) {
        _sessionName.value = name
    }

    private fun clearSessionName() {
        _sessionName.value = null
    }

    fun isRecording(): Boolean {
        return isRecording.value ?: false
    }
}
