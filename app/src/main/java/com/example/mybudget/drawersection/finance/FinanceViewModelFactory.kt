package com.example.mybudget.drawersection.finance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class FinanceViewModelFactory(private val table: DatabaseReference, private val auth: FirebaseAuth, private val context: Context):ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(
            table = table, 
            auth = auth,
            context = context) as T
    }
}