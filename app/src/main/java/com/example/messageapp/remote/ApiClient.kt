package com.example.messageapp.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var retrofit: Retrofit? = null

    private fun getApiChat(): Retrofit {
        if(retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl("https://fcm.googleapis.com/v1/projects/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    val api: ApiService? by lazy {
        getApiChat().create(ApiService::class.java)
    }
}