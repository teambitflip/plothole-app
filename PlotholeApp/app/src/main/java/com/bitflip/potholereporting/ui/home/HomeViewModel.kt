package com.bitflip.potholereporting.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Pothole Reporting System \n by Team Bitflip"
    }
    val text: LiveData<String> = _text
}