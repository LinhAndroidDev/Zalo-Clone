package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.domain.model.StoryRing

interface StoryRepository {
    fun observeStoryRings(
        userId: String,
        onRings: (List<StoryRing>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit

    fun createStory(
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        localMediaUriString: String,
        privacy: StoryPrivacy,
        visibleToUserIds: List<String>,
        musicTrackId: String,
        musicName: String,
        musicArtist: String,
        musicAudioUrl: String,
        musicImageUrl: String,
        musicStickerX: Float = 0.5f,
        musicStickerY: Float = 0.5f,
        mediaScale: Float = 1f,
        mediaRotation: Float = 0f,
        mediaTranslationX: Float = 0f,
        mediaTranslationY: Float = 0f,
        onProgress: (Float) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    )

    fun markStoryViewed(storyId: String, viewerId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)

    fun getStoriesForAuthors(
        userId: String,
        authorIds: List<String>,
        onSuccess: (List<StoryRing>) -> Unit,
        onFailure: (String) -> Unit,
    )

    fun updateStoryPrivacy(
        storyId: String,
        authorId: String,
        privacy: StoryPrivacy,
        visibleToUserIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )

    fun deleteStory(
        storyId: String,
        authorId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )
}
