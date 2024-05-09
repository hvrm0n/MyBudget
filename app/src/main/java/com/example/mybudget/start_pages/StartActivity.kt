package com.example.mybudget.start_pages

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mybudget.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

object Constants{
    const val TAG_SIGNUP = "SignUpMyBudget"
    const val TAG_LOGIN = "LogInMyBudget"
    const val TAG_USER = "UserMyBudget"
    const val TAG_GOOGLE = "GoogleMyBudget"
    const val TAG_CONVERT = "ExchangeMyBudget"
    const val CHANNEL_ID_PLAN = "PLAN"
    const val CHANNEL_ID_GOAL = "GOAL"
    const val CHANNEL_ID_LOAN = "LOAN"
    const val CHANNEL_ID_SUB = "SUB"
}

class StartActivity:AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("exit")){
            Firebase.auth.signOut()
        }
        setContentView(R.layout.start_activity)
        createNotificationChannels()
    }

    private fun createNotificationChannels(){
        val channelPlan = NotificationChannel(
            Constants.CHANNEL_ID_PLAN,
            getString(R.string.plan_notification),
            NotificationManager.IMPORTANCE_DEFAULT)

        val channelGoal = NotificationChannel(
            Constants.CHANNEL_ID_GOAL,
            getString(R.string.goal_notification),
            NotificationManager.IMPORTANCE_HIGH)

        val channelLoan = NotificationChannel(
            Constants.CHANNEL_ID_LOAN,
            getString(R.string.loan_notification),
            NotificationManager.IMPORTANCE_HIGH)

        val channelSubscribe = NotificationChannel(
            Constants.CHANNEL_ID_SUB,
            getString(R.string.subscribe_notification),
            NotificationManager.IMPORTANCE_DEFAULT)

        val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(listOf(channelPlan, channelLoan, channelSubscribe, channelGoal))
    }
}