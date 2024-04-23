package com.example.mybudget

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object RetrofitClient {
    private const val BASE_URL = "https://v6.exchangerate-api.com/v6/7bb7737d9e8bdbee9acf875d/"

    val retrofit:Retrofit by lazy{
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val exhangeRateApi:ExchangeRateApi by lazy{
        retrofit.create(ExchangeRateApi::class.java)
    }
}