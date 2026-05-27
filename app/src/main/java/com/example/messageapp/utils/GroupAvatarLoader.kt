package com.example.messageapp.utils

import java.util.concurrent.ConcurrentHashMap

object GroupAvatarLoader {

    data class GroupAvatarData(
        val totalCount: Int,
        val avatarUrls: List<String>,
    )

    private val cache = ConcurrentHashMap<String, GroupAvatarData>()
    private val inFlight = ConcurrentHashMap<String, MutableList<(Result<GroupAvatarData>) -> Unit>>()

    fun load(
        groupId: String,
        onReady: (GroupAvatarData) -> Unit,
        onError: () -> Unit = {},
    ) {
        if (groupId.isBlank()) {
            onError()
            return
        }

        cache[groupId]?.let {
            onReady(it)
            return
        }

        val callback: (Result<GroupAvatarData>) -> Unit = { result ->
            result.fold(onSuccess = onReady, onFailure = { onError() })
        }

        synchronized(inFlight) {
            val pending = inFlight[groupId]
            if (pending != null) {
                pending.add(callback)
                return
            }
            inFlight[groupId] = mutableListOf(callback)
        }

        FireBaseInstance.getGroupMemberAvatars(
            groupId = groupId,
            success = { totalCount, avatarUrls ->
                val list = avatarUrls - avatarUrls[0]
                val data = GroupAvatarData(totalCount = totalCount - 1, avatarUrls = list)
                cache[groupId] = data
                dispatch(groupId, Result.success(data))
            },
            failure = {
                dispatch(groupId, Result.failure(IllegalStateException(it)))
            },
        )
    }

    fun cancel(groupId: String?) {
        // Recycled rows rely on avatarFriend.tag checks; in-flight work may still fill cache.
    }

    private fun dispatch(groupId: String, result: Result<GroupAvatarData>) {
        val callbacks = synchronized(inFlight) {
            inFlight.remove(groupId).orEmpty()
        }
        callbacks.forEach { it(result) }
    }
}
