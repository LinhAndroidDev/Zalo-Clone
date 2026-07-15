package com.example.messageapp.mapper

import com.example.messageapp.domain.model.Conversation as DomainConversation
import com.example.messageapp.domain.model.Emotion as DomainEmotion
import com.example.messageapp.domain.model.EmotionType as DomainEmotionType
import com.example.messageapp.domain.model.Message as DomainMessage
import com.example.messageapp.domain.model.MessageMention as DomainMessageMention
import com.example.messageapp.domain.model.MessageReply as DomainMessageReply
import com.example.messageapp.domain.model.User as DomainUser
import com.example.messageapp.domain.model.UserPresence as DomainUserPresence
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Emotion
import com.example.messageapp.model.EmotionType
import com.example.messageapp.model.Message
import com.example.messageapp.model.MessageMention
import com.example.messageapp.model.MessageReply
import com.example.messageapp.model.User
import com.example.messageapp.model.UserPresence

object ChatUiMapper {

    fun toUi(message: DomainMessage): Message = Message(
        message = message.message,
        receiver = message.receiver,
        sender = message.sender,
        time = message.time,
        emotion = message.emotion?.let { toUi(it) },
        mentions = message.mentions.map { toUi(it) },
        photos = ArrayList(message.photos),
        photoSizes = message.photoSizes?.let { ArrayList(it) },
        singlePhoto = ArrayList(message.singlePhoto),
        audio = message.audio,
        type = message.type,
        replyTo = message.replyTo?.let { toUi(it) },
        forwardFromId = message.forwardFromId,
        forwardFromName = message.forwardFromName,
        systemEvent = message.systemEvent,
        systemActorId = message.systemActorId,
        systemActorName = message.systemActorName,
        systemTargetIds = ArrayList(message.systemTargetIds),
        systemTargetNames = ArrayList(message.systemTargetNames),
    )

    fun toDomain(message: Message): DomainMessage = DomainMessage(
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
        forwardFromId = message.forwardFromId,
        forwardFromName = message.forwardFromName,
        systemEvent = message.systemEvent,
        systemActorId = message.systemActorId,
        systemActorName = message.systemActorName,
        systemTargetIds = message.systemTargetIds.toList(),
        systemTargetNames = message.systemTargetNames.toList(),
    )

    fun toUiList(messages: List<DomainMessage>): ArrayList<Message> =
        ArrayList(messages.map { toUi(it) })

    fun toUi(mention: DomainMessageMention): MessageMention = MessageMention(
        userId = mention.userId,
        token = mention.token,
        displayName = mention.displayName,
    )

    fun toDomain(mention: MessageMention): DomainMessageMention = DomainMessageMention(
        userId = mention.userId,
        token = mention.token,
        displayName = mention.displayName,
    )

    fun toUi(reply: DomainMessageReply): MessageReply = MessageReply(
        messageTime = reply.messageTime,
        senderId = reply.senderId,
        senderName = reply.senderName,
        previewText = reply.previewText,
        type = reply.type,
        photoUrl = reply.photoUrl,
    )

    fun toDomain(reply: MessageReply): DomainMessageReply = DomainMessageReply(
        messageTime = reply.messageTime,
        senderId = reply.senderId,
        senderName = reply.senderName,
        previewText = reply.previewText,
        type = reply.type,
        photoUrl = reply.photoUrl,
    )

    fun toUi(emotion: DomainEmotion): Emotion = Emotion(
        favourite = emotion.favourite,
        like = emotion.like,
        laugh = emotion.laugh,
        cry = emotion.cry,
        angry = emotion.angry,
    )

    fun toDomain(emotion: Emotion): DomainEmotion = DomainEmotion(
        favourite = emotion.favourite,
        like = emotion.like,
        laugh = emotion.laugh,
        cry = emotion.cry,
        angry = emotion.angry,
    )

    fun toUi(conversation: DomainConversation): Conversation = Conversation(
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

    fun toDomain(conversation: Conversation): DomainConversation = DomainConversation(
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

    fun toUi(user: DomainUser): User = User(
        name = user.name,
        email = user.email,
        avatar = user.avatar,
        imageCover = user.imageCover,
        keyAuth = user.keyAuth,
    )

    fun toDomain(user: User): DomainUser = DomainUser(
        name = user.name.orEmpty(),
        email = user.email.orEmpty(),
        avatar = user.avatar.orEmpty(),
        imageCover = user.imageCover.orEmpty(),
        keyAuth = user.keyAuth.orEmpty(),
    )

    fun toUi(presence: DomainUserPresence): UserPresence = UserPresence(
        online = presence.online,
        lastSeen = presence.lastSeen,
    )

    fun toDomain(type: EmotionType): DomainEmotionType = DomainEmotionType.valueOf(type.name)

    fun toUi(type: DomainEmotionType): EmotionType = EmotionType.valueOf(type.name)

    fun toUi(friend: com.example.messageapp.domain.model.Friend): com.example.messageapp.model.Friend =
        com.example.messageapp.model.Friend(
            name = friend.name,
            avatar = friend.avatar,
            keyAuth = friend.userId,
        )
}
