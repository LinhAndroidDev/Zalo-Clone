package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.Story
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
}
