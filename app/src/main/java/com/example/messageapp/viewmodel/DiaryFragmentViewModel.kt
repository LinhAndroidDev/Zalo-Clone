package com.example.messageapp.viewmodel

import android.os.SystemClock
import android.util.Log
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.diary.GetDiaryAuthorUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryFeedUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryNotificationUnreadCountUseCase
import com.example.messageapp.domain.usecase.diary.SetDiaryPostReactionUseCase
import com.example.messageapp.mapper.DiaryUiMapper
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.EmotionType
import com.example.messageapp.domain.usecase.story.ObserveStoryRingsUseCase
import com.example.messageapp.mapper.StoryUiMapper
import com.example.messageapp.model.StoryRingItem
import com.example.messageapp.model.StoryViewerCache
import com.example.messageapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DiaryFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getDiaryAuthorUseCase: GetDiaryAuthorUseCase,
    private val observeDiaryFeedUseCase: ObserveDiaryFeedUseCase,
    private val setDiaryPostReactionUseCase: SetDiaryPostReactionUseCase,
    private val observeDiaryNotificationUnreadCountUseCase: ObserveDiaryNotificationUnreadCountUseCase,
    private val deleteDiaryPostUseCase: com.example.messageapp.domain.usecase.diary.DeleteDiaryPostUseCase,
    private val observeStoryRingsUseCase: ObserveStoryRingsUseCase,
) : BaseViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _diaryPosts = MutableStateFlow<List<DiaryPost>>(emptyList())
    val diaryPosts = _diaryPosts.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount = _unreadNotificationCount.asStateFlow()

    private val _storyRings = MutableStateFlow<List<StoryRingItem>>(emptyList())
    val storyRings = _storyRings.asStateFlow()

    private var stopFeed: (() -> Unit)? = null
    private var stopStoryRings: (() -> Unit)? = null
    private var stopUnread: (() -> Unit)? = null
    private var activeUserId: String? = null
    private var lastDiaryFeedErrorAtMs = 0L
    private var lastStoryRingsErrorAtMs = 0L

    init {
        ensureDataForCurrentUser()
    }

    fun ensureDataForCurrentUser() {
        val userId = sessionRepository.getAuth()
        if (userId.isBlank()) {
            stopAllObservers()
            clearState()
            activeUserId = null
            return
        }
        if (userId == activeUserId && stopFeed != null) return
        stopAllObservers()
        clearState()
        activeUserId = userId
        StoryViewerCache.clear()
        getInfoUser()
        startDiaryFeed()
        startStoryRings()
        startNotificationBadge()
    }

    private fun clearState() {
        _user.value = null
        _diaryPosts.value = emptyList()
        _unreadNotificationCount.value = 0
        _storyRings.value = emptyList()
    }

    private fun stopAllObservers() {
        stopFeed?.invoke()
        stopFeed = null
        stopStoryRings?.invoke()
        stopStoryRings = null
        stopUnread?.invoke()
        stopUnread = null
    }

    fun currentUserId(): String = sessionRepository.getAuth()

    fun getInfoUser() {
        sessionRepository.getAuth().ifBlank { return }
        getDiaryAuthorUseCase(onSuccess = { u ->
            _user.value = SocialUiMapper.toUi(u)
            _storyRings.value = buildStoryRingList(_storyRings.value)
        })
    }

    fun startDiaryFeed() {
        if (stopFeed != null) return
        sessionRepository.getAuth().ifBlank { return }
        stopFeed = observeDiaryFeedUseCase(
            onPosts = { list -> _diaryPosts.value = DiaryUiMapper.toUiPosts(list) },
            onError = { msg ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastDiaryFeedErrorAtMs >= 4_000L) {
                    lastDiaryFeedErrorAtMs = now
                    showError(msg)
                }
            },
        )
    }

    fun startStoryRings() {
        if (stopStoryRings != null) return
        sessionRepository.getAuth().ifBlank { return }
        stopStoryRings = observeStoryRingsUseCase(
            onRings = { rings ->
                _storyRings.value = buildStoryRingList(rings.map { StoryUiMapper.toUi(it) })
                StoryViewerCache.update(_storyRings.value)
            },
            onError = { msg ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastStoryRingsErrorAtMs >= 4_000L) {
                    lastStoryRingsErrorAtMs = now
                    Log.w("DiaryFragmentViewModel", "Story rings: $msg")
                }
            },
        )
    }

    private fun buildStoryRingList(rings: List<StoryRingItem>): List<StoryRingItem> {
        val myId = currentUserId()
        val user = _user.value
        val myRing = rings.find { it.isMe } ?: StoryRingItem(
            authorId = myId,
            authorName = user?.name.orEmpty(),
            authorAvatarUrl = user?.avatar.orEmpty(),
            stories = emptyList(),
            hasUnseen = false,
            isMe = true,
        )
        val others = rings.filter { !it.isMe && it.stories.isNotEmpty() }
        return listOf(myRing.copy(
            authorName = user?.name ?: myRing.authorName,
            authorAvatarUrl = user?.avatar ?: myRing.authorAvatarUrl,
        )) + others
    }

    fun ringAuthorIds(): Array<String> {
        val myId = currentUserId()
        val ids = _storyRings.value
            .filter { it.stories.isNotEmpty() }
            .map { it.authorId }
            .toMutableList()
        if (ids.none { it == myId }) {
            val myStories = _storyRings.value.firstOrNull { it.isMe }?.stories.orEmpty()
            if (myStories.isNotEmpty()) {
                ids.add(0, myId)
            }
        } else {
            ids.remove(myId)
            ids.add(0, myId)
        }
        return ids.toTypedArray()
    }

    fun startNotificationBadge() {
        if (stopUnread != null) return
        sessionRepository.getAuth().ifBlank { return }
        stopUnread = observeDiaryNotificationUnreadCountUseCase(
            onUpdate = { _unreadNotificationCount.value = it },
            onError = {},
        )
    }

    fun setDiaryPostReaction(post: DiaryPost, type: EmotionType) {
        setDiaryPostReactionUseCase(
            post = DiaryUiMapper.toDomain(post),
            reactionType = com.example.messageapp.domain.model.EmotionType.valueOf(type.name),
            onSuccess = {},
            onFailure = { showError(it) },
        )
    }

    fun toggleDiaryPostLike(post: DiaryPost) {
        val type = post.myReactionType ?: EmotionType.LIKE
        setDiaryPostReaction(post, type)
    }

    fun deleteDiaryPost(postId: String, onSuccess: () -> Unit) {
        deleteDiaryPostUseCase(
            postId = postId,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopAllObservers()
        activeUserId = null
    }
}
