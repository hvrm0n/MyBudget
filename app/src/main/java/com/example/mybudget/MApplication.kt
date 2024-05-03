package com.example.mybudget

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.database.database

class MApplication : Application(){

    override fun onCreate() {
        super.onCreate()
        Firebase.database.setPersistenceEnabled(true)
    }
}
