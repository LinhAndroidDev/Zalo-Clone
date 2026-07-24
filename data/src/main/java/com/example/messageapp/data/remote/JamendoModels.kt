package com.example.messageapp.data.remote

import com.google.gson.annotations.SerializedName

data class JamendoTracksResponse(
    val headers: JamendoHeaders? = null,
    val results: List<JamendoTrackDto> = emptyList(),
)

data class JamendoHeaders(
    val status: String? = null,
    @SerializedName("results_count") val resultsCount: Int = 0,
    val next: String? = null,
)

data class JamendoTrackDto(
    val id: String = "",
    val name: String = "",
    @SerializedName("artist_name") val artistName: String = "",
    val audio: String = "",
    val image: String = "",
    val duration: Int = 0,
)
