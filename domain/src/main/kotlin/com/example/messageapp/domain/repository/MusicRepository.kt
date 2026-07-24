package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.MusicTrackPage

interface MusicRepository {
    suspend fun loadJamendoTracks(offset: Int, limit: Int): Result<MusicTrackPage>
}
