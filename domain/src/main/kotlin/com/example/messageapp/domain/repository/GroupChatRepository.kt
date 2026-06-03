package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.GroupChat
import com.example.messageapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface GroupChatRepository {
    fun createGroup(
        name: String,
        creatorId: String,
        creatorAvatar: String,
        welcomeMessage: String,
        welcomeInboxPerson: String,
        otherMemberIds: List<String>,
        onSuccess: (groupId: String, inboxConversation: Conversation) -> Unit,
        onFailure: (String) -> Unit,
    )
    fun getGroup(groupId: String, onSuccess: (GroupChat) -> Unit, onFailure: (String) -> Unit = {})
    fun getGroupMemberIds(groupId: String, onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit = {})
    fun getGroupMemberAvatars(
        groupId: String,
        limit: Int = 3,
        onSuccess: (memberCount: Int, avatarUrls: List<String>) -> Unit,
        onFailure: (String) -> Unit = {},
    )
    fun observeGroupMemberRead(groupId: String): Flow<Map<String, String>>
    fun observeGroupTyping(groupId: String, myUserId: String): Flow<Boolean>
    fun markGroupMessageRead(userId: String, groupId: String, lastReadTime: String)
    fun loadGroupMembers(groupId: String, onSuccess: (List<User>) -> Unit, onFailure: (String) -> Unit = {})
}
