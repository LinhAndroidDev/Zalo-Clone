package com.example.messageapp.utils

import com.example.messageapp.domain.repository.GroupChatRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupAvatarLoader @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
) {

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

        groupChatRepository.getGroupMemberAvatars(
            groupId = groupId,
            onSuccess = { totalCount, avatarUrls ->
                val data = GroupAvatarData(totalCount = totalCount, avatarUrls = avatarUrls)
                cache[groupId] = data
                dispatch(groupId, Result.success(data))
            },
            onFailure = {
                dispatch(groupId, Result.failure(IllegalStateException(it)))
            },
        )
    }

    fun cancel(groupId: String?) {
        // Recycled rows rely on avatarFriend.tag checks; in-flight work may still fill cache.
    }

    /** Call after group membership changes so composite avatar refetches current members. */
    fun invalidate(groupId: String) {
        if (groupId.isBlank()) return
        cache.remove(groupId)
    }

    private fun dispatch(groupId: String, result: Result<GroupAvatarData>) {
        val callbacks = synchronized(inFlight) {
            inFlight.remove(groupId).orEmpty()
        }
        callbacks.forEach { it(result) }
    }
}
