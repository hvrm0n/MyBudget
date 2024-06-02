package com.example.mybudget.start_pages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class BasicBudgetViewModel : ViewModel() {
    val name = MutableLiveData<String>()
    val savings = MutableLiveData<String>()
    val type = MutableLiveData<Int>()

    fun createBaseBudget(nameBudget:String, savings:String, type:String){
        Firebase.database.reference.child("Users").child(Firebase.auth.currentUser!!.uid).child("Budgets").child("Base budget").child("name").setValue(nameBudget)
        Firebase.database.reference.child("Users").child(Firebase.auth.currentUser!!.uid).child("Budgets").child("Base budget").child("amount").setValue(savings)
        Firebase.database.reference.child("Users").child(Firebase.auth.currentUser!!.uid).child("Budgets").child("Base budget").child("type").setValue(type)
        Firebase.database.reference.child("Users").child(Firebase.auth.currentUser!!.uid).child("Budgets").child("Base budget").child("count").setValue(0)
    }
}