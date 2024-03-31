package com.example.mybudget.drawersection.finance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val dataToPass = MutableLiveData<Triple<String, String, String>>()
}