package com.example.messageapp.domain.usecase.story

import com.example.messageapp.domain.model.MusicTrackPage
import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.domain.model.StoryRing
import com.example.messageapp.domain.repository.MusicRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.StoryRepository
import com.example.messageapp.domain.repository.UserRepository
import javax.inject.Inject

class ObserveStoryRingsUseCase @Inject constructor(
    private val storyRepository: StoryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        onRings: (List<StoryRing>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = storyRepository.observeStoryRings(
        userId = sessionRepository.getAuth(),
        onRings = onRings,
        onError = onError,
    )
}

class CreateStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(
        localMediaUriString: String,
        privacy: StoryPrivacy,
        visibleToUserIds: List<String>,
        musicTrackId: String = "",
        musicName: String = "",
        musicArtist: String = "",
        musicAudioUrl: String = "",
        musicImageUrl: String = "",
        musicStickerX: Float = 0.5f,
        musicStickerY: Float = 0.5f,
        mediaScale: Float = 1f,
        mediaRotation: Float = 0f,
        mediaTranslationX: Float = 0f,
        mediaTranslationY: Float = 0f,
        onProgress: (Float) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val userId = sessionRepository.getAuth()
        if (userId.isBlank()) {
            onFailure("Error")
            return
        }
        if (privacy == StoryPrivacy.CUSTOM && visibleToUserIds.isEmpty()) {
            onFailure("Chọn ít nhất một người bạn")
            return
        }
        userRepository.getUserById(
            userId = userId,
            onSuccess = { user ->
                storyRepository.createStory(
                    authorId = userId,
                    authorName = user.name.ifBlank { sessionRepository.getNameUser() },
                    authorAvatarUrl = user.avatar,
                    localMediaUriString = localMediaUriString,
                    privacy = privacy,
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
                    onSuccess = { onSuccess() },
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class MarkStoryViewedUseCase @Inject constructor(
    private val storyRepository: StoryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(storyId: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        storyRepository.markStoryViewed(
            storyId = storyId,
            viewerId = sessionRepository.getAuth(),
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class GetStoriesForViewerUseCase @Inject constructor(
    private val storyRepository: StoryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        authorIds: List<String>,
        onSuccess: (List<StoryRing>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val userId = sessionRepository.getAuth()
        val ids = (listOf(userId) + authorIds).filter { it.isNotBlank() }.distinct()
        storyRepository.getStoriesForAuthors(
            userId = userId,
            authorIds = ids,
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class LoadJamendoTracksUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(offset: Int = 0, limit: Int = 20): Result<MusicTrackPage> =
        musicRepository.loadJamendoTracks(offset, limit)
}
