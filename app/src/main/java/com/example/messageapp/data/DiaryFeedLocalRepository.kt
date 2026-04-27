package com.example.messageapp.data

import com.example.messageapp.model.DiaryPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryFeedLocalRepository @Inject constructor() {

    private val _posts = MutableStateFlow<List<DiaryPost>>(emptyList())
    val posts = _posts.asStateFlow()

    fun addPost(post: DiaryPost) {
        _posts.update { current -> listOf(post) + current }
    }
}
