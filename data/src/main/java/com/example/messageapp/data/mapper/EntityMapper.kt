package com.example.messageapp.data.mapper

import com.example.messageapp.data.firestore.Conversation as FsConversation
import com.example.messageapp.data.firestore.Emotion as FsEmotion
import com.example.messageapp.data.firestore.EmotionType as FsEmotionType
import com.example.messageapp.data.firestore.Friend as FsFriend
import com.example.messageapp.data.firestore.FriendRequest as FsFriendRequest
import com.example.messageapp.data.firestore.GroupChat as FsGroupChat
import com.example.messageapp.data.firestore.Message as FsMessage
import com.example.messageapp.data.firestore.MessageMention as FsMessageMention
import com.example.messageapp.data.firestore.MessageReply as FsMessageReply
import com.example.messageapp.data.firestore.User as FsUser
import com.example.messageapp.data.firestore.UserPresence as FsUserPresence
import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.Emotion
import com.example.messageapp.domain.model.EmotionType
import com.example.messageapp.domain.model.Friend
import com.example.messageapp.domain.model.FriendRequest
import com.example.messageapp.domain.model.GroupChat
import com.example.messageapp.domain.model.Message
import com.example.messageapp.domain.model.MessageMention
import com.example.messageapp.domain.model.MessageReply
import com.example.messageapp.domain.model.User
import com.example.messageapp.domain.model.UserPresence

object EntityMapper {

    fun toDomain(message: FsMessage): Message = Message(
        message = message.message,
        receiver = message.receiver,
        sender = message.sender,
        time = message.time,
        emotion = message.emotion?.let { toDomain(it) },
        mentions = message.mentions.map { toDomain(it) },
        photos = message.photos.toList(),
        photoSizes = message.photoSizes?.toList(),
        singlePhoto = message.singlePhoto.toList(),
        audio = message.audio,
        type = message.type,
        replyTo = message.replyTo?.let { toDomain(it) },
    )

    fun toFirestore(message: Message): FsMessage = FsMessage(
        message = message.message,
        receiver = message.receiver,
        sender = message.sender,
        time = message.time,
        emotion = message.emotion?.let { toFirestore(it) },
        mentions = message.mentions.map { toFirestore(it) },
        photos = ArrayList(message.photos),
        photoSizes = message.photoSizes?.let { ArrayList(it) },
        singlePhoto = ArrayList(message.singlePhoto),
        audio = message.audio,
        type = message.type,
        replyTo = message.replyTo?.let { toFirestore(it) },
    )

    fun toDomain(mention: FsMessageMention): MessageMention = MessageMention(
        userId = mention.userId,
        token = mention.token,
        displayName = mention.displayName,
    )

    fun toFirestore(mention: MessageMention): FsMessageMention = FsMessageMention(
        userId = mention.userId,
        token = mention.token,
        displayName = mention.displayName,
    )

    fun toDomain(reply: FsMessageReply): MessageReply = MessageReply(
        messageTime = reply.messageTime,
        senderId = reply.senderId,
        senderName = reply.senderName,
        previewText = reply.previewText,
        type = reply.type,
        photoUrl = reply.photoUrl,
    )

    fun toFirestore(reply: MessageReply): FsMessageReply = FsMessageReply(
        messageTime = reply.messageTime,
        senderId = reply.senderId,
        senderName = reply.senderName,
        previewText = reply.previewText,
        type = reply.type,
        photoUrl = reply.photoUrl,
    )

    fun toDomain(emotion: FsEmotion): Emotion = Emotion(
        favourite = emotion.favourite,
        like = emotion.like,
        laugh = emotion.laugh,
        cry = emotion.cry,
        angry = emotion.angry,
    )

    fun toFirestore(emotion: Emotion): FsEmotion = FsEmotion(
        favourite = emotion.favourite,
        like = emotion.like,
        laugh = emotion.laugh,
        cry = emotion.cry,
        angry = emotion.angry,
    )

    fun toDomain(conversation: FsConversation): Conversation = Conversation(
        friendId = conversation.friendId,
        friendImage = conversation.friendImage,
        message = conversation.message,
        name = conversation.name,
        person = conversation.person,
        sender = conversation.sender,
        time = conversation.time,
        seen = conversation.seen,
        numberUnSeen = conversation.numberUnSeen,
        typing = conversation.typing,
        isGroup = conversation.isGroup,
    )

    fun toFirestore(conversation: Conversation): FsConversation = FsConversation(
        friendId = conversation.friendId,
        friendImage = conversation.friendImage,
        message = conversation.message,
        name = conversation.name,
        person = conversation.person,
        sender = conversation.sender,
        time = conversation.time,
        seen = conversation.seen,
        numberUnSeen = conversation.numberUnSeen,
        typing = conversation.typing,
        isGroup = conversation.isGroup,
    )

    fun toDomain(group: FsGroupChat): GroupChat = GroupChat(
        name = group.name,
        photoUrl = group.photoUrl,
        memberIds = group.memberIds,
        createdBy = group.createdBy,
        createdAt = group.createdAt,
        typing = group.typing,
        typingUserId = group.typingUserId,
    )

    fun toDomain(presence: FsUserPresence): UserPresence = UserPresence(
        online = presence.online,
        lastSeen = presence.lastSeen,
    )

    fun toDomain(user: FsUser): User = User(
        name = user.name.orEmpty(),
        email = user.email.orEmpty(),
        avatar = user.avatar.orEmpty(),
        imageCover = user.imageCover.orEmpty(),
        keyAuth = user.keyAuth.orEmpty(),
    )

    fun toDomain(friend: FsFriend): Friend = Friend(
        userId = friend.keyAuth,
        name = friend.name,
        avatar = friend.avatar,
    )

    fun toDomain(request: FsFriendRequest): FriendRequest = FriendRequest(
        id = request.requestId,
        fromId = request.fromId,
        toId = request.toId,
        status = request.status,
        createdAt = request.createdAt,
    )

    fun toFirestore(type: EmotionType): FsEmotionType = FsEmotionType.valueOf(type.name)
}
