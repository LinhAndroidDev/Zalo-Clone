package com.example.messageapp.remote

import com.example.messageapp.remote.request.Notification
import com.example.messageapp.utils.AccessToken
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @POST("zalo-clone-45246/messages:send")
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
        )
    fun sendMessage(
        @Body message: Notification,
        @Header("Authorization") accessToken: String = "Bearer ${AccessToken.getAccessToken()}"
    ): Call<Notification>
}