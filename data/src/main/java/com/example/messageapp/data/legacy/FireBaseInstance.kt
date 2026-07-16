package com.example.messageapp.data.legacy

import android.content.Context
import android.net.Uri
import android.util.Log

import com.example.messageapp.data.firestore.Conversation
import com.example.messageapp.data.firestore.ChatThreadPin
import com.example.messageapp.data.firestore.Emotion
import com.example.messageapp.data.firestore.EmotionType
import com.example.messageapp.data.firestore.Friend
import com.example.messageapp.data.firestore.FriendRequest
import com.example.messageapp.data.firestore.Message
import com.example.messageapp.data.firestore.Sticker
import com.example.messageapp.data.firestore.TypeMessage
import com.example.messageapp.data.firestore.DiaryLinkPreview
import com.example.messageapp.data.firestore.DiaryPost
import com.example.messageapp.data.firestore.DiaryPostComment
import com.example.messageapp.data.firestore.DiaryNotificationFirestore
import com.example.messageapp.data.firestore.DiaryPostFirestore
import com.example.messageapp.domain.model.DiaryNotification
import com.example.messageapp.domain.model.DiaryNotificationType
import com.example.messageapp.domain.model.EmotionType as DomainEmotionType
import com.example.messageapp.data.firestore.GroupChat
import com.example.messageapp.data.firestore.User
import com.example.messageapp.data.remote.ApiClient
import com.example.messageapp.data.remote.Token
import com.example.messageapp.data.remote.request.Data
import com.example.messageapp.data.remote.request.MessageRequest
import com.example.messageapp.data.remote.request.NotificationData
import com.example.messageapp.data.legacy.MediaFileUtils.compressImage
import com.example.messageapp.data.legacy.MediaFileUtils.isVideoUri
import com.example.messageapp.data.legacy.MediaFileUtils.readUriBytes
import com.example.messageapp.domain.chat.MentionParser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.HashMap
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FireBaseInstance {
    private val db by lazy { Firebase.firestore }

    private const val PATH_USER = "users"
    private const val PATH_EMAIL = "email"
    private const val PATH_PASSWORD = "password"
    private const val PATH_AVATAR = "avatar"
    private const val PATH_MESSAGE = "messages"
    private const val PATH_CHAT = "chats"
    private const val PATH_TOKEN = "Tokens"
    private const val PATH_IMAGE_COVER = "imageCover"
    private const val PATH_IMAGE = "images"
    private const val PATH_PHOTO = "photo"
    private const val PATH_EMOTION = "emotion"
    private const val PATH_AUDIO = "audios"
    private const val PATH_TYPING = "typing"
    private const val PATH_STICKER = "sticker"
    private const val PATH_FRIEND_REQUESTS = "friendRequests"
    private const val PATH_FRIENDS = "friends"
    private const val PATH_SEARCH_HISTORY = "searchHistory"
    private const val PATH_ITEMS = "items"
    private const val PATH_GROUPS = "groups"
    private const val PATH_MEMBER_READ = "memberRead"
    private const val PATH_PINNED_MESSAGES = "pinnedMessages"
    private const val PATH_PINNED_MESSAGE_TIME = "pinnedMessageTime"
    private const val PATH_PINNED_BY = "pinnedBy"
    private const val PATH_PINNED_BY_NAME = "pinnedByName"
    private const val PATH_PINNED_PREVIEW_TEXT = "pinnedPreviewText"
    private const val PATH_PINNED_MESSAGE_TYPE = "pinnedMessageType"
    private const val PATH_PINNED_PHOTO_URL = "pinnedPhotoUrl"
    private const val MAX_PINNED_MESSAGES = 10

    /**
     * This function is used to check the login of the user
     * + By query whereEqualTo @param email and @param password
     * @param email email of user
     * @param password password of user
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun checkLogin(
        email: String,
        password: String,
        success: (QuerySnapshot) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER)
            .whereEqualTo(PATH_EMAIL, email)
            .whereEqualTo(PATH_PASSWORD, password)
            .get()
            .addOnSuccessListener { querySnapshot ->
                success.invoke(querySnapshot)
            }
            .addOnFailureListener { exception ->
                failure.invoke(exception.message.toString())
            }
    }

    /**
     * This function is used to get all users from the FireStore database
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun getUsers(success: (QuerySnapshot) -> Unit, failure: (String) -> Unit) {
        db.collection(PATH_USER).get().addOnSuccessListener { result ->
            success.invoke(result)
        }.addOnFailureListener { e ->
            failure.invoke(e.message.toString())
        }
    }

    /**
     * This function is used to add a new user to the FiresStore database
     * @param user data user
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun addUser(
        user: HashMap<String, String>,
        success: (String) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER).add(user)
            .addOnSuccessListener {
                success.invoke("Create Successful")
            }.addOnFailureListener { e ->
                failure.invoke(e.message.toString())
            }
    }

    /**
     * This function is used to get all messages from the FireStore database
     * @param idRoom document id under [PATH_MESSAGE] (see [messageThreadDocumentId])
     * @param success callback when query is successful
     */
    fun getMessage(
        idRoom: String,
        success: (QuerySnapshot?) -> Unit,
        failure: (String) -> Unit,
    ): ListenerRegistration {
        return db.collection(PATH_MESSAGE)
            .document(idRoom)
            .collection(PATH_CHAT)
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                success.invoke(value)
            }
    }

    /**
     * Document id under [PATH_MESSAGE] for this chat thread.
     * Group: [Conversation.friendId] is the group id (or UUID-shaped room id when [Conversation.isGroupThread]).
     */
    fun messageThreadDocumentId(conversation: Conversation, userId: String): String =
        if (conversation.isGroupThread()) conversation.friendId
        else listOf(conversation.friendId, userId).sorted().toString()

    /**
     * Creates a group chat document and an inbox [Conversation] row for each member.
     */
    fun createGroup(
        name: String,
        creatorId: String,
        creatorAvatar: String,
        welcomeMessage: String,
        welcomeInboxPerson: String,
        otherMemberIds: List<String>,
        success: (groupId: String, inboxConversation: Conversation) -> Unit,
        failure: (String) -> Unit,
    ) {
        val others = otherMemberIds.filter { it.isNotBlank() && it != creatorId }.distinct()
        if (others.isEmpty()) {
            failure.invoke("Chọn ít nhất một thành viên")
            return
        }
        val memberIds = (listOf(creatorId) + others).distinct()
        if (memberIds.size < 2) {
            failure.invoke("Nhóm cần ít nhất 2 thành viên")
            return
        }
        val groupId = UUID.randomUUID().toString()
        val displayName = name.ifBlank { "Nhóm mới" }
        val photoUrl = creatorAvatar.trim()
        val group = GroupChat(
            name = displayName,
            photoUrl = photoUrl,
            memberIds = memberIds,
            createdBy = creatorId,
            createdAt = System.currentTimeMillis(),
        )
        val time = DateUtils.getTimeCurrent()
        val inboxConversationCreator = Conversation(
            friendId = groupId,
            friendImage = photoUrl,
            message = welcomeMessage,
            name = displayName,
            person = welcomeInboxPerson,
            sender = creatorId,
            time = time,
            seen = "1",
            numberUnSeen = 0,
            typing = false,
            isGroup = true,
        )
        val inboxConversationMember = inboxConversationCreator.copy(
            seen = "0",
            numberUnSeen = 1,
        )
        val welcomeChatMessage = Message(
            message = welcomeMessage,
            receiver = groupId,
            sender = creatorId,
            time = time,
            type = TypeMessage.MESSAGE.rawValue,
        )
        val batch = db.batch()
        batch.set(db.collection(PATH_GROUPS).document(groupId), group)
        for (m in memberIds) {
            val row = if (m == creatorId) inboxConversationCreator else inboxConversationMember
            batch.set(db.collection("Conversation$m").document(groupId), row)
        }
        batch.set(
            db.collection(PATH_MESSAGE).document(groupId).collection(PATH_CHAT).document(time),
            welcomeChatMessage,
        )
        batch.commit()
            .addOnSuccessListener { success.invoke(groupId, inboxConversationCreator) }
            .addOnFailureListener { e ->
                failure.invoke(e.message ?: "Lỗi tạo nhóm")
            }
    }

    /**
     * One-time read of [GroupChat] for [groupId].
     */
    fun getGroup(
        groupId: String,
        success: (GroupChat) -> Unit,
        failure: (String) -> Unit,
    ) {
        if (groupId.isBlank()) {
            failure.invoke("ID nhóm không hợp lệ")
            return
        }
        db.collection(PATH_GROUPS).document(groupId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    failure.invoke("Không tìm thấy nhóm")
                    return@addOnSuccessListener
                }
                val g = doc.toObject(GroupChat::class.java)
                if (g == null || g.memberIds.isEmpty()) {
                    failure.invoke("Dữ liệu nhóm không hợp lệ")
                    return@addOnSuccessListener
                }
                success.invoke(g)
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    fun addGroupMembers(
        groupId: String,
        inviterId: String,
        newMemberIds: List<String>,
        success: () -> Unit,
        failure: (String) -> Unit,
    ) {
        val toAdd = newMemberIds.filter { it.isNotBlank() }.distinct()
        if (toAdd.isEmpty()) {
            failure.invoke("Chọn ít nhất một thành viên")
            return
        }
        getGroup(
            groupId = groupId,
            success = { group ->
                val existing = group.memberIds.distinct().filter { it.isNotBlank() }
                val newcomers = toAdd.filter { it !in existing }
                if (newcomers.isEmpty()) {
                    failure.invoke("Các thành viên đã có trong nhóm")
                    return@getGroup
                }
                val merged = (existing + newcomers).distinct()
                val inboxRow = Conversation(
                    friendId = groupId,
                    friendImage = "",
                    message = "",
                    name = group.name,
                    person = "",
                    sender = inviterId,
                    time = "",
                    seen = "0",
                    numberUnSeen = 0,
                    typing = false,
                    isGroup = true,
                )
                val batch = db.batch()
                batch.update(
                    db.collection(PATH_GROUPS).document(groupId),
                    mapOf("memberIds" to merged, "photoUrl" to ""),
                )
                newcomers.forEach { memberId ->
                    batch.set(db.collection("Conversation$memberId").document(groupId), inboxRow)
                }
                batch.commit()
                    .addOnSuccessListener {
                        resetGroupInboxAvatars(groupId, merged)
                        fetchDisplayNames(listOf(inviterId) + newcomers) { names ->
                            val actorName = displayNameFor(inviterId, names)
                            val targetNames = newcomers.map { id ->
                                displayNameFor(id, names).trim().ifBlank { id }
                            }
                            val text = buildGroupEventAddedText(actorName, targetNames)
                            postGroupSystemMessage(
                                groupId = groupId,
                                text = text,
                                memberIdsForInbox = merged,
                                actorId = inviterId,
                                actorName = actorName,
                                groupName = group.name,
                                systemEvent = SYSTEM_EVENT_ADD,
                                systemTargetIds = newcomers,
                                systemTargetNames = targetNames,
                            )
                            success.invoke()
                        }
                    }
                    .addOnFailureListener { e -> failure.invoke(e.message ?: "Lỗi thêm thành viên") }
            },
            failure = failure,
        )
    }

    fun removeGroupMember(
        groupId: String,
        memberId: String,
        actorId: String,
        success: () -> Unit,
        failure: (String) -> Unit,
    ) {
        if (groupId.isBlank() || memberId.isBlank()) {
            failure.invoke("Dữ liệu không hợp lệ")
            return
        }
        getGroup(
            groupId = groupId,
            success = { group ->
                val current = group.memberIds.distinct().filter { it.isNotBlank() }
                if (memberId !in current) {
                    failure.invoke("Thành viên không có trong nhóm")
                    return@getGroup
                }
                val updated = current.filter { it != memberId }
                if (updated.size < 2) {
                    failure.invoke("Nhóm cần ít nhất 2 thành viên")
                    return@getGroup
                }
                val batch = db.batch()
                batch.update(
                    db.collection(PATH_GROUPS).document(groupId),
                    mapOf("memberIds" to updated, "photoUrl" to ""),
                )
                batch.delete(db.collection("Conversation$memberId").document(groupId))
                batch.commit()
                    .addOnSuccessListener {
                        resetGroupInboxAvatars(groupId, updated)
                        val idsToResolve = if (actorId == memberId) {
                            listOf(memberId)
                        } else {
                            listOf(actorId, memberId)
                        }
                        fetchDisplayNames(idsToResolve) { names ->
                            val text = if (actorId == memberId) {
                                buildGroupEventLeftText(displayNameFor(memberId, names))
                            } else {
                                buildGroupEventRemovedText(
                                    displayNameFor(actorId, names),
                                    displayNameFor(memberId, names),
                                )
                            }
                            val targetName = displayNameFor(memberId, names)
                            val actorDisplayName = if (actorId == memberId) {
                                targetName
                            } else {
                                displayNameFor(actorId, names)
                            }
                            postGroupSystemMessage(
                                groupId = groupId,
                                text = text,
                                memberIdsForInbox = updated,
                                actorId = actorId,
                                actorName = actorDisplayName,
                                groupName = group.name,
                                systemEvent = if (actorId == memberId) SYSTEM_EVENT_LEAVE else SYSTEM_EVENT_REMOVE,
                                systemTargetIds = listOf(memberId),
                                systemTargetNames = listOf(targetName),
                            )
                            success.invoke()
                        }
                    }
                    .addOnFailureListener { e -> failure.invoke(e.message ?: "Lỗi xóa thành viên") }
            },
            failure = failure,
        )
    }

    fun leaveGroup(
        groupId: String,
        userId: String,
        actorId: String,
        success: () -> Unit,
        failure: (String) -> Unit,
    ) {
        removeGroupMember(
            groupId = groupId,
            memberId = userId,
            actorId = actorId,
            success = success,
            failure = failure,
        )
    }

    private const val GROUP_INBOX_PERSON_SYSTEM = "Thông báo"
    private const val SYSTEM_EVENT_ADD = "add"
    private const val SYSTEM_EVENT_REMOVE = "remove"
    private const val SYSTEM_EVENT_LEAVE = "leave"

    private fun formatDisplayNameList(names: List<String>): String {
        val cleaned = names.map { it.trim() }.filter { it.isNotBlank() }
        return when (cleaned.size) {
            0 -> ""
            1 -> cleaned[0]
            2 -> "${cleaned[0]} và ${cleaned[1]}"
            else -> cleaned.dropLast(1).joinToString(", ") + " và ${cleaned.last()}"
        }
    }

    private fun displayNameFor(userId: String, names: Map<String, String>): String =
        names[userId]?.trim()?.takeIf { it.isNotBlank() } ?: userId

    private fun fetchDisplayNames(
        userIds: List<String>,
        onComplete: (Map<String, String>) -> Unit,
    ) {
        val ids = userIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) {
            onComplete(emptyMap())
            return
        }
        val result = hashMapOf<String, String>()
        var remaining = ids.size
        ids.forEach { id ->
            getUserById(
                userId = id,
                success = { user ->
                    result[id] = user.name.orEmpty().ifBlank { id }
                    remaining -= 1
                    if (remaining == 0) onComplete(result)
                },
                failure = {
                    result[id] = id
                    remaining -= 1
                    if (remaining == 0) onComplete(result)
                },
            )
        }
    }

    private fun buildGroupEventAddedText(actorName: String, targetNames: List<String>): String =
        "$actorName đã thêm ${formatDisplayNameList(targetNames)} vào nhóm"

    private fun buildGroupEventRemovedText(actorName: String, targetName: String): String =
        "$actorName đã xóa $targetName khỏi nhóm"

    private fun buildGroupEventLeftText(memberName: String): String =
        "$memberName đã rời nhóm"

    /**
     * Clears static group photo on inbox rows so the client reloads composite member avatars.
     */
    private fun resetGroupInboxAvatars(groupId: String, memberIds: List<String>) {
        val ids = memberIds.distinct().filter { it.isNotBlank() }
        if (groupId.isBlank() || ids.isEmpty()) return
        val batch = db.batch()
        ids.forEach { mid ->
            batch.update(
                db.collection("Conversation$mid").document(groupId),
                mapOf("friendImage" to ""),
            )
        }
        batch.commit().addOnFailureListener {
            Log.e("resetGroupInboxAvatars", it.message.orEmpty())
        }
    }

    private fun postGroupSystemMessage(
        groupId: String,
        text: String,
        memberIdsForInbox: List<String>,
        actorId: String,
        actorName: String,
        groupName: String,
        systemEvent: String = "",
        systemTargetIds: List<String> = emptyList(),
        systemTargetNames: List<String> = emptyList(),
    ) {
        if (groupId.isBlank() || text.isBlank()) return
        val time = DateUtils.getTimeCurrent()
        val chatMessage = Message(
            message = text,
            receiver = groupId,
            sender = actorId,
            time = time,
            type = TypeMessage.SYSTEM.rawValue,
            systemEvent = systemEvent,
            systemActorId = actorId,
            systemActorName = actorName,
            systemTargetIds = ArrayList(systemTargetIds),
            systemTargetNames = ArrayList(systemTargetNames),
        )
        val batch = db.batch()
        batch.set(
            db.collection(PATH_MESSAGE).document(groupId).collection(PATH_CHAT).document(time),
            chatMessage,
        )
        memberIdsForInbox.distinct().filter { it.isNotBlank() }.forEach { mid ->
            val isActor = mid == actorId
            val conv = Conversation(
                friendId = groupId,
                friendImage = "",
                message = text,
                name = groupName,
                person = GROUP_INBOX_PERSON_SYSTEM,
                sender = "",
                time = time,
                seen = if (isActor) "1" else "0",
                numberUnSeen = if (isActor) 0 else 1,
                typing = false,
                isGroup = true,
            )
            batch.set(db.collection("Conversation$mid").document(groupId), conv, SetOptions.merge())
        }
        batch.commit().addOnFailureListener {
            Log.e("postGroupSystemMessage", it.message.orEmpty())
        }
    }

    /**
     * Marks the current user's group inbox row as read.
     */
    fun markGroupConversationSeen(userId: String, groupId: String) {
        markGroupMessageRead(userId, groupId, lastReadTime = "")
    }

    /**
     * Marks group inbox read and updates this member's read cursor for read receipts.
     */
    fun markGroupMessageRead(userId: String, groupId: String, lastReadTime: String) {
        if (userId.isBlank() || groupId.isBlank()) return
        db.collection("Conversation$userId").document(groupId)
            .update(
                mapOf(
                    "seen" to "1",
                    "numberUnSeen" to 0,
                ),
            )
        if (lastReadTime.isNotBlank()) {
            db.collection(PATH_GROUPS).document(groupId)
                .collection(PATH_MEMBER_READ).document(userId)
                .set(mapOf("lastReadTime" to lastReadTime))
        }
    }

    fun observeGroupMemberRead(
        groupId: String,
        onChange: (Map<String, String>) -> Unit,
    ): ListenerRegistration {
        if (groupId.isBlank()) {
            onChange(emptyMap())
            return object : ListenerRegistration {
                override fun remove() {}
            }
        }
        return db.collection(PATH_GROUPS).document(groupId)
            .collection(PATH_MEMBER_READ)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onChange(emptyMap())
                    return@addSnapshotListener
                }
                val map = hashMapOf<String, String>()
                snapshot.documents.forEach { doc ->
                    val time = doc.getString("lastReadTime").orEmpty()
                    if (time.isNotBlank()) {
                        map[doc.id] = time
                    }
                }
                onChange(map)
            }
    }

    fun getGroupMemberIds(
        groupId: String,
        success: (List<String>) -> Unit,
        failure: (String) -> Unit = {},
    ) {
        getGroup(
            groupId = groupId,
            success = { group -> success.invoke(group.memberIds.distinct().filter { it.isNotBlank() }) },
            failure = failure,
        )
    }

    fun getGroupMemberAvatars(
        groupId: String,
        limit: Int = 3,
        success: (memberCount: Int, avatarUrls: List<String>) -> Unit,
        failure: (String) -> Unit = {},
    ) {
        getGroupMemberIds(
            groupId = groupId,
            success = { memberIds ->
                val totalCount = memberIds.size
                val idsToFetch = memberIds.take(limit)
                if (idsToFetch.isEmpty()) {
                    success.invoke(0, emptyList())
                    return@getGroupMemberIds
                }

                val avatarUrls = Array(idsToFetch.size) { "" }
                var completed = 0

                fun finishIfDone() {
                    if (completed == idsToFetch.size) {
                        success.invoke(totalCount, avatarUrls.toList())
                    }
                }

                idsToFetch.forEachIndexed { index, userId ->
                    getUserById(
                        userId = userId,
                        success = { user ->
                            avatarUrls[index] = user.avatar.orEmpty()
                            completed++
                            finishIfDone()
                        },
                        failure = {
                            completed++
                            finishIfDone()
                        },
                    )
                }
            },
            failure = failure,
        )
    }

    /**
     * This function is used to send a message to the FireStore database
     * + Here we send a message and create 2 conversations between the sender and the receiver
     * @param message data message
     * @param userId key auth of user
     * @param time time message sent
     * @param conversation data conversation
     * @param nameSender name of sender
     * @param type type of message
     * @param success callback when query is successful
     */
    fun sendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        type: TypeMessage = TypeMessage.MESSAGE,
        sendFirst: Boolean,
        success: () -> Unit,
    ) {
        val idRoom = messageThreadDocumentId(conversation, userId)

        db.collection(PATH_MESSAGE)
            .document(idRoom)
            .collection(PATH_CHAT)
            .document(time)
            .set(message)

        if (conversation.isGroupThread()) {
            if (sendFirst) {
                handleSendMessageGroup(
                    message, userId, time, conversation, nameSender, type, sendFirst = true
                )
            } else {
                handleSendMessageGroup(
                    message, userId, time, conversation, nameSender, type, sendFirst = false
                )
            }
            success.invoke()
            return
        }

        if (sendFirst) {
            Log.e("sendMessage", "sendFirst")
            handleSendMessage(message, userId, time, conversation, nameSender, type, 1)
        } else {
            Log.e("sendMessage", "not sendFirst")
            getConversation(conversation.friendId, userId) { cvt ->
                val num = cvt.numberUnSeen + 1
                handleSendMessage(message, userId, time, conversation, nameSender, type, num)
            }
        }
        success.invoke()
    }

    /**
     * This function is used to handle send a message to the FireStore database
     * + Send a notification to the receiver via the receiver's token.
     * + Update data conversation between sender and receiver
     * @param message data message
     * @param userId key auth of user
     * @param time time message sent
     * @param conversation data conversation
     * @param nameSender name of sender
     * @param type type of message
     * @param numberUnSeen quantity of message unseen
     */
    private fun handleSendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        type: TypeMessage = TypeMessage.MESSAGE,
        numberUnSeen: Int
    ) {

        //Get token of receiver to send notification message to receiver
        val fcmReply = ReplyNotificationHelper.buildFcmReplyFields(message, time, nameSender)
        getTokenMessage(
            conversation.friendId,
            success = { token ->
                val baseBody = notificationBodyForType(type, message, nameSender)
                val notificationNotification = NotificationData(
                    token = token,
                    data = Data(
                        title = nameSender,
                        body = ReplyNotificationHelper.formatNotificationBody(message, baseBody),
                        senderId = userId,
                        recipientUserId = conversation.friendId,
                        groupId = null,
                        messageTime = fcmReply.messageTime,
                        replyPreviewText = fcmReply.replyPreviewText,
                        replySenderName = fcmReply.replySenderName,
                        replyType = fcmReply.replyType,
                        replyPhotoUrl = fcmReply.replyPhotoUrl,
                    )
                )

                enqueueSendMessageApi(notificationNotification)
            },
            failure = {
                Log.e("Send Message", "Token retrieval failed")
            }
        )

        //Create Data Conversation For Sender
        val conversationData = Conversation(
            friendId = conversation.friendId,
            friendImage = conversation.friendImage,
            message = ReplyNotificationHelper.formatInboxPreview(
                message,
                inboxMessagePreviewSender(type, message, conversation.name),
            ),
            name = conversation.name,
            person = "Bạn",
            sender = userId,
            time = time,
        )

        //Create Conversation For Sender
        db.collection("Conversation${userId}")
            .document(conversation.friendId)
            .set(conversationData)

        //Create Data Conversation For Receiver
        val conversationFriend = Conversation(
            friendId = userId,
            message = ReplyNotificationHelper.formatInboxPreview(
                message,
                inboxMessagePreviewOthers(type, message, nameSender),
            ),
            name = nameSender,
            person = nameSender,
            sender = userId,
            time = time,
            numberUnSeen = numberUnSeen
        )

        //Create Conversation For Receiver
        db.collection("Conversation${conversation.friendId}")
            .document(userId)
            .set(conversationFriend)
    }

    private fun enqueueSendMessageApi(notification: NotificationData) {
        ApiClient.api?.sendMessage(MessageRequest(message = notification))
            ?.enqueue(object : Callback<MessageRequest> {
                override fun onFailure(call: Call<MessageRequest>, t: Throwable) {
                    Log.e("Send Message", "Send Fail")
                }

                override fun onResponse(
                    call: Call<MessageRequest>,
                    response: Response<MessageRequest>,
                ) {
                    Log.e("Send Message", "Send Successful")
                }
            })
    }

    private fun notificationBodyForType(
        type: TypeMessage,
        message: Message,
        nameSender: String,
    ): String = when (type) {
        TypeMessage.MESSAGE, TypeMessage.SYSTEM -> message.message
        TypeMessage.PHOTOS -> "$nameSender đã gửi ảnh cho bạn"
        TypeMessage.SINGLE_PHOTO -> "$nameSender đã gửi 1 ảnh cho bạn"
        TypeMessage.AUDIO -> "$nameSender đã gửi 1 file ghi âm cho bạn"
    }

    private fun inboxMessagePreviewSender(
        type: TypeMessage,
        message: Message,
        conversationName: String,
    ): String = when (type) {
        TypeMessage.MESSAGE, TypeMessage.SYSTEM -> message.message
        TypeMessage.PHOTOS -> "Bạn đã gửi ảnh cho $conversationName"
        TypeMessage.SINGLE_PHOTO -> "Bạn đã gửi 1 ảnh cho $conversationName"
        TypeMessage.AUDIO -> "Bạn đã gửi 1 file ghi âm cho $conversationName"
    }

    private fun inboxMessagePreviewOthers(
        type: TypeMessage,
        message: Message,
        nameSender: String,
    ): String = when (type) {
        TypeMessage.MESSAGE, TypeMessage.SYSTEM -> message.message
        TypeMessage.PHOTOS -> "$nameSender đã gửi ảnh cho bạn"
        TypeMessage.SINGLE_PHOTO -> "$nameSender đã gửi 1 ảnh cho bạn"
        TypeMessage.AUDIO -> "$nameSender đã gửi 1 file ghi âm cho bạn"
    }

    private fun handleSendMessageGroup(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        type: TypeMessage,
        sendFirst: Boolean,
    ) {
        val groupId = conversation.friendId
        getGroup(
            groupId,
            success = { group ->
                val memberIds = group.memberIds.distinct().filter { it.isNotBlank() }
                val notifBody = ReplyNotificationHelper.formatNotificationBody(
                    message,
                    notificationBodyForType(type, message, nameSender),
                )
                val fcmReply = ReplyNotificationHelper.buildFcmReplyFields(message, time, nameSender)
                val othersInboxPreview = ReplyNotificationHelper.formatInboxPreview(
                    message,
                    inboxMessagePreviewOthers(type, message, nameSender),
                )
                val mentionedTargets = MentionParser.resolveMentionTargetUserIds(
                    mentions = message.mentions.map {
                        com.example.messageapp.domain.model.MessageMention(it.userId, it.token, it.displayName)
                    },
                    memberIds = memberIds,
                    senderId = userId,
                )
                val isAllMention = MentionParser.hasAllMention(
                    message.mentions.map {
                        com.example.messageapp.domain.model.MessageMention(it.userId, it.token, it.displayName)
                    },
                )
                for (mid in memberIds) {
                    if (mid == userId) continue
                    getTokenMessage(
                        mid,
                        success = { token ->
                            val isMentioned = mid in mentionedTargets
                            val body = if (isMentioned) {
                                MentionParser.mentionNotificationBody(
                                    senderName = nameSender,
                                    groupName = conversation.name,
                                    messageText = message.message,
                                    isAllMention = isAllMention,
                                )
                            } else {
                                notifBody
                            }
                            enqueueSendMessageApi(
                                NotificationData(
                                    token = token,
                                    data = Data(
                                        title = nameSender,
                                        body = body,
                                        senderId = userId,
                                        recipientUserId = mid,
                                        groupId = groupId,
                                        isMention = if (isMentioned) "1" else "0",
                                        mentionType = when {
                                            !isMentioned -> null
                                            isAllMention -> "all"
                                            else -> "user"
                                        },
                                        messageTime = fcmReply.messageTime,
                                        replyPreviewText = fcmReply.replyPreviewText,
                                        replySenderName = fcmReply.replySenderName,
                                        replyType = fcmReply.replyType,
                                        replyPhotoUrl = fcmReply.replyPhotoUrl,
                                    ),
                                )
                            )
                        },
                        failure = { Log.e("Send Message", "Token retrieval failed for $mid") },
                    )
                }

                val batch = db.batch()
                for (m in memberIds) {
                    val ref = db.collection("Conversation$m").document(groupId)
                    if (m == userId) {
                        val conv = Conversation(
                            friendId = groupId,
                            friendImage = conversation.friendImage,
                            message = ReplyNotificationHelper.formatInboxPreview(
                                message,
                                inboxMessagePreviewSender(type, message, conversation.name),
                            ),
                            name = conversation.name,
                            person = "Bạn",
                            sender = userId,
                            time = time,
                            seen = "1",
                            numberUnSeen = 0,
                            typing = false,
                            isGroup = true,
                        )
                        batch.set(ref, conv, SetOptions.merge())
                    } else {
                        if (sendFirst) {
                            val conv = Conversation(
                                friendId = groupId,
                                friendImage = conversation.friendImage,
                                message = othersInboxPreview,
                                name = conversation.name,
                                person = nameSender,
                                sender = userId,
                                time = time,
                                seen = "0",
                                numberUnSeen = 1,
                                typing = false,
                                isGroup = true,
                            )
                            batch.set(ref, conv, SetOptions.merge())
                        } else {
                            batch.update(
                                ref,
                                mapOf(
                                    "message" to othersInboxPreview,
                                    "person" to nameSender,
                                    "sender" to userId,
                                    "time" to time,
                                    "typing" to false,
                                    "numberUnSeen" to FieldValue.increment(1),
                                    "isGroup" to true,
                                ),
                            )
                        }
                    }
                }
                val groupDocRef = db.collection(PATH_GROUPS).document(groupId)
                batch.update(
                    groupDocRef,
                    mapOf("typing" to false, "typingUserId" to ""),
                )
                batch.commit().addOnFailureListener {
                    Log.e("Send Message", "batch group inbox: ${it.message}")
                }
            },
            failure = { err ->
                Log.e("Send Message", "getGroup failed: $err")
            },
        )
    }

    /**
     * This function is used to get all conversations from the FireStore database
     * @param userId key auth of user
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun getListConversation(
        userId: String,
        success: (QuerySnapshot?) -> Unit,
        failure: (String) -> Unit,
    ): ListenerRegistration {
        return db.collection("Conversation${userId}")
            .orderBy("time", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                success.invoke(value)
            }
    }

    /**
     * This function is used to save token of user to the FireStore database
     * @param userId key auth of user
     * @param data data token
     */
    fun saveTokenMessage(userId: String, data: HashMap<String, String>) {
        db.collection(PATH_TOKEN).document(userId).set(data).addOnSuccessListener {
        }
    }

    /**
     * One-time read of the receiver's FCM token from Firestore.
     * Uses [com.google.firebase.firestore.DocumentReference.get] instead of a snapshot listener so
     * each send triggers at most one notification attempt (listeners would fire on every token
     * change and could duplicate API calls). Empty or blank tokens are treated as failure.
     *
     * @param friendId key auth of friend
     * @param success callback with non-blank token
     * @param failure callback when query fails, doc missing, or token empty
     */
    private fun getTokenMessage(
        friendId: String,
        success: (String) -> Unit,
        failure: (String) -> Unit
    ) {
        if (friendId.isBlank()) {
            failure.invoke("ID người nhận không hợp lệ")
            return
        }
        db.collection(PATH_TOKEN).document(friendId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    failure.invoke("Token not found")
                    return@addOnSuccessListener
                }
                val token = doc.toObject(Token::class.java)?.token?.trim().orEmpty()
                if (token.isEmpty()) {
                    failure.invoke("Token rỗng")
                    return@addOnSuccessListener
                }
                success.invoke(token)
            }
            .addOnFailureListener { e ->
                failure.invoke(e.message.toString())
            }
    }

    /**
     * This function is used to get information user from the FireStore database
     * @param userId key auth of user
     * @param success callback when query is successful
     */
    fun getInfoUser(userId: String, success: (User) -> Unit) {
        db.collection(PATH_USER)
            .document(userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("getInfoUser", error.message.toString())
                }
                if (value != null && value.exists()) {
                    success.invoke(value.toObject(User::class.java)!!)
                } else {
                    Log.e("getInfoUser", "User not found")
                }
            }
    }

    /**
     * One-time fetch to verify if a user with [userId] exists.
     * Calls [success] with the User if found, [failure] if not found or on error.
     */
    fun getUserById(
        userId: String,
        success: (User) -> Unit,
        failure: (String) -> Unit
    ) {
        if (userId.isBlank()) {
            failure.invoke("ID không hợp lệ")
            return
        }
        db.collection(PATH_USER)
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        success.invoke(user.copy(keyAuth = doc.id))
                    } else {
                        failure.invoke("Không tìm thấy người dùng")
                    }
                } else {
                    failure.invoke("Không tìm thấy người dùng")
                }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * This function is used to upload image to the Storage Firebase
     * @param context context of activity
     * @param uriPhoto uri of photo
     * @param success callback when upload is successful
     */
    fun uploadImage(
        context: Context,
        uriPhoto: Uri,
        success: (String) -> Unit,
        failure: ((String) -> Unit)? = null
    ) {
        val bytes = context.compressImage(uriPhoto)
        val fileName = "${UUID.randomUUID()}.jpg"
        val folder = PATH_IMAGE // Firebase path: images/*

        CloudinaryManager.uploadBytes(
            fileBytes = bytes,
            fileName = fileName,
            mimeType = "image/jpeg",
            folder = folder,
            onSuccess = success,
            onFailure = { e ->
                Log.e("Check fail uploadImage Cloudinary", "uploadImage failed: ${e.message}", e)
                failure?.invoke(
                    e.message ?: "Error"
                )
            }
        )
    }

    /**
     * This function is used to update avatar of user to the FireStore database
     * @param avatar data avatar
     * @param userId key auth of user
     */
    fun updateAvatarUser(avatar: String, userId: String) {
        db.collection(PATH_USER)
            .document(userId)
            .update(PATH_AVATAR, avatar)
    }

    /**
     * This function is used to get conversation from the FireStore database
     * @param friendId key auth of friend
     * @param userId key auth of user
     * @param success callback when query is successful
     */
    fun getConversation(friendId: String, userId: String, success: (Conversation) -> Unit) {
        db.collection("Conversation${friendId}")
            .document(userId)
            .get()
            .addOnSuccessListener { result ->
                val conversation = result.toObject(Conversation::class.java)
                conversation?.let { success.invoke(it) }
            }
    }

    /**
     * This function is used to get conversation from the FireStore database with Realtime
     * @param friendId key auth of friend
     * @param userId key auth of user
     * @param success callback when query is successful
     */
    fun getConversationRlt(friendId: String, userId: String, success: (Conversation) -> Unit): ListenerRegistration {
        return db.collection("Conversation${friendId}")
            .document(userId)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    val conversation = value.toObject(Conversation::class.java)
                    conversation?.let { success.invoke(it) }
                }
            }
    }

    /**
     * This function is used to update seen message for conversation user and friend
     * @param userId key auth of user
     * @param friendId key auth of friend
     */
    fun seenMessage(userId: String, friendId: String) {
        db.collection("Conversation${friendId}")
            .document(userId)
            .update(
                "seen", "1",
                "numberUnSeen", 0
            )
        db.collection("Conversation${userId}")
            .document(friendId)
            .update(
                "seen", "1",
                "numberUnSeen", 0
            )
    }

    /**
     * This function is used to get number of unread messages from the FireStore database
     * @param userId key auth of user
     * @param number callback number of unread messages
     */
    fun getNumberUnreadMessages(userId: String, number: (Int) -> Unit) {
        db.collection("Conversation${userId}")
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    var num = 0
                    value.forEach { document ->
                        val conversation = document.toObject(Conversation::class.java)
                        num += conversation.numberUnSeen
                    }
                    number.invoke(num)
                }
            }
    }

    /**
     * This function is used to upload list photo to the Storage Firebase
     * @param context context of activity
     * @param uris list uri of photo
     * @param roomId list id room of photo
     * @param process callback when upload is processing
     * @param success callback when upload is successful
     */
    fun uploadListPhoto(
        context: Context,
        uris: ArrayList<Uri>,
        roomId: List<String>,
        process: (Pair<Int, Double>) -> Unit,
        success: (ArrayList<String>) -> Unit,
        failure: ((Throwable) -> Unit)? = null,
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val n = uris.size
            if (n == 0) {
                success.invoke(arrayListOf())
                return@launch
            }
            val urls = ArrayList<String>(n)
            uris.forEachIndexed { index, uri ->
                process(Pair(index, index * 100.0 / n))
                val url = uploadSingleChatMedia(
                    context = context,
                    uri = uri,
                    roomId = roomId,
                    onFileUploadProgress = { filePct ->
                        val overall = (index / n.toDouble() + filePct / 100.0 / n) * 100.0
                        process(Pair(index, overall))
                    }
                ) ?: throw IllegalStateException("Upload failed for item $index")
                urls.add(url)
            }
            success.invoke(urls)
        } catch (t: Throwable) {
            Log.e("FireBaseInstance", "uploadListPhoto", t)
            failure?.invoke(t)
        }
    }

    private suspend fun uploadSingleChatMedia(
        context: Context,
        uri: Uri,
        roomId: List<String>,
        onFileUploadProgress: (Float) -> Unit = { },
    ): String? {
        return if (context.isVideoUri(uri)) {
            uploadVideoToCloud(context, uri, roomId, onFileUploadProgress)
        } else {
            uploadImageToCloud(context, uri, roomId, onFileUploadProgress)
        }
    }

    private suspend fun uploadImageToCloud(
        context: Context,
        uri: Uri,
        roomId: List<String>,
        onFileUploadProgress: (Float) -> Unit = { },
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bytes = context.compressImage(uri)
                onFileUploadProgress(0f)
                val fileName = "${UUID.randomUUID()}.jpg"
                val folder = "$PATH_PHOTO/$roomId"

                CloudinaryManager.uploadBytes(
                    fileBytes = bytes,
                    fileName = fileName,
                    mimeType = "image/jpeg",
                    folder = folder,
                    onSuccess = { url ->
                        continuation.resume(url)
                    },
                    onFailure = { e ->
                        continuation.resumeWithException(e)
                    },
                    onUploadProgress = { p -> onFileUploadProgress(p) },
                )
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    private suspend fun uploadVideoToCloud(
        context: Context,
        uri: Uri,
        roomId: List<String>,
        onFileUploadProgress: (Float) -> Unit = { },
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bytes = context.readUriBytes(uri)
                onFileUploadProgress(0f)
                val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                val ext = when {
                    mime.contains("webm", ignoreCase = true) -> "webm"
                    mime.contains("quicktime", ignoreCase = true) || mime.contains("mov", ignoreCase = true) -> "mov"
                    mime.contains("3gp", ignoreCase = true) -> "3gp"
                    else -> "mp4"
                }
                val fileName = "${UUID.randomUUID()}.$ext"
                val folder = "$PATH_PHOTO/$roomId"

                CloudinaryManager.uploadBytes(
                    fileBytes = bytes,
                    fileName = fileName,
                    mimeType = mime,
                    folder = folder,
                    onSuccess = { url -> continuation.resume(url) },
                    onFailure = { e -> continuation.resumeWithException(e) },
                    onUploadProgress = { p -> onFileUploadProgress(p) },
                )
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    /**
     * This function is used to upload audio to the Storage Firebase
     * @param roomId id room of audio
     * @param uriAudio uri of audio
     * @param process callback (0..100) khi upload lên Cloudinary
     * @param success callback when upload is successful
     * @param failure callback when upload fails
     */
    fun uploadAudio(
        roomId: List<String>,
        uriAudio: Uri,
        success: (String) -> Unit,
        process: (Double) -> Unit = {},
        failure: ((Throwable) -> Unit)? = null,
    ) {
        process(0.0)
        val audioFilePath = uriAudio.path
        val audioFile = audioFilePath?.let { File(it) }
        val bytes = audioFile?.takeIf { it.exists() }?.readBytes()

        if (bytes == null) {
            Log.e(
                "Check fail uploadAudio Cloudinary",
                "uploadAudio failed: cannot read file from uri=$uriAudio path=$audioFilePath"
            )
            failure?.invoke(
                IllegalStateException("Không đọc được file ghi âm")
            )
            return
        }

        process(2.0)
        val fileName = "${UUID.randomUUID()}.mp3"
        // Firebase path: audios/<roomId.toString()>/*
        val folder = "$PATH_AUDIO/$roomId"

        CloudinaryManager.uploadBytes(
            fileBytes = bytes,
            fileName = fileName,
            mimeType = "audio/mpeg",
            folder = folder,
            onSuccess = { url ->
                process(100.0)
                success.invoke(url)
            },
            onFailure = { e ->
                Log.e("Check fail uploadAudio Cloudinary", "uploadAudio failed: ${e.message}", e)
                failure?.invoke(e)
            },
            onUploadProgress = { p ->
                process(2.0 + (p / 100.0) * 98.0)
            },
        )
    }

    /**
     * This function is used to remove message from the FireStore database
     * @param conversation data conversation
     * @param userId key auth of user
     * @param time time message sent
     */
    fun removeMessage(conversation: Conversation, userId: String, time: String) {
        val idRoom = messageThreadDocumentId(conversation, userId)
        val roomRef = db.collection(PATH_MESSAGE).document(idRoom)
        roomRef.get().addOnSuccessListener { roomDoc ->
            db.collection(PATH_MESSAGE)
                .document(idRoom)
                .collection(PATH_CHAT)
                .document(time)
                .delete()
            val pins = parsePinnedMessages(roomDoc)
            if (pins.any { it.messageTime == time }) {
                removePinnedMessage(idRoom, time)
            }
        }
    }

    fun observePinnedMessages(
        idRoom: String,
        success: (List<ChatThreadPin>) -> Unit,
        failure: (String) -> Unit,
    ): ListenerRegistration {
        return db.collection(PATH_MESSAGE)
            .document(idRoom)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    failure.invoke(error.message.orEmpty())
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    success.invoke(emptyList())
                    return@addSnapshotListener
                }
                val pins = parsePinnedMessages(snapshot)
                val legacyPin = parseLegacyPinnedMessage(snapshot)
                when {
                    pins.isNotEmpty() -> success.invoke(pins)
                    legacyPin != null -> {
                        success.invoke(listOf(legacyPin))
                        migrateLegacyPinnedMessage(idRoom, legacyPin)
                    }
                    else -> success.invoke(emptyList())
                }
            }
    }

    fun addPinnedMessage(idRoom: String, pin: ChatThreadPin, maxPins: Int = MAX_PINNED_MESSAGES) {
        val roomRef = db.collection(PATH_MESSAGE).document(idRoom)
        roomRef.get().addOnSuccessListener { snapshot ->
            val current = if (snapshot.exists()) parsePinnedMessages(snapshot) else emptyList()
            val legacy = if (current.isEmpty() && snapshot.exists()) parseLegacyPinnedMessage(snapshot) else null
            val merged = if (legacy != null) listOf(legacy) else current
            if (merged.any { it.messageTime == pin.messageTime }) return@addOnSuccessListener
            if (merged.size >= maxPins) return@addOnSuccessListener
            val updated = merged + pin
            writePinnedMessages(idRoom, updated, clearLegacy = legacy != null || snapshot.exists())
        }
    }

    fun removePinnedMessage(idRoom: String, messageTime: String) {
        val roomRef = db.collection(PATH_MESSAGE).document(idRoom)
        roomRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener
            val current = parsePinnedMessages(snapshot)
            val legacy = if (current.isEmpty()) parseLegacyPinnedMessage(snapshot) else null
            val merged = if (legacy != null) listOf(legacy) else current
            val updated = merged.filterNot { it.messageTime == messageTime }
            if (updated.isEmpty()) {
                clearPinnedMessages(idRoom)
            } else {
                writePinnedMessages(idRoom, updated, clearLegacy = legacy != null)
            }
        }
    }

    fun setPinnedMessagesOrder(idRoom: String, orderedTimes: List<String>) {
        val roomRef = db.collection(PATH_MESSAGE).document(idRoom)
        roomRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener
            val current = parsePinnedMessages(snapshot)
            val legacy = if (current.isEmpty()) parseLegacyPinnedMessage(snapshot) else null
            val merged = if (legacy != null) listOf(legacy) else current
            if (merged.isEmpty()) return@addOnSuccessListener
            val byTime = merged.associateBy { it.messageTime }
            val reordered = orderedTimes.mapNotNull { byTime[it] }
            if (reordered.isEmpty()) return@addOnSuccessListener
            writePinnedMessages(idRoom, reordered, clearLegacy = legacy != null)
        }
    }

    private fun parsePinnedMessages(snapshot: com.google.firebase.firestore.DocumentSnapshot): List<ChatThreadPin> {
        @Suppress("UNCHECKED_CAST")
        val rawList = snapshot.get(PATH_PINNED_MESSAGES) as? List<Map<String, Any?>> ?: return emptyList()
        return rawList.mapNotNull { map -> mapToChatThreadPin(map) }
    }

    private fun parseLegacyPinnedMessage(snapshot: com.google.firebase.firestore.DocumentSnapshot): ChatThreadPin? {
        val messageTime = snapshot.getString(PATH_PINNED_MESSAGE_TIME).orEmpty()
        if (messageTime.isBlank()) return null
        return ChatThreadPin(
            messageTime = messageTime,
            pinnedBy = snapshot.getString(PATH_PINNED_BY).orEmpty(),
            pinnedByName = snapshot.getString(PATH_PINNED_BY_NAME).orEmpty(),
            previewText = snapshot.getString(PATH_PINNED_PREVIEW_TEXT).orEmpty(),
            messageType = snapshot.getLong(PATH_PINNED_MESSAGE_TYPE)?.toInt() ?: 0,
            photoUrl = snapshot.getString(PATH_PINNED_PHOTO_URL),
        )
    }

    private fun mapToChatThreadPin(map: Map<String, Any?>): ChatThreadPin? {
        val messageTime = map["messageTime"] as? String ?: map[PATH_PINNED_MESSAGE_TIME] as? String
        if (messageTime.isNullOrBlank()) return null
        return ChatThreadPin(
            messageTime = messageTime,
            pinnedBy = map["pinnedBy"] as? String ?: "",
            pinnedByName = map["pinnedByName"] as? String ?: "",
            previewText = map["previewText"] as? String ?: map[PATH_PINNED_PREVIEW_TEXT] as? String ?: "",
            messageType = (map["messageType"] as? Long)?.toInt()
                ?: (map["messageType"] as? Int)
                ?: (map[PATH_PINNED_MESSAGE_TYPE] as? Long)?.toInt()
                ?: 0,
            photoUrl = map["photoUrl"] as? String ?: map[PATH_PINNED_PHOTO_URL] as? String,
        )
    }

    private fun pinToFirestoreMap(pin: ChatThreadPin): Map<String, Any?> = mapOf(
        "messageTime" to pin.messageTime,
        "pinnedBy" to pin.pinnedBy,
        "pinnedByName" to pin.pinnedByName,
        "previewText" to pin.previewText,
        "messageType" to pin.messageType,
        "photoUrl" to pin.photoUrl,
    )

    private fun writePinnedMessages(
        idRoom: String,
        pins: List<ChatThreadPin>,
        clearLegacy: Boolean,
    ) {
        val data = hashMapOf<String, Any>(
            PATH_PINNED_MESSAGES to pins.map { pinToFirestoreMap(it) },
        )
        if (clearLegacy) {
            data[PATH_PINNED_MESSAGE_TIME] = FieldValue.delete()
            data[PATH_PINNED_BY] = FieldValue.delete()
            data[PATH_PINNED_BY_NAME] = FieldValue.delete()
            data[PATH_PINNED_PREVIEW_TEXT] = FieldValue.delete()
            data[PATH_PINNED_MESSAGE_TYPE] = FieldValue.delete()
            data[PATH_PINNED_PHOTO_URL] = FieldValue.delete()
        }
        db.collection(PATH_MESSAGE)
            .document(idRoom)
            .set(data, SetOptions.merge())
    }

    private fun migrateLegacyPinnedMessage(idRoom: String, legacyPin: ChatThreadPin) {
        writePinnedMessages(idRoom, listOf(legacyPin), clearLegacy = true)
    }

    private fun clearPinnedMessages(idRoom: String) {
        val data = hashMapOf<String, Any>(
            PATH_PINNED_MESSAGES to FieldValue.delete(),
            PATH_PINNED_MESSAGE_TIME to FieldValue.delete(),
            PATH_PINNED_BY to FieldValue.delete(),
            PATH_PINNED_BY_NAME to FieldValue.delete(),
            PATH_PINNED_PREVIEW_TEXT to FieldValue.delete(),
            PATH_PINNED_MESSAGE_TYPE to FieldValue.delete(),
            PATH_PINNED_PHOTO_URL to FieldValue.delete(),
        )
        db.collection(PATH_MESSAGE)
            .document(idRoom)
            .update(data)
    }

    /**
     * This function is used to update photo cover of user to the FireStore database
     * @param userId key auth of user
     * @param imageCover data image cover
     */
    fun updateImageCover(userId: String, imageCover: String) {
        db.collection(PATH_USER)
            .document(userId)
            .update(PATH_IMAGE_COVER, imageCover)
    }

    /**
     * This function is used to search friend from the FireStore database
     * @param queryText query text search
     * @param success callback when query is successful
     */
    fun searchFriend(queryText: String, success: (ArrayList<User>) -> Unit) {
        val queryLowerCase = MentionParser.removeAccent(queryText.lowercase())

        db.collection(PATH_USER)
            .get()
            .addOnSuccessListener { result ->
                // toObject() does not include the document ID, so set keyAuth manually
                val friends = result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    User(
                        name = data["name"]?.toString() ?: "",
                        email = data["email"]?.toString() ?: "",
                        avatar = data["avatar"]?.toString() ?: "",
                        imageCover = data["imageCover"]?.toString() ?: "",
                        keyAuth = doc.id
                    )
                }.filter { friend ->
                    val nameFriendAccent = MentionParser.removeAccent(friend.name?.lowercase().toString())
                    nameFriendAccent.contains(queryLowerCase)
                }
                if (queryText.isNotEmpty()) {
                    success.invoke(ArrayList(friends))
                } else {
                    success.invoke(arrayListOf())
                }
            }

    }

    /**
     * Toggle a reaction on a message. One reaction per user; same type toggles off.
     */
    fun toggleMessageReaction(
        time: String,
        idRoom: String,
        userId: String,
        type: EmotionType,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        if (time.isBlank() || idRoom.isBlank() || userId.isBlank()) return
        val messageRef = db.collection(PATH_MESSAGE)
            .document(idRoom)
            .collection(PATH_CHAT)
            .document(time)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(messageRef)
            val current = snapshot.toObject(Message::class.java)?.emotion ?: Emotion()
            val merged = current.toggleUserReaction(userId, type)
            if (merged.emotionEmpty()) {
                transaction.update(messageRef, PATH_EMOTION, FieldValue.delete())
            } else {
                transaction.update(messageRef, PATH_EMOTION, merged)
            }
            null
        }.addOnSuccessListener {
            onSuccess.invoke()
        }.addOnFailureListener { error ->
            Log.e("toggleMessageReaction", error.message.orEmpty())
            onFailure.invoke(error.message ?: "Không thể cập nhật cảm xúc")
        }
    }

    /**
     * This function is used to update typing message for conversation
     * @param userId key auth of user
     * @param friendId key auth of friend
     * @param typing boolean value to indicate if the user is typing
     */
    fun updateTypingMessage(userId: String, friendId: String, typing: Boolean) {
        db.collection("Conversation${userId}")
            .document(friendId)
            .update(PATH_TYPING, typing)
    }

    /** Typing indicator for group chats (stored on the group document). */
    fun updateGroupTyping(groupId: String, userId: String, typing: Boolean) {
        if (groupId.isBlank() || userId.isBlank()) return
        db.collection(PATH_GROUPS).document(groupId)
            .update(
                mapOf(
                    "typing" to typing,
                    "typingUserId" to if (typing) userId else "",
                ),
            )
    }

    /**
     * Listens for typing from other members (not [myUserId]).
     */
    fun observeGroupTyping(
        groupId: String,
        myUserId: String,
        onTypingFromOthers: (Boolean) -> Unit,
    ): ListenerRegistration {
        return db.collection(PATH_GROUPS).document(groupId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    onTypingFromOthers(false)
                    return@addSnapshotListener
                }
                val typing = snap.getBoolean("typing") == true
                val uid = snap.getString("typingUserId").orEmpty()
                onTypingFromOthers(typing && uid.isNotBlank() && uid != myUserId)
            }
    }

    /**
     * Send a friend request from [fromId] to [toId].
     * Checks for an existing pending request before creating a new one.
     */
    fun sendFriendRequest(
        fromId: String,
        fromName: String,
        fromAvatar: String,
        toId: String,
        toName: String = "",
        toAvatar: String = "",
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("fromId", fromId)
            .whereEqualTo("toId", toId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    failure.invoke("Đã gửi lời mời kết bạn rồi")
                    return@addOnSuccessListener
                }
                val docRef = db.collection(PATH_FRIEND_REQUESTS).document()
                val request = FriendRequest(
                    requestId = docRef.id,
                    fromId = fromId,
                    toId = toId,
                    fromName = fromName,
                    fromAvatar = fromAvatar,
                    toName = toName,
                    toAvatar = toAvatar,
                    status = FriendRequest.STATUS_PENDING,
                    createdAt = System.currentTimeMillis()
                )
                docRef.set(request)
                    .addOnSuccessListener { success.invoke() }
                    .addOnFailureListener { failure.invoke(it.message.toString()) }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Real-time listener for incoming pending friend requests for [userId].
     */
    fun getIncomingFriendRequests(
        userId: String,
        success: (List<FriendRequest>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("toId", userId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                val list = value?.documents
                    ?.mapNotNull { it.toObject(FriendRequest::class.java) }
                    ?: emptyList()
                success.invoke(list)
            }
    }

    /**
     * Real-time listener for outgoing pending friend requests sent by [userId].
     */
    fun getOutgoingFriendRequests(
        userId: String,
        success: (List<FriendRequest>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("fromId", userId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                val list = value?.documents
                    ?.mapNotNull { it.toObject(FriendRequest::class.java) }
                    ?: emptyList()
                success.invoke(list)
            }
    }

    /**
     * Accept a friend request. Uses a batch write to atomically:
     * - Update request status to "accepted"
     * - Add both users to each other's friends subcollection
     * - If neither user already has a 1:1 inbox row for the other (`Conversation{me}/{other}`),
     *   seed both rows with the "became friends" preview (avoids a second welcome thread after
     *   unfriend / re-friend when the old inbox docs were kept).
     */
    fun acceptFriendRequest(
        request: FriendRequest,
        myName: String,
        myAvatar: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val convSenderRef =
            db.collection("Conversation${request.fromId}").document(request.toId)
        val convAccepterRef =
            db.collection("Conversation${request.toId}").document(request.fromId)

        convSenderRef.get()
            .addOnSuccessListener { snapFrom ->
                convAccepterRef.get()
                    .addOnSuccessListener { snapTo ->
                        val convAlreadyExists = snapFrom.exists() || snapTo.exists()
                        val batch = db.batch()

                        val requestRef =
                            db.collection(PATH_FRIEND_REQUESTS).document(request.requestId)
                        batch.update(requestRef, "status", FriendRequest.STATUS_ACCEPTED)

                        val myFriendRef = db.collection(PATH_USER).document(request.toId)
                            .collection(PATH_FRIENDS).document(request.fromId)
                        batch.set(
                            myFriendRef, Friend(
                                name = request.fromName,
                                avatar = request.fromAvatar,
                                keyAuth = request.fromId,
                                since = System.currentTimeMillis()
                            )
                        )

                        // Accepter on sender's phone book: prefer name/avatar stored on the request when sent
                        // (avoids empty name if getInfoUser snapshot was incomplete — empty name is omitted from
                        // PhoneBook because grouping uses capitalLetters on friend.name).
                        val accepterName = request.toName.ifBlank { myName }
                        val accepterAvatar = request.toAvatar.ifBlank { myAvatar }

                        val theirFriendRef = db.collection(PATH_USER).document(request.fromId)
                            .collection(PATH_FRIENDS).document(request.toId)
                        batch.set(
                            theirFriendRef, Friend(
                                name = accepterName,
                                avatar = accepterAvatar,
                                keyAuth = request.toId,
                                since = System.currentTimeMillis()
                            )
                        )

                        if (!convAlreadyExists) {
                            val time = DateUtils.getTimeCurrent()
                            val becomeFriendsMsg = "Hai bạn đã trở thành bạn bè"

                            batch.set(
                                convSenderRef,
                                Conversation(
                                    friendId = request.toId,
                                    friendImage = accepterAvatar,
                                    message = becomeFriendsMsg,
                                    name = accepterName,
                                    person = "Bạn",
                                    sender = request.fromId,
                                    time = time,
                                    numberUnSeen = 0,
                                    typing = false
                                )
                            )

                            batch.set(
                                convAccepterRef,
                                Conversation(
                                    friendId = request.fromId,
                                    friendImage = request.fromAvatar,
                                    message = becomeFriendsMsg,
                                    name = request.fromName,
                                    person = request.fromName,
                                    sender = request.fromId,
                                    time = time,
                                    numberUnSeen = 0,
                                    typing = false
                                )
                            )
                        }

                        batch.commit()
                            .addOnSuccessListener { success.invoke() }
                            .addOnFailureListener { failure.invoke(it.message.toString()) }
                    }
                    .addOnFailureListener { failure.invoke(it.message.toString()) }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Reject a friend request by updating its status to "rejected".
     */
    fun rejectFriendRequest(
        requestId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS).document(requestId)
            .update("status", FriendRequest.STATUS_REJECTED)
            .addOnSuccessListener { success.invoke() }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Cancel an outgoing friend request sent from [fromId] to [toId].
     * Deletes the pending request document from Firestore.
     */
    fun cancelFriendRequest(
        fromId: String,
        toId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("fromId", fromId)
            .whereEqualTo("toId", toId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    success.invoke()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                result.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { success.invoke() }
                    .addOnFailureListener { failure.invoke(it.message.toString()) }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Real-time listener for the friends list of [userId].
     */
    fun getFriends(
        userId: String,
        success: (List<Friend>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER).document(userId)
            .collection(PATH_FRIENDS)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                val list = value?.documents
                    ?.mapNotNull { it.toObject(Friend::class.java) }
                    ?: emptyList()
                success.invoke(list)
            }
    }

    /**
     * Check the friendship status between [myId] and [targetId].
     * Returns one of: "friend", "pending_sent", "pending_received", "none"
     */
    fun getFriendshipStatus(
        myId: String,
        targetId: String,
        result: (String) -> Unit
    ) {
        db.collection(PATH_USER).document(myId)
            .collection(PATH_FRIENDS).document(targetId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    result.invoke("friend")
                    return@addOnSuccessListener
                }
                db.collection(PATH_FRIEND_REQUESTS)
                    .whereEqualTo("fromId", myId)
                    .whereEqualTo("toId", targetId)
                    .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                    .get()
                    .addOnSuccessListener { sent ->
                        if (!sent.isEmpty) {
                            result.invoke("pending_sent")
                            return@addOnSuccessListener
                        }
                        db.collection(PATH_FRIEND_REQUESTS)
                            .whereEqualTo("fromId", targetId)
                            .whereEqualTo("toId", myId)
                            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                            .get()
                            .addOnSuccessListener { received ->
                                if (!received.isEmpty) result.invoke("pending_received")
                                else result.invoke("none")
                            }
                            .addOnFailureListener { result.invoke("none") }
                    }
                    .addOnFailureListener { result.invoke("none") }
            }
            .addOnFailureListener { result.invoke("none") }
    }

    /**
     * Save a user to the current user's search history.
     * Uses the friend's keyAuth as the document ID so duplicate entries are overwritten.
     */
    fun saveSearchHistory(
        myId: String,
        user: User,
        success: () -> Unit = {},
        failure: (String) -> Unit = {}
    ) {
        val data = hashMapOf(
            "name" to (user.name ?: ""),
            "avatar" to (user.avatar ?: ""),
            "keyAuth" to (user.keyAuth ?: ""),
            "searchedAt" to System.currentTimeMillis()
        )
        db.collection(PATH_SEARCH_HISTORY)
            .document(myId)
            .collection(PATH_ITEMS)
            .document(user.keyAuth ?: return)
            .set(data)
            .addOnSuccessListener { success.invoke() }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Fetch the current user's search history ordered by most recent first.
     */
    fun getSearchHistory(
        myId: String,
        success: (List<User>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_SEARCH_HISTORY)
            .document(myId)
            .collection(PATH_ITEMS)
            .orderBy("searchedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    User(
                        name = data["name"]?.toString() ?: "",
                        avatar = data["avatar"]?.toString() ?: "",
                        keyAuth = data["keyAuth"]?.toString() ?: doc.id
                    )
                }
                success.invoke(list)
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    fun getSticker(sticker: Sticker, onSuccess: (List<String>) -> Unit) {
        if (sticker == Sticker.CONGRATULATION) {
            Log.e("getSticker", "init")
        }
        val stickerPaths = arrayListOf<String>()
        db.collection(PATH_STICKER)
            .document(sticker.value)
            .get()
            .addOnSuccessListener {
                it.data?.map { doc ->
                    if (sticker == Sticker.CONGRATULATION) {
                        Log.e("getSticker", "key: ${doc.key}, value: ${doc.value}")
                    }
                    stickerPaths.add(doc.key)
                }
                onSuccess.invoke(stickerPaths)
            }
            .addOnFailureListener {
                Log.e("getSticker", it.message.toString())
            }
    }

    // region Diary posts (Firestore)

    /**
     * Số bài tối đa lấy mỗi chunk (whereIn). Không dùng orderBy trên server để tránh bắt buộc composite index;
     * sắp xếp theo [DiaryPost.createdAtMillis] khi merge.
     */
    private const val DIARY_FEED_LIMIT_PER_CHUNK = 80

    /** Số bài tối đa sau khi merge + sort (client). */
    private const val DIARY_FEED_MAX_DISPLAY = 50

    /**
     * Local file URIs → Cloudinary URLs; [http/https] giữ nguyên (ảnh đã upload khi sửa bài).
     */
    private fun resolveDiaryImageUrls(
        context: Context,
        uris: List<Uri>,
        onDone: (List<String>) -> Unit,
        failure: (String) -> Unit
    ) {
        if (uris.isEmpty()) {
            onDone(emptyList())
            return
        }
        val urls = mutableListOf<String>()
        fun next(index: Int) {
            if (index >= uris.size) {
                onDone(urls)
                return
            }
            val uri = uris[index]
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                urls.add(uri.toString())
                next(index + 1)
            } else {
                uploadImage(
                    context,
                    uri,
                    success = { url ->
                        urls.add(url)
                        next(index + 1)
                    },
                    failure = failure
                )
            }
        }
        next(0)
    }

    /**
     * Uploads local images then creates [DiaryPostFirestore.COLLECTION] document.
     */
    fun createDiaryPost(
        context: Context,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        content: String,
        localImageUris: List<Uri>,
        linkPreview: DiaryLinkPreview? = null,
        success: (postId: String) -> Unit,
        failure: (String) -> Unit
    ) {
        val col = db.collection(DiaryPostFirestore.COLLECTION)
        fun writePost(imageUrls: List<String>) {
            val doc = col.document()
            val data = hashMapOf<String, Any>(
                DiaryPostFirestore.FIELD_AUTHOR_ID to authorId,
                DiaryPostFirestore.FIELD_AUTHOR_NAME to authorName,
                DiaryPostFirestore.FIELD_AUTHOR_AVATAR to authorAvatarUrl,
                DiaryPostFirestore.FIELD_CONTENT to content,
                DiaryPostFirestore.FIELD_IMAGE_URLS to imageUrls,
                DiaryPostFirestore.FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                DiaryPostFirestore.FIELD_LIKE_COUNT to 0,
                DiaryPostFirestore.FIELD_COMMENT_COUNT to 0
            )
            if (linkPreview != null) {
                data[DiaryPostFirestore.FIELD_LINK_PREVIEW] = hashMapOf(
                    DiaryPostFirestore.LINK_FIELD_URL to linkPreview.url,
                    DiaryPostFirestore.LINK_FIELD_TITLE to linkPreview.title,
                    DiaryPostFirestore.LINK_FIELD_DESCRIPTION to linkPreview.description,
                    DiaryPostFirestore.LINK_FIELD_IMAGE_URL to linkPreview.imageUrl.orEmpty()
                )
            }
            doc.set(data)
                .addOnSuccessListener { success(doc.id) }
                .addOnFailureListener {
                    failure(it.message ?: "Error")
                }
        }
        resolveDiaryImageUrls(context, localImageUris, onDone = { writePost(it) }, failure = failure)
    }

    fun getDiaryPost(
        postId: String,
        success: (DiaryPost) -> Unit,
        failure: (String) -> Unit
    ) {
        if (postId.isBlank()) {
            failure("Error")
            return
        }
        db.collection(DiaryPostFirestore.COLLECTION).document(postId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    failure("Error")
                    return@addOnSuccessListener
                }
                val post = DiaryPostFirestore.fromDocument(doc)
                if (post != null) success(post)
                else failure("Error")
            }
            .addOnFailureListener {
                failure(it.message ?: "Error")
            }
    }

    fun updateDiaryPost(
        context: Context,
        postId: String,
        editorUserId: String,
        content: String,
        imageUris: List<Uri>,
        linkPreview: DiaryLinkPreview?,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val ref = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    failure("Error")
                    return@addOnSuccessListener
                }
                val authorId = snap.getString(DiaryPostFirestore.FIELD_AUTHOR_ID).orEmpty()
                if (authorId != editorUserId) {
                    failure("Error")
                    return@addOnSuccessListener
                }
                resolveDiaryImageUrls(
                    context,
                    imageUris,
                    onDone = { urls ->
                        val updates = hashMapOf<String, Any>(
                            DiaryPostFirestore.FIELD_CONTENT to content,
                            DiaryPostFirestore.FIELD_IMAGE_URLS to urls,
                            DiaryPostFirestore.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                        )
                        if (linkPreview != null) {
                            updates[DiaryPostFirestore.FIELD_LINK_PREVIEW] = hashMapOf(
                                DiaryPostFirestore.LINK_FIELD_URL to linkPreview.url,
                                DiaryPostFirestore.LINK_FIELD_TITLE to linkPreview.title,
                                DiaryPostFirestore.LINK_FIELD_DESCRIPTION to linkPreview.description,
                                DiaryPostFirestore.LINK_FIELD_IMAGE_URL to linkPreview.imageUrl.orEmpty()
                            )
                        } else {
                            updates[DiaryPostFirestore.FIELD_LINK_PREVIEW] = FieldValue.delete()
                        }
                        ref.update(updates)
                            .addOnSuccessListener { success() }
                            .addOnFailureListener {
                                failure(it.message ?: "Error")
                            }
                    },
                    failure = failure
                )
            }
            .addOnFailureListener {
                failure(it.message ?: "Error")
            }
    }

    fun deleteDiaryPost(
        postId: String,
        editorUserId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val ref = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    success()
                    return@addOnSuccessListener
                }
                val authorId = snap.getString(DiaryPostFirestore.FIELD_AUTHOR_ID).orEmpty()
                if (authorId != editorUserId) {
                    failure("Error")
                    return@addOnSuccessListener
                }
                ref.delete()
                    .addOnSuccessListener { success() }
                    .addOnFailureListener {
                        failure(it.message ?: "Error")
                    }
            }
            .addOnFailureListener {
                failure(it.message ?: "Error")
            }
    }

    /**
     * Real-time diary feed: posts where [authorId] is in `me + friends`.
     * Firestore [Query.whereIn] allows at most 10 values — friends are chunked.
     */
    fun observeDiaryFeed(
        userId: String,
        onPosts: (List<DiaryPost>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val postsCol = db.collection(DiaryPostFirestore.COLLECTION)
        val chunkPosts = mutableMapOf<Int, Map<String, DiaryPost>>()
        val myReactionTypes = mutableMapOf<String, String>()
        val reactionRegs = mutableMapOf<String, ListenerRegistration>()
        val legacyLikeRegs = mutableMapOf<String, ListenerRegistration>()
        val postChunkRegs = mutableListOf<ListenerRegistration>()
        var friendsReg: ListenerRegistration? = null

        fun mergeAndEmit() {
            val merged = linkedMapOf<String, DiaryPost>()
            chunkPosts.values.forEach { map ->
                map.forEach { (id, post) -> merged[id] = post }
            }
            val list = merged.values
                .map { p ->
                    val typeName = myReactionTypes[p.id].orEmpty()
                    p.copy(
                        likedByMe = typeName.isNotBlank(),
                        myReactionType = typeName,
                    )
                }
                .sortedByDescending { it.createdAtMillis }
                .take(DIARY_FEED_MAX_DISPLAY)
            onPosts(list)
        }

        fun syncMyReactionListeners(visiblePostIds: Set<String>) {
            val toRemove = reactionRegs.keys - visiblePostIds
            toRemove.forEach { pid ->
                reactionRegs.remove(pid)?.remove()
                legacyLikeRegs.remove(pid)?.remove()
                myReactionTypes.remove(pid)
            }
            val toAdd = visiblePostIds - reactionRegs.keys
            toAdd.forEach { postId ->
                val postRef = postsCol.document(postId)
                val reactionReg = postRef
                    .collection(DiaryPostFirestore.SUB_REACTIONS)
                    .document(userId)
                    .addSnapshotListener { snap, _ ->
                        val type = snap?.getString(DiaryPostFirestore.REACTION_FIELD_TYPE).orEmpty()
                        if (type.isNotBlank()) {
                            myReactionTypes[postId] = type
                            mergeAndEmit()
                        } else {
                            myReactionTypes.remove(postId)
                            mergeAndEmit()
                        }
                    }
                reactionRegs[postId] = reactionReg
                val legacyReg = postRef
                    .collection(DiaryPostFirestore.SUB_LIKES)
                    .document(userId)
                    .addSnapshotListener { snap, _ ->
                        if (myReactionTypes[postId].isNullOrBlank() && snap?.exists() == true) {
                            myReactionTypes[postId] = EmotionType.LIKE.name
                            mergeAndEmit()
                        }
                    }
                legacyLikeRegs[postId] = legacyReg
            }
        }

        fun mergeKeys(): Set<String> {
            val keys = mutableSetOf<String>()
            chunkPosts.values.forEach { m -> keys.addAll(m.keys) }
            return keys
        }

        fun attachPostListeners(authorIds: List<String>) {
            postChunkRegs.forEach { it.remove() }
            postChunkRegs.clear()
            chunkPosts.clear()
            if (authorIds.isEmpty()) {
                mergeAndEmit()
                syncMyReactionListeners(emptySet())
                return
            }
            // whereIn tối đa 10 giá trị — chunk theo authorId.
            // Không orderBy trên Firestore: tránh lỗi index + listener gọi onError lặp khi chưa deploy composite index.
            val chunks = authorIds.distinct().chunked(10)
            chunks.forEachIndexed { chunkIndex, chunk ->
                val reg = postsCol
                    .whereIn(DiaryPostFirestore.FIELD_AUTHOR_ID, chunk)
                    .limit(DIARY_FEED_LIMIT_PER_CHUNK.toLong())
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            onError(
                                err.message ?: "Error"
                            )
                            return@addSnapshotListener
                        }
                        val map = snap?.documents?.mapNotNull { doc ->
                            DiaryPostFirestore.fromDocument(doc, likedByMe = false)
                                ?.let { d -> d.id to d }
                        }?.toMap().orEmpty()
                        chunkPosts[chunkIndex] = map
                        mergeAndEmit()
                        syncMyReactionListeners(mergeKeys())
                    }
                    postChunkRegs.add(reg)
            }
            mergeAndEmit()
            syncMyReactionListeners(mergeKeys())
        }

        friendsReg = db.collection(PATH_USER).document(userId)
            .collection(PATH_FRIENDS)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError(
                        error.message ?: "Error"
                    )
                    return@addSnapshotListener
                }
                val friendIds = value?.documents?.map { it.id }.orEmpty()
                val authorIds = (listOf(userId) + friendIds).distinct().filter { it.isNotBlank() }
                reactionRegs.values.forEach { it.remove() }
                reactionRegs.clear()
                legacyLikeRegs.values.forEach { it.remove() }
                legacyLikeRegs.clear()
                myReactionTypes.clear()
                attachPostListeners(authorIds)
            }

        return {
            friendsReg?.remove()
            friendsReg = null
            postChunkRegs.forEach { it.remove() }
            postChunkRegs.clear()
            reactionRegs.values.forEach { it.remove() }
            reactionRegs.clear()
            legacyLikeRegs.values.forEach { it.remove() }
            legacyLikeRegs.clear()
            chunkPosts.clear()
            myReactionTypes.clear()
        }
    }

    fun setDiaryPostReaction(
        postId: String,
        userId: String,
        reactionType: DomainEmotionType,
        currentReaction: DomainEmotionType?,
        actorName: String,
        actorAvatarUrl: String,
        success: () -> Unit,
        failure: (String) -> Unit,
    ) {
        val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        val reactionRef = postRef.collection(DiaryPostFirestore.SUB_REACTIONS).document(userId)
        val legacyLikeRef = postRef.collection(DiaryPostFirestore.SUB_LIKES).document(userId)
        postRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    failure("Error")
                    return@addOnSuccessListener
                }
                val authorId = snap.getString(DiaryPostFirestore.FIELD_AUTHOR_ID).orEmpty()
                val summary = DiaryPostFirestore.parseEmotionSummary(
                    snap.get(DiaryPostFirestore.FIELD_EMOTION_SUMMARY),
                ).toMutableMap()
                val batch = db.batch()
                val typeKey = reactionType.name
                val isRemoving = currentReaction == reactionType

                if (isRemoving) {
                    batch.delete(reactionRef)
                    batch.delete(legacyLikeRef)
                    decrementSummary(summary, currentReaction!!.name)
                    batch.update(postRef, DiaryPostFirestore.FIELD_LIKE_COUNT, FieldValue.increment(-1))
                } else {
                    if (currentReaction != null) {
                        decrementSummary(summary, currentReaction.name)
                    } else {
                        batch.update(postRef, DiaryPostFirestore.FIELD_LIKE_COUNT, FieldValue.increment(1))
                    }
                    incrementSummary(summary, typeKey)
                    batch.set(
                        reactionRef,
                        mapOf(
                            DiaryPostFirestore.REACTION_FIELD_TYPE to typeKey,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                    if (reactionType == DomainEmotionType.LIKE) {
                        batch.set(legacyLikeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                    } else {
                        batch.delete(legacyLikeRef)
                    }
                }
                batch.update(postRef, DiaryPostFirestore.FIELD_EMOTION_SUMMARY, summary)
                batch.commit()
                    .addOnSuccessListener {
                        if (!isRemoving && currentReaction == null && authorId.isNotBlank() && authorId != userId) {
                            val preview = snap.getString(DiaryPostFirestore.FIELD_CONTENT).orEmpty()
                            @Suppress("UNCHECKED_CAST")
                            val images = snap.get(DiaryPostFirestore.FIELD_IMAGE_URLS) as? List<*>
                            val thumb = images?.firstOrNull()?.toString().orEmpty()
                            createDiaryNotification(
                                recipientId = authorId,
                                type = DiaryNotificationType.POST_REACTION,
                                actorId = userId,
                                actorName = actorName,
                                actorAvatarUrl = actorAvatarUrl,
                                postId = postId,
                                reactionType = reactionType,
                                postPreviewText = preview,
                                postThumbnailUrl = thumb,
                            )
                        }
                        success()
                    }
                    .addOnFailureListener { failure(it.message ?: "Error") }
            }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun toggleDiaryPostLike(
        postId: String,
        userId: String,
        currentlyLiked: Boolean,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        setDiaryPostReaction(
            postId = postId,
            userId = userId,
            reactionType = DomainEmotionType.LIKE,
            currentReaction = if (currentlyLiked) DomainEmotionType.LIKE else null,
            actorName = "",
            actorAvatarUrl = "",
            success = success,
            failure = failure,
        )
    }

    fun addDiaryComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        text: String,
        success: (commentId: String) -> Unit,
        failure: (String) -> Unit
    ) {
        val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        val newRef = postRef.collection(DiaryPostFirestore.SUB_COMMENTS).document()
        val data = hashMapOf<String, Any>(
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_ID to authorId,
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_NAME to authorName,
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_AVATAR to authorAvatarUrl,
            DiaryPostFirestore.COMMENT_FIELD_TEXT to text.trim(),
            DiaryPostFirestore.COMMENT_FIELD_CREATED_AT to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(newRef, data)
        batch.update(postRef, DiaryPostFirestore.FIELD_COMMENT_COUNT, FieldValue.increment(1))
        batch.commit()
            .addOnSuccessListener {
                success(newRef.id)
                postRef.get().addOnSuccessListener { postSnap ->
                    val postAuthorId = postSnap.getString(DiaryPostFirestore.FIELD_AUTHOR_ID).orEmpty()
                    if (postAuthorId.isNotBlank() && postAuthorId != authorId) {
                        val preview = postSnap.getString(DiaryPostFirestore.FIELD_CONTENT).orEmpty()
                        @Suppress("UNCHECKED_CAST")
                        val images = postSnap.get(DiaryPostFirestore.FIELD_IMAGE_URLS) as? List<*>
                        createDiaryNotification(
                            recipientId = postAuthorId,
                            type = DiaryNotificationType.POST_COMMENT,
                            actorId = authorId,
                            actorName = authorName,
                            actorAvatarUrl = authorAvatarUrl,
                            postId = postId,
                            commentId = newRef.id,
                            postPreviewText = preview,
                            postThumbnailUrl = images?.firstOrNull()?.toString().orEmpty(),
                            commentPreviewText = text.trim(),
                        )
                    }
                }
            }
            .addOnFailureListener {
                failure(
                    it.message ?: "Error"
                )
            }
    }

    fun observeDiaryComments(
        postId: String,
        userId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val commentsCol = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS)
        var comments = emptyList<DiaryPostComment>()
        val likedByMe = mutableMapOf<String, Boolean>()
        val likeRegs = mutableMapOf<String, ListenerRegistration>()

        fun emit() {
            onUpdate(comments.map { it.copy(likedByMe = likedByMe[it.id] ?: false) })
        }

        fun syncLikeListeners(visibleIds: Set<String>) {
            (likeRegs.keys - visibleIds).forEach { id -> likeRegs.remove(id)?.remove() }
            (visibleIds - likeRegs.keys).forEach { id ->
                if (userId.isBlank()) return@forEach
                likeRegs[id] = commentsCol.document(id)
                    .collection(DiaryPostFirestore.SUB_COMMENT_LIKES)
                    .document(userId)
                    .addSnapshotListener { snap, _ ->
                        likedByMe[id] = snap?.exists() == true
                        emit()
                    }
            }
        }

        val commentsReg = commentsCol
            .orderBy(DiaryPostFirestore.COMMENT_FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err.message ?: "Error")
                    return@addSnapshotListener
                }
                comments = snap?.documents?.mapNotNull { doc ->
                    DiaryPostFirestore.commentFromDocument(postId, doc)
                }.orEmpty()
                syncLikeListeners(comments.map { it.id }.toSet())
                emit()
            }

        return {
            commentsReg.remove()
            likeRegs.values.forEach { it.remove() }
            likeRegs.clear()
        }
    }

    fun editDiaryComment(
        postId: String,
        commentId: String,
        newText: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
            .update(DiaryPostFirestore.COMMENT_FIELD_TEXT, newText.trim())
            .addOnSuccessListener { success() }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun deleteDiaryComment(
        postId: String,
        commentId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        val commentRef = postRef.collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
        val batch = db.batch()
        batch.delete(commentRef)
        batch.update(postRef, DiaryPostFirestore.FIELD_COMMENT_COUNT, FieldValue.increment(-1))
        batch.commit()
            .addOnSuccessListener { success() }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun editDiaryReply(
        postId: String,
        commentId: String,
        replyId: String,
        newText: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
            .collection(DiaryPostFirestore.SUB_REPLIES).document(replyId)
            .update(DiaryPostFirestore.COMMENT_FIELD_TEXT, newText.trim())
            .addOnSuccessListener { success() }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun deleteDiaryReply(
        postId: String,
        commentId: String,
        replyId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val commentRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
        val replyRef = commentRef.collection(DiaryPostFirestore.SUB_REPLIES).document(replyId)
        val batch = db.batch()
        batch.delete(replyRef)
        batch.update(commentRef, DiaryPostFirestore.COMMENT_FIELD_REPLY_COUNT, FieldValue.increment(-1))
        batch.commit()
            .addOnSuccessListener { success() }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun toggleDiaryCommentLike(
        postId: String,
        commentId: String,
        userId: String,
        currentlyLiked: Boolean,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        val commentRef = postRef.collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
        val likeRef = commentRef.collection(DiaryPostFirestore.SUB_COMMENT_LIKES).document(userId)
        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            batch.update(commentRef, DiaryPostFirestore.COMMENT_FIELD_LIKE_COUNT, FieldValue.increment(-1))
        } else {
            batch.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
            batch.update(commentRef, DiaryPostFirestore.COMMENT_FIELD_LIKE_COUNT, FieldValue.increment(1))
        }
        batch.commit()
            .addOnSuccessListener {
                if (!currentlyLiked) {
                    commentRef.get().addOnSuccessListener { commentSnap ->
                        val commentAuthorId = commentSnap.getString(DiaryPostFirestore.COMMENT_FIELD_AUTHOR_ID).orEmpty()
                        if (commentAuthorId.isNotBlank() && commentAuthorId != userId) {
                            postRef.get().addOnSuccessListener { postSnap ->
                                val preview = postSnap.getString(DiaryPostFirestore.FIELD_CONTENT).orEmpty()
                                @Suppress("UNCHECKED_CAST")
                                val images = postSnap.get(DiaryPostFirestore.FIELD_IMAGE_URLS) as? List<*>
                                createDiaryNotification(
                                    recipientId = commentAuthorId,
                                    type = DiaryNotificationType.COMMENT_LIKE,
                                    actorId = userId,
                                    actorName = "",
                                    actorAvatarUrl = "",
                                    postId = postId,
                                    commentId = commentId,
                                    postPreviewText = preview,
                                    postThumbnailUrl = images?.firstOrNull()?.toString().orEmpty(),
                                    commentPreviewText = commentSnap.getString(DiaryPostFirestore.COMMENT_FIELD_TEXT).orEmpty(),
                                )
                            }
                        }
                    }
                }
                success()
            }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun addDiaryReply(
        postId: String,
        commentId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        text: String,
        mentionedUserId: String,
        mentionedName: String,
        success: (replyId: String) -> Unit,
        failure: (String) -> Unit
    ) {
        val commentRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
        val newRef = commentRef.collection(DiaryPostFirestore.SUB_REPLIES).document()
        val data = hashMapOf<String, Any>(
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_ID to authorId,
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_NAME to authorName,
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_AVATAR to authorAvatarUrl,
            DiaryPostFirestore.COMMENT_FIELD_TEXT to text.trim(),
            DiaryPostFirestore.COMMENT_FIELD_PARENT_ID to commentId,
            DiaryPostFirestore.COMMENT_FIELD_MENTIONED_ID to mentionedUserId,
            DiaryPostFirestore.COMMENT_FIELD_MENTIONED_NAME to mentionedName,
            DiaryPostFirestore.COMMENT_FIELD_CREATED_AT to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(newRef, data)
        batch.update(commentRef, DiaryPostFirestore.COMMENT_FIELD_REPLY_COUNT, FieldValue.increment(1))
        batch.commit()
            .addOnSuccessListener {
                success(newRef.id)
                val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
                postRef.get().addOnSuccessListener { postSnap ->
                    val preview = postSnap.getString(DiaryPostFirestore.FIELD_CONTENT).orEmpty()
                    @Suppress("UNCHECKED_CAST")
                    val images = postSnap.get(DiaryPostFirestore.FIELD_IMAGE_URLS) as? List<*>
                    val thumb = images?.firstOrNull()?.toString().orEmpty()
                    commentRef.get().addOnSuccessListener { commentSnap ->
                        val commentAuthorId = commentSnap.getString(DiaryPostFirestore.COMMENT_FIELD_AUTHOR_ID).orEmpty()
                        val recipients = linkedSetOf<String>()
                        if (commentAuthorId.isNotBlank() && commentAuthorId != authorId) {
                            recipients.add(commentAuthorId)
                        }
                        if (mentionedUserId.isNotBlank() && mentionedUserId != authorId) {
                            recipients.add(mentionedUserId)
                        }
                        recipients.forEach { recipientId ->
                            createDiaryNotification(
                                recipientId = recipientId,
                                type = DiaryNotificationType.COMMENT_REPLY,
                                actorId = authorId,
                                actorName = authorName,
                                actorAvatarUrl = authorAvatarUrl,
                                postId = postId,
                                commentId = commentId,
                                replyId = newRef.id,
                                postPreviewText = preview,
                                postThumbnailUrl = thumb,
                                commentPreviewText = text.trim(),
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun observeDiaryReplies(
        postId: String,
        commentId: String,
        userId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val repliesCol = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
            .collection(DiaryPostFirestore.SUB_REPLIES)
        var replies = emptyList<DiaryPostComment>()
        val likedByMe = mutableMapOf<String, Boolean>()
        val likeRegs = mutableMapOf<String, ListenerRegistration>()

        fun emit() {
            onUpdate(replies.map { it.copy(likedByMe = likedByMe[it.id] ?: false) })
        }

        fun syncLikeListeners(visibleIds: Set<String>) {
            (likeRegs.keys - visibleIds).forEach { id -> likeRegs.remove(id)?.remove() }
            (visibleIds - likeRegs.keys).forEach { id ->
                if (userId.isBlank()) return@forEach
                likeRegs[id] = repliesCol.document(id)
                    .collection(DiaryPostFirestore.SUB_COMMENT_LIKES)
                    .document(userId)
                    .addSnapshotListener { snap, _ ->
                        likedByMe[id] = snap?.exists() == true
                        emit()
                    }
            }
        }

        val repliesReg = repliesCol
            .orderBy(DiaryPostFirestore.COMMENT_FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err.message ?: "Error")
                    return@addSnapshotListener
                }
                replies = snap?.documents?.mapNotNull { doc ->
                    DiaryPostFirestore.replyFromDocument(postId, commentId, doc)
                }.orEmpty()
                syncLikeListeners(replies.map { it.id }.toSet())
                emit()
            }

        return {
            repliesReg.remove()
            likeRegs.values.forEach { it.remove() }
            likeRegs.clear()
        }
    }

    fun toggleDiaryReplyLike(
        postId: String,
        commentId: String,
        replyId: String,
        userId: String,
        currentlyLiked: Boolean,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val replyRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS).document(commentId)
            .collection(DiaryPostFirestore.SUB_REPLIES).document(replyId)
        val likeRef = replyRef.collection(DiaryPostFirestore.SUB_COMMENT_LIKES).document(userId)
        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            batch.update(replyRef, DiaryPostFirestore.COMMENT_FIELD_LIKE_COUNT, FieldValue.increment(-1))
        } else {
            batch.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
            batch.update(replyRef, DiaryPostFirestore.COMMENT_FIELD_LIKE_COUNT, FieldValue.increment(1))
        }
        batch.commit()
            .addOnSuccessListener {
                if (!currentlyLiked) {
                    val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
                    replyRef.get().addOnSuccessListener { replySnap ->
                        val replyAuthorId = replySnap.getString(DiaryPostFirestore.COMMENT_FIELD_AUTHOR_ID).orEmpty()
                        if (replyAuthorId.isNotBlank() && replyAuthorId != userId) {
                            postRef.get().addOnSuccessListener { postSnap ->
                                val preview = postSnap.getString(DiaryPostFirestore.FIELD_CONTENT).orEmpty()
                                @Suppress("UNCHECKED_CAST")
                                val images = postSnap.get(DiaryPostFirestore.FIELD_IMAGE_URLS) as? List<*>
                                createDiaryNotification(
                                    recipientId = replyAuthorId,
                                    type = DiaryNotificationType.REPLY_LIKE,
                                    actorId = userId,
                                    actorName = "",
                                    actorAvatarUrl = "",
                                    postId = postId,
                                    commentId = commentId,
                                    replyId = replyId,
                                    postPreviewText = preview,
                                    postThumbnailUrl = images?.firstOrNull()?.toString().orEmpty(),
                                    commentPreviewText = replySnap.getString(DiaryPostFirestore.COMMENT_FIELD_TEXT).orEmpty(),
                                )
                            }
                        }
                    }
                }
                success()
            }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun observeDiaryNotifications(
        userId: String,
        onUpdate: (List<DiaryNotification>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit {
        if (userId.isBlank()) {
            onUpdate(emptyList())
            return {}
        }
        val reg = db.collection(PATH_USER).document(userId)
            .collection(DiaryNotificationFirestore.SUB_COLLECTION)
            .orderBy(DiaryNotificationFirestore.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err.message ?: "Error")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    DiaryNotificationFirestore.fromDocument(doc)
                }.orEmpty()
                onUpdate(list)
            }
        return { reg.remove() }
    }

    fun markDiaryNotificationRead(
        userId: String,
        notificationId: String,
        success: () -> Unit,
        failure: (String) -> Unit,
    ) {
        if (userId.isBlank() || notificationId.isBlank()) {
            failure("Error")
            return
        }
        db.collection(PATH_USER).document(userId)
            .collection(DiaryNotificationFirestore.SUB_COLLECTION)
            .document(notificationId)
            .update(DiaryNotificationFirestore.FIELD_READ, true)
            .addOnSuccessListener { success() }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    fun markAllDiaryNotificationsRead(
        userId: String,
        success: () -> Unit,
        failure: (String) -> Unit,
    ) {
        if (userId.isBlank()) {
            failure("Error")
            return
        }
        db.collection(PATH_USER).document(userId)
            .collection(DiaryNotificationFirestore.SUB_COLLECTION)
            .whereEqualTo(DiaryNotificationFirestore.FIELD_READ, false)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    success()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, DiaryNotificationFirestore.FIELD_READ, true)
                }
                batch.commit()
                    .addOnSuccessListener { success() }
                    .addOnFailureListener { failure(it.message ?: "Error") }
            }
            .addOnFailureListener { failure(it.message ?: "Error") }
    }

    private fun incrementSummary(summary: MutableMap<String, Int>, typeKey: String) {
        summary[typeKey] = (summary[typeKey] ?: 0) + 1
    }

    private fun decrementSummary(summary: MutableMap<String, Int>, typeKey: String) {
        val next = (summary[typeKey] ?: 0) - 1
        if (next <= 0) summary.remove(typeKey) else summary[typeKey] = next
    }

    private fun resolveActorProfile(
        actorId: String,
        actorName: String,
        actorAvatarUrl: String,
        onReady: (name: String, avatar: String) -> Unit,
    ) {
        if (actorName.isNotBlank()) {
            onReady(actorName, actorAvatarUrl)
            return
        }
        if (actorId.isBlank()) {
            onReady("", "")
            return
        }
        db.collection(PATH_USER).document(actorId).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                onReady(user?.name.orEmpty(), user?.avatar.orEmpty())
            }
            .addOnFailureListener { onReady("", "") }
    }

    private fun createDiaryNotification(
        recipientId: String,
        type: DiaryNotificationType,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String,
        postId: String,
        commentId: String = "",
        replyId: String = "",
        reactionType: DomainEmotionType? = null,
        postPreviewText: String = "",
        postThumbnailUrl: String = "",
        commentPreviewText: String = "",
    ) {
        if (recipientId.isBlank() || actorId.isBlank() || recipientId == actorId || postId.isBlank()) return
        resolveActorProfile(actorId, actorName, actorAvatarUrl) { resolvedName, resolvedAvatar ->
            val ref = db.collection(PATH_USER).document(recipientId)
                .collection(DiaryNotificationFirestore.SUB_COLLECTION)
                .document()
            val data = hashMapOf<String, Any>(
                DiaryNotificationFirestore.FIELD_TYPE to type.name,
                DiaryNotificationFirestore.FIELD_ACTOR_ID to actorId,
                DiaryNotificationFirestore.FIELD_ACTOR_NAME to resolvedName,
                DiaryNotificationFirestore.FIELD_ACTOR_AVATAR to resolvedAvatar,
                DiaryNotificationFirestore.FIELD_POST_ID to postId,
                DiaryNotificationFirestore.FIELD_READ to false,
                DiaryNotificationFirestore.FIELD_CREATED_AT to FieldValue.serverTimestamp(),
            )
            if (commentId.isNotBlank()) data[DiaryNotificationFirestore.FIELD_COMMENT_ID] = commentId
            if (replyId.isNotBlank()) data[DiaryNotificationFirestore.FIELD_REPLY_ID] = replyId
            if (reactionType != null) {
                data[DiaryNotificationFirestore.FIELD_REACTION_TYPE] = reactionType.name
            }
            if (postPreviewText.isNotBlank()) {
                data[DiaryNotificationFirestore.FIELD_POST_PREVIEW] = postPreviewText.take(200)
            }
            if (postThumbnailUrl.isNotBlank()) {
                data[DiaryNotificationFirestore.FIELD_POST_THUMBNAIL] = postThumbnailUrl
            }
            if (commentPreviewText.isNotBlank()) {
                data[DiaryNotificationFirestore.FIELD_COMMENT_PREVIEW] = commentPreviewText.take(200)
            }
            ref.set(data)
                .addOnSuccessListener {
                    sendDiaryPushNotification(
                        recipientId = recipientId,
                        actorId = actorId,
                        actorName = resolvedName.ifBlank { "Người dùng" },
                        type = type,
                        postId = postId,
                        commentId = commentId,
                        replyId = replyId,
                        reactionType = reactionType,
                        previewText = commentPreviewText.ifBlank { postPreviewText },
                    )
                }
        }
    }

    private fun sendDiaryPushNotification(
        recipientId: String,
        actorId: String,
        actorName: String,
        type: DiaryNotificationType,
        postId: String,
        commentId: String = "",
        replyId: String = "",
        reactionType: DomainEmotionType? = null,
        previewText: String = "",
    ) {
        getTokenMessage(
            recipientId,
            success = { token ->
                val body = when (type) {
                    DiaryNotificationType.POST_REACTION -> "$actorName đã bày tỏ cảm xúc với bài viết của bạn"
                    DiaryNotificationType.POST_COMMENT -> "$actorName đã bình luận về bài viết của bạn"
                    DiaryNotificationType.COMMENT_LIKE -> "$actorName đã thích bình luận của bạn"
                    DiaryNotificationType.COMMENT_REPLY -> "$actorName đã trả lời bình luận của bạn"
                    DiaryNotificationType.REPLY_LIKE -> "$actorName đã thích câu trả lời của bạn"
                }
                val notification = NotificationData(
                    token = token,
                    data = Data(
                        title = actorName,
                        body = if (previewText.isNotBlank()) previewText else body,
                        senderId = actorId,
                        diaryNotificationType = type.name,
                        recipientUserId = recipientId,
                        postId = postId,
                        commentId = commentId.ifBlank { null },
                        replyId = replyId.ifBlank { null },
                        reactionType = reactionType?.name,
                    ),
                )
                enqueueSendMessageApi(notification)
            },
            failure = { Log.e("DiaryNotification", "Push token failed: $it") },
        )
    }

    // endregion Diary posts
}