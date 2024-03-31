package com.example.mybudget.start_pages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BasicBudgetViewModel : ViewModel() {
    val name = MutableLiveData<String>()
    val savings = MutableLiveData<String>()
    val type = MutableLiveData<Int>()
}