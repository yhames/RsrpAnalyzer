package com.example.rsrpanalyzer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecordViewModel() : ViewModel() {
    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> get() = _isRecording

    fun updateRecordingStatus(value: Boolean) {
        _isRecording.value = value
    }

    fun isRecording(): Boolean {
        return isRecording.value ?: false
    }
}
