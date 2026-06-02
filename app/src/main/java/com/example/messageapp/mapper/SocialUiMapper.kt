package com.example.messageapp.mapper

import com.example.messageapp.data.firestore.FriendRequest as FsFriendRequest
import com.example.messageapp.data.firestore.Sticker as FsSticker
import com.example.messageapp.data.firestore.User as FsUser
import com.example.messageapp.domain.model.Friend as DomainFriend
import com.example.messageapp.domain.model.FriendRequest as DomainFriendRequest
import com.example.messageapp.domain.model.User as DomainUser
import com.example.messageapp.model.Friend
import com.example.messageapp.model.FriendRequest
import com.example.messageapp.model.Sticker
import com.example.messageapp.model.User

object SocialUiMapper {

    fun toUi(user: DomainUser): User = ChatUiMapper.toUi(user)

    fun toDomain(user: User): DomainUser = ChatUiMapper.toDomain(user)

    fun toUi(user: FsUser): User = User(
        name = user.name,
        email = user.email,
        avatar = user.avatar,
        imageCover = user.imageCover,
        keyAuth = user.keyAuth,
    )

    fun toFirestore(user: User): FsUser = FsUser(
        name = user.name,
        email = user.email,
        avatar = user.avatar,
        imageCover = user.imageCover,
        keyAuth = user.keyAuth,
    )

    fun toUiUsers(users: List<DomainUser>): List<User> = users.map { toUi(it) }

    fun toUi(friend: DomainFriend): Friend = Friend(
        name = friend.name,
        avatar = friend.avatar,
        keyAuth = friend.userId,
    )

    fun toUiFriends(friends: List<DomainFriend>): List<Friend> = friends.map { toUi(it) }

    fun toUi(request: DomainFriendRequest): FriendRequest = FriendRequest(
        requestId = request.id,
        fromId = request.fromId,
        toId = request.toId,
        fromName = request.fromName,
        fromAvatar = request.fromAvatar,
        toName = request.toName,
        toAvatar = request.toAvatar,
        status = request.status,
        createdAt = request.createdAt,
    )

    fun toDomain(request: FriendRequest): DomainFriendRequest = DomainFriendRequest(
        id = request.requestId,
        fromId = request.fromId,
        toId = request.toId,
        fromName = request.fromName,
        fromAvatar = request.fromAvatar,
        toName = request.toName,
        toAvatar = request.toAvatar,
        status = request.status,
        createdAt = request.createdAt,
    )

    fun toUiRequests(requests: List<DomainFriendRequest>): List<FriendRequest> = requests.map { toUi(it) }

    fun toFirestore(request: FriendRequest): FsFriendRequest = FsFriendRequest(
        requestId = request.requestId,
        fromId = request.fromId,
        toId = request.toId,
        fromName = request.fromName,
        fromAvatar = request.fromAvatar,
        toName = request.toName,
        toAvatar = request.toAvatar,
        status = request.status,
        createdAt = request.createdAt,
    )

    fun toFirestore(sticker: Sticker): FsSticker = FsSticker.valueOf(sticker.name)
}
