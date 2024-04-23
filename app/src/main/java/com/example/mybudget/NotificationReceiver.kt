package com.example.mybudget

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelID = intent.getStringExtra("channelID")
        /*val notificationID = intent.getStringExtra("notificationID")*/
        val placeName = intent.getStringExtra("placeName")
       /* val time = intent.getStringExtra("time")
        val date = intent.getIntExtra("date", 0)*/
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val notification:Notification = NotificationCompat.Builder(context, channelID!!)
            .setSmallIcon(R.drawable.piggybank_18)
            .setContentTitle(
                when (channelID){
                    "PLAN" -> "Не забывайте про запланированные траты!"
                    else ->""
                }
            )
            .setContentText(
                when (channelID){
                    "PLAN" -> "Трата по категории $placeName приближается!"
                    else ->""
                }
            )
            .build()
        Log.e("CheckNotification", "Yes")
        notificationManager.notify(42, notification)
    }
}