package com.example.messageapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface JamendoApiService {
    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): JamendoTracksResponse

    @GET
    suspend fun getTracksByUrl(@Url url: String): JamendoTracksResponse
}
