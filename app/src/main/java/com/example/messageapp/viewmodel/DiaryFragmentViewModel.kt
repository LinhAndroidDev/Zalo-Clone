package com.example.messageapp.viewmodel

import android.os.SystemClock
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DiaryFragmentViewModel @Inject constructor(
    private val shared: SharePreferenceRepository
) : BaseViewModel() {

    private val _user: MutableStateFlow<User?> = MutableStateFlow(null)
    val user = _user.asStateFlow()

    private val _diaryPosts = MutableStateFlow<List<DiaryPost>>(emptyList())
    val diaryPosts = _diaryPosts.asStateFlow()

    private var stopFeed: (() -> Unit)? = null
    private var lastDiaryFeedErrorAtMs = 0L

    fun getInfoUser() {
        val uid = shared.getAuth().ifBlank { return }
        FireBaseInstance.getInfoUser(uid) { u ->
            _user.value = u
        }
    }

    fun startDiaryFeed() {
        val uid = shared.getAuth().ifBlank { return }
        stopFeed?.invoke()
        stopFeed = FireBaseInstance.observeDiaryFeed(
            userId = uid,
            onPosts = { list -> _diaryPosts.value = list },
            onError = { msg ->
                // Tránh spam khi listener Firestore báo lỗi lặp (mạng / quyền / v.v.)
                val now = SystemClock.elapsedRealtime()
                if (now - lastDiaryFeedErrorAtMs >= 4_000L) {
                    lastDiaryFeedErrorAtMs = now
                    showError(msg)
                }
            }
        )
    }

    fun toggleDiaryPostLike(post: DiaryPost) {
        val uid = shared.getAuth().ifBlank { return }
        FireBaseInstance.toggleDiaryPostLike(
            postId = post.id,
            userId = uid,
            currentlyLiked = post.likedByMe,
            success = {},
            failure = { showError(it) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopFeed?.invoke()
        stopFeed = null
    }
}
