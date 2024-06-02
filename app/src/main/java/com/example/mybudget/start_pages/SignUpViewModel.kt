package com.example.mybudget.start_pages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignUpViewModel: ViewModel() {
    val emailText = MutableLiveData<String>()
    val passwordText = MutableLiveData<String>()
    val hintEmailState = MutableLiveData<Int>()
    val hintPasswordState = MutableLiveData<Int>()

}