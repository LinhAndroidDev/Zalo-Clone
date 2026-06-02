package com.example.messageapp.data.remote

import com.example.messageapp.data.remote.request.MessageRequest
import com.example.messageapp.data.legacy.AccessToken
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
        @Body message: MessageRequest,
        @Header("Authorization") accessToken: String = "Bearer ${AccessToken.getAccessToken()}"
    ): Call<MessageRequest>
}