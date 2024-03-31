package com.example.mybudget.start_pages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CurrencyViewModel : ViewModel() {
    val selection = MutableLiveData<Triple<String, String, String>?>()
}