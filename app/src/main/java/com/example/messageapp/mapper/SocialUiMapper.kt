package com.example.messageapp.mapper

import com.example.messageapp.data.firestore.Friend as FsFriend
import com.example.messageapp.data.firestore.FriendRequest as FsFriendRequest
import com.example.messageapp.data.firestore.Sticker as FsSticker
import com.example.messageapp.data.firestore.User as FsUser
import com.example.messageapp.model.Friend
import com.example.messageapp.model.FriendRequest
import com.example.messageapp.model.Sticker
import com.example.messageapp.model.User

object SocialUiMapper {

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

    fun toUiUsers(users: List<FsUser>): List<User> = users.map { toUi(it) }

    fun toUi(friend: FsFriend): Friend = Friend(
        name = friend.name,
        avatar = friend.avatar,
        keyAuth = friend.keyAuth,
        since = friend.since,
    )

    fun toUiFriends(friends: List<FsFriend>): List<Friend> = friends.map { toUi(it) }

    fun toUi(request: FsFriendRequest): FriendRequest = FriendRequest(
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

    fun toUiRequests(requests: List<FsFriendRequest>): List<FriendRequest> = requests.map { toUi(it) }

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
