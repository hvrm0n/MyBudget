package com.example.mybudget

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.Navigation
import com.example.mybudget.start_pages.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ExchangeRateManager {
    private const val PREF_NAME = "exchange_rate_data"

    fun saveExchangeRateResponse(context: Context, response: ExchangeRateResponse) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val responseJson = Gson().toJson(response)
        editor.putString("exchange_rate_data", responseJson)
        editor.apply()
    }

    fun getExchangeRateResponse(context: Context): ExchangeRateResponse? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("exchange_rate_data", null)
        return Gson().fromJson(responseJson, ExchangeRateResponse::class.java)
    }

    fun request(table: DatabaseReference, auth: FirebaseAuth, context:Context, scope:LifecycleCoroutineScope,view:View, activity: Activity, start:Boolean){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").child("currency").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val currency = snapshot.value
                    val savedresponse = getExchangeRateResponse(context)
                    Log.e(Constants.TAG_CONVERT, savedresponse.toString())
                    if(savedresponse==null || currentTime().after(parseDateFromString(savedresponse.timeNextUpdateUtc))){
                        scope.launch(Dispatchers.IO) {
                            try{
                                val response = RetrofitClient.exhangeRateApi.getExhangeRates(currency.toString())
                                if(response.result == "success"){
                                    saveExchangeRateResponse(context, response)
                                } else Log.e(Constants.TAG_CONVERT, response.baseCode)
                            } catch (e:Exception){
                                Log.e(Constants.TAG_CONVERT, e.message.toString())
                            }
                            if(start){
                            withContext(Dispatchers.Main) {
                                Navigation.findNavController(view).navigate(R.id.action_enterFragment_to_homePageActivity).also { activity.finish() }
                            }}
                        }
                    } else if (start){
                        Navigation.findNavController(view).navigate(R.id.action_enterFragment_to_homePageActivity).also { activity.finish() }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun currentTime() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
    private fun parseDateFromString(dateString: String): Date {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.parse(dateString)!!
    }
}