package com.example.mybudget.start_pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mybudget.R

object Constants{
    const val TAG_SIGNUP = "SignUpMyBudget"
    const val TAG_LOGIN = "LogInMyBudget"
    const val TAG_USER = "UserMyBudget"
    const val TAG_GOOGLE = "GoogleMyBudget"
}

class StartActivity:AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.start_activity)

    }
}