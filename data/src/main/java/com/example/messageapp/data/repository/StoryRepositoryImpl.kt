package com.example.messageapp.data.repository

import androidx.core.net.toUri
import com.example.messageapp.data.DataContextHolder
import com.example.messageapp.data.firestore.Story as FsStory
import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.domain.model.StoryRing
import com.example.messageapp.domain.repository.StoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepositoryImpl @Inject constructor() : StoryRepository {

    override fun observeStoryRings(
        userId: String,
        onRings: (List<StoryRing>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = FireBaseInstance.observeStoryRings(
        userId = userId,
        onRings = { stories -> onRings(groupIntoRings(stories, userId)) },
        onError = onError,
    )

    override fun createStory(
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
        musicStickerX: Float,
        musicStickerY: Float,
        mediaScale: Float,
        mediaRotation: Float,
        mediaTranslationX: Float,
        mediaTranslationY: Float,
        onProgress: (Float) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val context = DataContextHolder.appContext
        FireBaseInstance.createStory(
            context = context,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            mediaUri = localMediaUriString.toUri(),
            privacy = EntityMapper.privacyToFirestore(privacy),
            visibleToUserIds = visibleToUserIds,
            musicTrackId = musicTrackId,
            musicName = musicName,
            musicArtist = musicArtist,
            musicAudioUrl = musicAudioUrl,
            musicImageUrl = musicImageUrl,
            musicStickerX = musicStickerX,
            musicStickerY = musicStickerY,
            mediaScale = mediaScale,
            mediaRotation = mediaRotation,
            mediaTranslationX = mediaTranslationX,
            mediaTranslationY = mediaTranslationY,
            onProgress = onProgress,
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun markStoryViewed(
        storyId: String,
        viewerId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.markStoryViewed(storyId, viewerId, onSuccess, onFailure)
    }

    override fun getStoriesForAuthors(
        userId: String,
        authorIds: List<String>,
        onSuccess: (List<StoryRing>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.getStoriesForAuthors(
            userId = userId,
            authorIds = authorIds,
            onSuccess = { stories ->
                onSuccess(groupIntoRings(stories, userId))
            },
            onFailure = onFailure,
        )
    }

    private fun groupIntoRings(stories: List<FsStory>, myUserId: String): List<StoryRing> {
        if (stories.isEmpty()) return emptyList()
        return stories
            .groupBy { it.authorId }
            .map { (authorId, authorStories) ->
                val meta = authorStories.first()
                EntityMapper.toDomainRing(
                    authorId = authorId,
                    authorName = meta.authorName,
                    authorAvatarUrl = meta.authorAvatarUrl,
                    stories = authorStories,
                    myUserId = myUserId,
                )
            }
            .sortedWith(compareBy<StoryRing>({ !it.isMe }, { it.authorName.lowercase() }))
    }
}
