package com.example.messageapp.domain.model

enum class StoryPrivacy {
    EVERYONE,
    FRIENDS,
    CUSTOM,
}

enum class StoryMediaType {
    IMAGE,
    VIDEO,
}

data class Story(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val mediaUrl: String,
    val mediaType: StoryMediaType,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val privacy: StoryPrivacy,
    val visibleToUserIds: List<String> = emptyList(),
    val musicTrackId: String = "",
    val musicName: String = "",
    val musicArtist: String = "",
    val musicAudioUrl: String = "",
    val musicImageUrl: String = "",
    val viewedByMe: Boolean = false,
)

data class StoryRing(
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val stories: List<Story>,
    val hasUnseen: Boolean,
    val isMe: Boolean,
)

data class MusicTrack(
    val id: String,
    val name: String,
    val artistName: String,
    val audioUrl: String,
    val imageUrl: String,
    val durationSeconds: Int,
)

data class MusicTrackPage(
    val tracks: List<MusicTrack>,
    val nextOffset: Int?,
)
