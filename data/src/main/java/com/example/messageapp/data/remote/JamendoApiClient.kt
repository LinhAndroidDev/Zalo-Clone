package com.example.messageapp.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object JamendoApiClient {
    private const val BASE_URL = "https://api.jamendo.com/v3.0/"
    const val CLIENT_ID = "c488f858"

    val api: JamendoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JamendoApiService::class.java)
    }
}
