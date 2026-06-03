package com.example.messageapp.data.repository

import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.GroupChat
import com.example.messageapp.domain.model.User
import com.example.messageapp.domain.repository.GroupChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupChatRepositoryImpl @Inject constructor() : GroupChatRepository {

    override fun createGroup(
        name: String,
        creatorId: String,
        creatorAvatar: String,
        welcomeMessage: String,
        welcomeInboxPerson: String,
        otherMemberIds: List<String>,
        onSuccess: (groupId: String, inboxConversation: Conversation) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.createGroup(
            name = name,
            creatorId = creatorId,
            creatorAvatar = creatorAvatar,
            welcomeMessage = welcomeMessage,
            welcomeInboxPerson = welcomeInboxPerson,
            otherMemberIds = otherMemberIds,
            success = { groupId, inbox -> onSuccess(groupId, EntityMapper.toDomain(inbox)) },
            failure = onFailure,
        )
    }

    override fun getGroup(groupId: String, onSuccess: (GroupChat) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.getGroup(
            groupId = groupId,
            success = { onSuccess(EntityMapper.toDomain(it)) },
            failure = onFailure,
        )
    }

    override fun getGroupMemberIds(groupId: String, onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.getGroupMemberIds(groupId = groupId, success = onSuccess, failure = onFailure)
    }

    override fun getGroupMemberAvatars(
        groupId: String,
        limit: Int,
        onSuccess: (memberCount: Int, avatarUrls: List<String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.getGroupMemberAvatars(groupId = groupId, limit = limit, success = onSuccess, failure = onFailure)
    }

    override fun observeGroupMemberRead(groupId: String): Flow<Map<String, String>> = callbackFlow {
        val registration = FireBaseInstance.observeGroupMemberRead(groupId) { map ->
            trySend(map)
        }
        awaitClose { registration.remove() }
    }

    override fun observeGroupTyping(groupId: String, myUserId: String): Flow<Boolean> = callbackFlow {
        val registration = FireBaseInstance.observeGroupTyping(groupId, myUserId) { show ->
            trySend(show)
        }
        awaitClose { registration.remove() }
    }

    override fun markGroupMessageRead(userId: String, groupId: String, lastReadTime: String) {
        FireBaseInstance.markGroupMessageRead(userId, groupId, lastReadTime)
    }

    override fun loadGroupMembers(
        groupId: String,
        onSuccess: (List<User>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.getGroupMemberIds(
            groupId = groupId,
            success = { memberIds ->
                if (memberIds.isEmpty()) {
                    onSuccess(emptyList())
                    return@getGroupMemberIds
                }
                val users = ArrayList<User>(memberIds.size)
                var remaining = memberIds.size
                memberIds.forEach { memberId ->
                    FireBaseInstance.getInfoUser(memberId) { user ->
                        users.add(
                            EntityMapper.toDomain(
                                user.copy(keyAuth = user.keyAuth?.takeIf { it.isNotBlank() } ?: memberId),
                            ),
                        )
                        remaining -= 1
                        if (remaining == 0) onSuccess(users.toList())
                    }
                }
            },
            failure = onFailure,
        )
    }

    override fun addGroupMembers(
        groupId: String,
        newMemberIds: List<String>,
        inviterId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.addGroupMembers(
            groupId = groupId,
            inviterId = inviterId,
            newMemberIds = newMemberIds,
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun removeGroupMember(
        groupId: String,
        memberId: String,
        actorId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.removeGroupMember(
            groupId = groupId,
            memberId = memberId,
            actorId = actorId,
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun leaveGroup(
        groupId: String,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.leaveGroup(
            groupId = groupId,
            userId = userId,
            actorId = userId,
            success = onSuccess,
            failure = onFailure,
        )
    }
}
