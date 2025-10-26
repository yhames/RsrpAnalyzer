package com.example.rsrpanalyzer.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignalViewModel : ViewModel() {
    private val _location = MutableLiveData<Location>()
    val location: LiveData<Location> = _location

    private val _rsrp = MutableLiveData<Int>()
    val rsrp: LiveData<Int> = _rsrp

    private val _rsrq = MutableLiveData<Int>()
    val rsrq: LiveData<Int> = _rsrq

    fun updateLocation(loc: Location) {
        _location.value = loc
    }

    fun updateSignal(rsrp: Int, rsrq: Int) {
        _rsrp.value = rsrp
        _rsrq.value = rsrq
    }
}