package com.example.messageapp.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.model.StoryRing
import com.example.messageapp.domain.usecase.story.GetStoriesForViewerUseCase
import com.example.messageapp.domain.usecase.story.MarkStoryViewedUseCase
import com.example.messageapp.mapper.StoryUiMapper
import com.example.messageapp.model.StoryItem
import com.example.messageapp.model.StoryRingItem
import com.example.messageapp.model.StoryViewerCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getStoriesForViewerUseCase: GetStoriesForViewerUseCase,
    private val markStoryViewedUseCase: MarkStoryViewedUseCase,
) : BaseViewModel() {

    private val startAuthorId: String = savedStateHandle.get<String>("startAuthorId").orEmpty()
    private val ringAuthorIds: Array<String> =
        savedStateHandle.get<Array<String>>("ringAuthorIds") ?: emptyArray()

    private val _rings = MutableStateFlow<List<StoryRingItem>>(emptyList())
    val rings = _rings.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished = _finished.asStateFlow()

    init {
        loadRings()
    }

    private fun loadRings() {
        val ids = if (ringAuthorIds.isNotEmpty()) ringAuthorIds.toList() else listOf(startAuthorId)
        getStoriesForViewerUseCase(
            authorIds = ids,
            onSuccess = { domainRings ->
                var ordered = orderRings(domainRings, ids).map { StoryUiMapper.toUi(it) }
                if (ordered.isEmpty() || ordered.all { it.stories.isEmpty() }) {
                    ordered = orderRingsFromCache(ids)
                }
                _rings.value = ordered
                _loading.value = false
                if (ordered.isEmpty() || ordered.all { it.stories.isEmpty() }) {
                    _finished.value = true
                }
            },
            onFailure = { msg ->
                _loading.value = false
                Log.w("StoryViewerViewModel", "Load stories failed: $msg")
            },
        )
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

    fun startRingIndex(): Int {
        val rings = _rings.value
        if (rings.isEmpty()) return 0
        val idx = rings.indexOfFirst { it.authorId == startAuthorId }
        return if (idx >= 0) idx else 0
    }

    fun markViewed(story: StoryItem) {
        if (story.viewedByMe) return
        markStoryViewedUseCase(story.id)
    }

}
