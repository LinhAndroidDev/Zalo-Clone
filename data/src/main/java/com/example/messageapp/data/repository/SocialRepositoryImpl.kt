package com.example.messageapp.data.repository

import com.example.messageapp.data.firestore.FriendRequest as FsFriendRequest
import com.example.messageapp.data.firestore.User as FsUser
import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.model.Friend
import com.example.messageapp.domain.model.FriendRequest
import com.example.messageapp.domain.model.User
import com.example.messageapp.domain.repository.AuthRepository
import com.example.messageapp.domain.repository.FriendRepository
import com.example.messageapp.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override fun checkLogin(
        email: String,
        password: String,
        onSuccess: (List<User>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.checkLogin(
            email,
            password,
            success = { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FsUser::class.java)?.let {
                        EntityMapper.toDomain(it.copy(keyAuth = doc.id))
                    }
                }
                onSuccess(users)
            },
            failure = onFailure,
        )
    }

    override fun registerUser(user: User, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val map = hashMapOf(
            "email" to user.email,
            "password" to password,
            "name" to user.name,
            "avatar" to user.avatar,
        )
        FireBaseInstance.addUser(map, success = { onSuccess() }, failure = onFailure)
    }
}

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {
    override fun getInfoUser(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.getInfoUser(userId) { onSuccess(EntityMapper.toDomain(it)) }
    }

    override fun getUserById(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.getUserById(userId, success = { onSuccess(EntityMapper.toDomain(it)) }, failure = onFailure)
    }

    override fun updateAvatar(userId: String, avatarUrl: String) {
        FireBaseInstance.updateAvatarUser(avatarUrl, userId)
    }

    override fun updateImageCover(userId: String, imageCoverUrl: String) {
        FireBaseInstance.updateImageCover(userId, imageCoverUrl)
    }

    override fun saveFcmToken(userId: String, token: String) {
        FireBaseInstance.saveTokenMessage(userId, hashMapOf("token" to token))
    }
}

@Singleton
class FriendRepositoryImpl @Inject constructor() : FriendRepository {
    override fun getFriends(userId: String, onSuccess: (List<Friend>) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.getFriends(
            userId,
            success = { list -> onSuccess(list.map { EntityMapper.toDomain(it) }) },
            failure = onFailure,
        )
    }

    override fun sendFriendRequest(
        fromId: String,
        fromName: String,
        fromAvatar: String,
        toId: String,
        toName: String,
        toAvatar: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.sendFriendRequest(
            fromId, fromName, fromAvatar, toId, toName, toAvatar, onSuccess, onFailure,
        )
    }

    override fun getIncomingFriendRequests(
        userId: String,
        onSuccess: (List<FriendRequest>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.getIncomingFriendRequests(
            userId,
            success = { list -> onSuccess(list.map { EntityMapper.toDomain(it) }) },
            failure = onFailure,
        )
    }

    override fun getOutgoingFriendRequests(
        userId: String,
        onSuccess: (List<FriendRequest>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.getOutgoingFriendRequests(
            userId,
            success = { list -> onSuccess(list.map { EntityMapper.toDomain(it) }) },
            failure = onFailure,
        )
    }

    override fun acceptFriendRequest(
        request: FriendRequest,
        myName: String,
        myAvatar: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val fsRequest = FsFriendRequest(
            requestId = request.id,
            fromId = request.fromId,
            toId = request.toId,
            status = request.status,
            createdAt = request.createdAt,
        )
        FireBaseInstance.acceptFriendRequest(fsRequest, myName, myAvatar, onSuccess, onFailure)
    }

    override fun rejectFriendRequest(requestId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.rejectFriendRequest(requestId, onSuccess, onFailure)
    }

    override fun cancelFriendRequest(fromId: String, toId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.cancelFriendRequest(fromId, toId, onSuccess, onFailure)
    }

    override fun searchUsers(query: String, onSuccess: (List<User>) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.searchFriend(query) { list -> onSuccess(list.map { EntityMapper.toDomain(it) }) }
    }

    override fun saveSearchHistory(userId: String, user: User) {
        FireBaseInstance.saveSearchHistory(
            myId = userId,
            user = FsUser(user.name, user.email, user.avatar, user.imageCover, user.keyAuth),
        )
    }

    override fun getSearchHistory(userId: String, onSuccess: (List<User>) -> Unit) {
        FireBaseInstance.getSearchHistory(
            myId = userId,
            success = { list -> onSuccess(list.map { EntityMapper.toDomain(it) }) },
            failure = {},
        )
    }

    override fun getFriendshipStatus(userId: String, otherId: String, onSuccess: (String) -> Unit) {
        FireBaseInstance.getFriendshipStatus(userId, otherId, onSuccess)
    }
}
