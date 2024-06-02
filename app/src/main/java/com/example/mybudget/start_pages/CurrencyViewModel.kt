package com.example.mybudget.start_pages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class CurrencyViewModel : ViewModel() {

    private val _selection = MutableLiveData<Triple<String, String, String>?>()
    val selection: LiveData<Triple<String, String, String>?> get() = _selection

    fun updateSelection(newData: Triple<String, String, String>?){
        _selection.value = newData
    }

    fun addCurency(){
        Firebase.database.reference.child("Users").child(Firebase.auth.currentUser!!.uid).child("Budgets").child("Base budget").child("currency").setValue(_selection.value?.first.toString())
    }
}