package com.example.mybudget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.security.MessageDigest
import java.util.Calendar
import kotlin.math.abs

object BudgetNotificationManager {

     private fun stringToUniqueId(input: String): Int {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        val hexString = bytes.joinToString("") { "%02x".format(it) }
        return abs(hexString.hashCode() and Int.MAX_VALUE)
    }

    fun setAutoTransaction(context: Context, id:String, placeId:String, year: Int, month:Int, dateOfExpence: Calendar, type:String){
        val transactionIntent = Intent(context, TransactionReceiver::class.java).apply {
            putExtra("categoryId", placeId)
            putExtra("year", year.toString())
            putExtra("month", month.toString())
            putExtra("planId", id)
            putExtra("type", type)
        }
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, dateOfExpence.get(Calendar.YEAR))
        calendar.set(Calendar.MONTH, dateOfExpence.get(Calendar.MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, dateOfExpence.get(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY,0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val pendingIntent = PendingIntent.getBroadcast(context, stringToUniqueId(id)+1, transactionIntent,  PendingIntent.FLAG_MUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    fun cancelAutoTransaction(context: Context, id: String){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TransactionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, stringToUniqueId(id)+1, intent, PendingIntent.FLAG_MUTABLE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

     fun cancelAlarmManager(context: Context, id:String, deletePrefs:Boolean = true){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, stringToUniqueId(id), intent, PendingIntent.FLAG_MUTABLE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            if (deletePrefs) deleteSharedPreference(id, context)
        }
    }

     fun notification(context: Context, channelID:String, id:String, placeId:String?=null, time:String, dateOfExpence:Calendar, periodOfNotification:String){
         val periods = context.resources.getStringArray(R.array.periodicity)
         val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
             putExtra("channelID", channelID)
             putExtra("placeId", placeId?:id)
             putExtra("date", dateOfExpence.timeInMillis)
             putExtra("deletePrefs", when(periodOfNotification){
                 periods[1], periods[2], periods[3]->true
                 else ->false
             })
        }

         Log.e("EnterNotification", channelID)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, dateOfExpence.get(Calendar.YEAR))
        calendar.set(Calendar.MONTH, dateOfExpence.get(Calendar.MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, dateOfExpence.get(Calendar.DAY_OF_MONTH))
         if (time.isNotEmpty()) {
             calendar.set(Calendar.HOUR_OF_DAY, time.split(":")[0].toInt())
             calendar.set(Calendar.MINUTE, time.split(":")[1].toInt())
         }
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val today = Calendar.getInstance()
         if (time.isNotEmpty()) {
            today.set(Calendar.HOUR_OF_DAY,time.split(":")[0].toInt())
            today.set(Calendar.MINUTE,time.split(":")[1].toInt())
         }
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val pendingIntent = PendingIntent.getBroadcast(context, stringToUniqueId(id), notificationIntent,  PendingIntent.FLAG_MUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

         updateSharedPreference(id, time, periodOfNotification, context)

         when(periodOfNotification){
             periods[0]-> deleteSharedPreference(id, context)
             periods[1]-> alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis - (24 * 60 * 60 * 1000), pendingIntent)
             periods[2]-> alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis - (3 * 24 * 60 * 60 * 1000), pendingIntent)
             periods[3]-> alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis - (7 * 24 * 60 * 60 * 1000), pendingIntent)
             periods[4]-> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, today.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
             periods[5]-> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, today.timeInMillis, AlarmManager.INTERVAL_DAY*7, pendingIntent)
             periods[6]-> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, today.timeInMillis, AlarmManager.INTERVAL_DAY*30, pendingIntent)
             periods[7]-> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, today.timeInMillis, AlarmManager.INTERVAL_DAY*365, pendingIntent)
        }
    }

    private fun updateSharedPreference(id:String, time:String, periodOfNotification: String, context: Context){
        val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(id, "$periodOfNotification|$time")
        editor.apply()
    }

    fun deleteSharedPreference(id:String, context: Context){
        val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(id)
        editor.apply()
    }
}