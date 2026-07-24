package com.example.messageapp.model

object StoryViewerCache {
    @Volatile
    var rings: List<StoryRingItem> = emptyList()

    fun update(rings: List<StoryRingItem>) {
        this.rings = rings
    }

    fun clear() {
        rings = emptyList()
    }
}
