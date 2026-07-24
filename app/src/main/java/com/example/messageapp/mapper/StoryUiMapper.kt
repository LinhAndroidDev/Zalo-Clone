package com.example.messageapp.mapper

import com.example.messageapp.domain.model.MusicTrack
import com.example.messageapp.domain.model.Story
import com.example.messageapp.domain.model.StoryRing
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.model.StoryItem
import com.example.messageapp.model.StoryRingItem

object StoryUiMapper {

    fun toUi(story: Story): StoryItem = StoryItem(
        id = story.id,
        authorId = story.authorId,
        authorName = story.authorName,
        authorAvatarUrl = story.authorAvatarUrl,
        mediaUrl = story.mediaUrl,
        mediaType = story.mediaType,
        createdAtMillis = story.createdAtMillis,
        expiresAtMillis = story.expiresAtMillis,
        privacy = story.privacy,
        visibleToUserIds = story.visibleToUserIds,
        musicTrackId = story.musicTrackId,
        musicName = story.musicName,
        musicArtist = story.musicArtist,
        musicAudioUrl = story.musicAudioUrl,
        musicImageUrl = story.musicImageUrl,
        viewedByMe = story.viewedByMe,
    )

    fun toUi(ring: StoryRing): StoryRingItem = StoryRingItem(
        authorId = ring.authorId,
        authorName = ring.authorName,
        authorAvatarUrl = ring.authorAvatarUrl,
        stories = ring.stories.map { toUi(it) },
        hasUnseen = ring.hasUnseen,
        isMe = ring.isMe,
    )

    fun toUiTracks(tracks: List<MusicTrack>): List<MusicTrackItem> = tracks.map {
        MusicTrackItem(
            id = it.id,
            name = it.name,
            artistName = it.artistName,
            audioUrl = it.audioUrl,
            imageUrl = it.imageUrl,
            durationSeconds = it.durationSeconds,
        )
    }

    fun toDomain(track: MusicTrackItem): MusicTrack = MusicTrack(
        id = track.id,
        name = track.name,
        artistName = track.artistName,
        audioUrl = track.audioUrl,
        imageUrl = track.imageUrl,
        durationSeconds = track.durationSeconds,
    )
}
