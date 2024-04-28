package com.example.mybudget

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.start_pages.CategoryBeginWithKey
import com.example.mybudget.start_pages.Constants
import com.example.mybudget.start_pages._CategoryBegin
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelID = intent.getStringExtra("channelID")
        val placeId = intent.getStringExtra("placeId")
        val date = Calendar.getInstance()
        date.timeInMillis = intent.getLongExtra("date", 0)
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        if (channelID!=null&&placeId!=null){
            getName(channelID, placeId, date){
            val notification:Notification = NotificationCompat.Builder(context, channelID!!)
                .setSmallIcon(R.drawable.piggybank_18)
                .setContentTitle(
                    when (channelID){
                        "PLAN" -> "Не забывайте про запланированные траты!"
                        else ->""
                    }
                )
                .setContentText(it)
                .build()
                Log.e("CheckNotification", "Yes")
                notificationManager.notify(42, notification)
            }
        }
    }

    private fun getName(channelId:String,placeId:String, dateOfExpence:Calendar, callback: (String) -> Unit){
        when(channelId){
            Constants.CHANNEL_ID_PLAN->{
                Firebase.database.reference
                    .child("Users")
                    .child(Firebase.auth.currentUser!!.uid)
                    .child("Categories")
                    .child("Categories base")
                    .child(placeId).addListenerForSingleValueEvent(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(_CategoryBegin::class.java)?.let {
                                callback("Трата по категории ${it.name} приближается!")
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}

                    })
            }
            Constants.CHANNEL_ID_GOAL->{
                Firebase.database.reference
                    .child("Users")
                    .child(Firebase.auth.currentUser!!.uid)
                    .child("Goals")
                    .child(placeId).addListenerForSingleValueEvent(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(GoalItem::class.java)?.let {
                                callback("Вдохновляйтесь и действуйте! ${it.name} - ваша цель ждет вас!")
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }
}