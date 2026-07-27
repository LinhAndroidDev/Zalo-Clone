package com.example.messageapp.model

data class StoryViewerPage(
    val ring: StoryRingItem,
    val story: StoryItem,
    val storyIndexInRing: Int,
)
