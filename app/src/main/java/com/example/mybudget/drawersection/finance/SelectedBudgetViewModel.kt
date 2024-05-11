package com.example.mybudget.drawersection.finance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SelectedBudgetViewModel : ViewModel(){
    private val _selectedBudget = MutableLiveData<List<String>>()
    val selectedBudget: LiveData<List<String>> get() = _selectedBudget

    private val _autoTransaction = MutableLiveData<Boolean>()
    val autoTransaction: LiveData<Boolean> get() = _autoTransaction

    private val _textNotification = MutableLiveData<Boolean>()
    val textNotification: LiveData<Boolean> get() = _textNotification

    private val _allNotification = MutableLiveData<Boolean>()
    val allNotification: LiveData<Boolean> get() = _allNotification

     fun updateSelectionData(newData: List<String>) {
        _selectedBudget.value = newData
     }

    fun updateAutoTransactionFlag(newData:Boolean) {
        _autoTransaction.value = newData
    }

    fun updateTextNotification(newData:Boolean) {
        _textNotification.value = newData
    }

    fun updateAllNotification(newData:Boolean) {
        _allNotification.value = newData
    }
}