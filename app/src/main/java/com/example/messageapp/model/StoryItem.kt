package com.example.messageapp.model

import android.os.Parcelable
import com.example.messageapp.domain.model.StoryMediaType
import com.example.messageapp.domain.model.StoryPrivacy
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoryItem(
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
) : Parcelable

@Parcelize
data class StoryRingItem(
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val stories: List<StoryItem>,
    val hasUnseen: Boolean,
    val isMe: Boolean,
) : Parcelable

@Parcelize
data class MusicTrackItem(
    val id: String,
    val name: String,
    val artistName: String,
    val audioUrl: String,
    val imageUrl: String,
    val durationSeconds: Int,
) : Parcelable
