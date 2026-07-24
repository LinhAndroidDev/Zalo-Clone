package com.example.messageapp.viewmodel

import android.net.Uri
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.domain.usecase.story.CreateStoryUseCase
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.model.StoryMediaTransform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CreateStoryViewModel @Inject constructor(
    private val createStoryUseCase: CreateStoryUseCase,
) : BaseViewModel() {

    private val _mediaUri = MutableStateFlow<Uri?>(null)
    val mediaUri = _mediaUri.asStateFlow()

    private val _isVideo = MutableStateFlow(false)
    val isVideo = _isVideo.asStateFlow()

    private val _privacy = MutableStateFlow(StoryPrivacy.EVERYONE)
    val privacy = _privacy.asStateFlow()

    private val _visibleToUserIds = MutableStateFlow<List<String>>(emptyList())
    val visibleToUserIds = _visibleToUserIds.asStateFlow()

    private val _selectedMusic = MutableStateFlow<MusicTrackItem?>(null)
    val selectedMusic = _selectedMusic.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    private val _published = MutableStateFlow(false)
    val published = _published.asStateFlow()

    fun setMedia(uri: Uri, video: Boolean) {
        _mediaUri.value = uri
        _isVideo.value = video
    }

    fun clearMedia() {
        _mediaUri.value = null
        _isVideo.value = false
    }

    fun setPrivacy(privacy: StoryPrivacy) {
        _privacy.value = privacy
        if (privacy != StoryPrivacy.CUSTOM) {
            _visibleToUserIds.value = emptyList()
        }
    }

    fun setVisibleFriends(userIds: List<String>) {
        _visibleToUserIds.value = userIds
    }

    fun setMusic(track: MusicTrackItem?) {
        _selectedMusic.value = track
    }

    fun publishStory(
        musicStickerX: Float = 0.5f,
        musicStickerY: Float = 0.5f,
        mediaTransform: StoryMediaTransform = StoryMediaTransform.Default,
    ) {
        val uri = _mediaUri.value ?: run {
            showError("Chọn ảnh hoặc video")
            return
        }
        if (_privacy.value == StoryPrivacy.CUSTOM && _visibleToUserIds.value.isEmpty()) {
            showError("Chọn ít nhất một người bạn")
            return
        }
        val music = _selectedMusic.value
        _uploadProgress.value = 0f
        createStoryUseCase(
            localMediaUriString = uri.toString(),
            privacy = _privacy.value,
            visibleToUserIds = _visibleToUserIds.value,
            musicTrackId = music?.id.orEmpty(),
            musicName = music?.name.orEmpty(),
            musicArtist = music?.artistName.orEmpty(),
            musicAudioUrl = music?.audioUrl.orEmpty(),
            musicImageUrl = music?.imageUrl.orEmpty(),
            musicStickerX = musicStickerX,
            musicStickerY = musicStickerY,
            mediaScale = mediaTransform.scale,
            mediaRotation = mediaTransform.rotation,
            mediaTranslationX = mediaTransform.translationXNorm,
            mediaTranslationY = mediaTransform.translationYNorm,
            onProgress = { progress -> _uploadProgress.value = progress },
            onSuccess = {
                _uploadProgress.value = null
                _published.value = true
            },
            onFailure = { msg ->
                _uploadProgress.value = null
                showError(msg)
            },
        )
    }
}
