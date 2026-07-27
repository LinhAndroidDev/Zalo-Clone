package com.example.messageapp.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.domain.model.StoryRing
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.story.DeleteStoryUseCase
import com.example.messageapp.domain.usecase.story.GetStoriesForViewerUseCase
import com.example.messageapp.domain.usecase.story.MarkStoryViewedUseCase
import com.example.messageapp.domain.usecase.story.UpdateStoryPrivacyUseCase
import com.example.messageapp.mapper.StoryUiMapper
import com.example.messageapp.model.StoryItem
import com.example.messageapp.model.StoryRingItem
import com.example.messageapp.model.StoryViewerCache
import com.example.messageapp.model.StoryViewerPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val getStoriesForViewerUseCase: GetStoriesForViewerUseCase,
    private val markStoryViewedUseCase: MarkStoryViewedUseCase,
    private val updateStoryPrivacyUseCase: UpdateStoryPrivacyUseCase,
    private val deleteStoryUseCase: DeleteStoryUseCase,
) : BaseViewModel() {

    private val startAuthorId: String = savedStateHandle.get<String>("startAuthorId").orEmpty()
    private val ringAuthorIds: Array<String> =
        savedStateHandle.get<Array<String>>("ringAuthorIds") ?: emptyArray()

    private val _rings = MutableStateFlow<List<StoryRingItem>>(emptyList())
    val rings = _rings.asStateFlow()

    private val _pages = MutableStateFlow<List<StoryViewerPage>>(emptyList())
    val pages = _pages.asStateFlow()

    private val _startPageIndex = MutableStateFlow(0)
    val startPageIndex = _startPageIndex.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished = _finished.asStateFlow()

    init {
        loadRings()
    }

    fun currentUserId(): String = sessionRepository.getAuth()

    private fun loadRings() {
        val ids = if (ringAuthorIds.isNotEmpty()) ringAuthorIds.toList() else listOf(startAuthorId)
        getStoriesForViewerUseCase(
            authorIds = ids,
            onSuccess = { domainRings ->
                var ordered = orderRings(domainRings, ids).map { StoryUiMapper.toUi(it) }
                if (ordered.isEmpty() || ordered.all { it.stories.isEmpty() }) {
                    ordered = orderRingsFromCache(ids)
                }
                applyRings(ordered)
                _loading.value = false
                if (_pages.value.isEmpty()) {
                    _finished.value = true
                }
            },
            onFailure = { msg ->
                _loading.value = false
                Log.w("StoryViewerViewModel", "Load stories failed: $msg")
            },
        )
    }

    fun resolveInitialPageIndex(pages: List<StoryViewerPage>): Int {
        if (pages.isEmpty()) return 0
        if (startAuthorId.isBlank()) return 0
        val index = pages.indexOfFirst { it.ring.authorId == startAuthorId }
        return if (index >= 0) index else 0
    }

    private fun applyRings(rings: List<StoryRingItem>) {
        _rings.value = rings
        val flat = flattenPages(rings)
        val startIndex = resolveInitialPageIndex(flat)
        _startPageIndex.value = startIndex
        _pages.value = flat
    }

    private fun flattenPages(rings: List<StoryRingItem>): List<StoryViewerPage> =
        rings.flatMap { ring ->
            ring.stories
                .sortedBy { it.createdAtMillis }
                .mapIndexed { index, story ->
                    StoryViewerPage(ring = ring, story = story, storyIndexInRing = index)
                }
        }

    private fun orderRings(rings: List<StoryRing>, authorOrder: List<String>): List<StoryRing> {
        val map = rings.associateBy { it.authorId }
        val ordered = authorOrder.mapNotNull { id ->
            map[id]?.takeIf { it.stories.isNotEmpty() }
        }.toMutableList()
        val myRing = rings.firstOrNull { it.isMe && it.stories.isNotEmpty() }
        if (myRing != null && ordered.none { it.authorId == myRing.authorId }) {
            ordered.add(0, myRing)
        }
        return ordered
    }

    private fun orderRingsFromCache(authorOrder: List<String>): List<StoryRingItem> {
        val cached = StoryViewerCache.rings.filter { it.stories.isNotEmpty() }
        if (cached.isEmpty()) return emptyList()
        val map = cached.associateBy { it.authorId }
        val ordered = authorOrder.mapNotNull { id -> map[id] }.toMutableList()
        val myRing = cached.firstOrNull { it.isMe }
        if (myRing != null && ordered.none { it.authorId == myRing.authorId }) {
            ordered.add(0, myRing)
        }
        return ordered
    }

    fun pageAt(index: Int): StoryViewerPage? = _pages.value.getOrNull(index)

    fun isFirstStoryInRing(page: StoryViewerPage): Boolean = page.storyIndexInRing == 0

    fun markViewed(story: StoryItem) {
        if (story.viewedByMe) return
        markStoryViewedUseCase(story.id)
    }

    fun updateStoryPrivacy(
        storyId: String,
        privacy: StoryPrivacy,
        visibleToUserIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        updateStoryPrivacyUseCase(
            storyId = storyId,
            privacy = privacy,
            visibleToUserIds = visibleToUserIds,
            onSuccess = {
                val updatedRings = _rings.value.map { ring ->
                    ring.copy(
                        stories = ring.stories.map { story ->
                            if (story.id == storyId) {
                                story.copy(privacy = privacy, visibleToUserIds = visibleToUserIds)
                            } else {
                                story
                            }
                        },
                    )
                }
                applyRings(updatedRings)
                onSuccess()
            },
            onFailure = onFailure,
        )
    }

    fun deleteStory(
        storyId: String,
        currentPageIndex: Int,
        onSuccess: (newPageIndex: Int?) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        deleteStoryUseCase(
            storyId = storyId,
            onSuccess = {
                val updatedRings = _rings.value.map { ring ->
                    ring.copy(stories = ring.stories.filter { it.id != storyId })
                }.filter { it.stories.isNotEmpty() }
                applyRings(updatedRings)
                if (_pages.value.isEmpty()) {
                    _finished.value = true
                    onSuccess(null)
                } else {
                    val newIndex = currentPageIndex.coerceAtMost(_pages.value.lastIndex)
                    onSuccess(newIndex)
                }
            },
            onFailure = onFailure,
        )
    }
}
