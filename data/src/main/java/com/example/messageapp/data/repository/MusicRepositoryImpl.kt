package com.example.messageapp.data.repository

import com.example.messageapp.data.remote.JamendoApiClient
import com.example.messageapp.domain.model.MusicTrack
import com.example.messageapp.domain.model.MusicTrackPage
import com.example.messageapp.domain.repository.MusicRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor() : MusicRepository {

    override suspend fun loadJamendoTracks(offset: Int, limit: Int): Result<MusicTrackPage> {
        return runCatching {
            val response = JamendoApiClient.api.getTracks(
                clientId = JamendoApiClient.CLIENT_ID,
                limit = limit,
                offset = offset,
            )
            val tracks = response.results.map { dto ->
                MusicTrack(
                    id = dto.id,
                    name = dto.name,
                    artistName = dto.artistName,
                    audioUrl = dto.audio,
                    imageUrl = dto.image,
                    durationSeconds = dto.duration,
                )
            }
            val nextOffset = response.headers?.next?.let { parseOffsetFromNext(it) }
            MusicTrackPage(tracks = tracks, nextOffset = nextOffset)
        }
    }

    private fun parseOffsetFromNext(nextUrl: String): Int? {
        val match = Regex("offset=(\\d+)").find(nextUrl) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }
}
