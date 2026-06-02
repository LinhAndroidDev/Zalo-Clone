package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.Friend
import com.example.messageapp.domain.model.FriendRequest
import com.example.messageapp.domain.model.User

interface AuthRepository {
    fun checkLogin(
        email: String,
        password: String,
        onSuccess: (List<User>) -> Unit,
        onFailure: (String) -> Unit,
    )
    fun registerUser(user: User, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
}

interface UserRepository {
    fun getInfoUser(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit = {})
    fun getUserById(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit = {})
    fun updateAvatar(userId: String, avatarUrl: String)
    fun updateImageCover(userId: String, imageCoverUrl: String)
    fun saveFcmToken(userId: String, token: String)
}

interface FriendRepository {
    fun getFriends(userId: String, onSuccess: (List<Friend>) -> Unit, onFailure: (String) -> Unit = {})
    fun sendFriendRequest(
        fromId: String,
        fromName: String,
        fromAvatar: String,
        toId: String,
        toName: String,
        toAvatar: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )
    fun getIncomingFriendRequests(userId: String, onSuccess: (List<FriendRequest>) -> Unit, onFailure: (String) -> Unit = {})
    fun getOutgoingFriendRequests(userId: String, onSuccess: (List<FriendRequest>) -> Unit, onFailure: (String) -> Unit = {})
    fun acceptFriendRequest(request: FriendRequest, myName: String, myAvatar: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun rejectFriendRequest(requestId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun cancelFriendRequest(fromId: String, toId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun searchUsers(query: String, onSuccess: (List<User>) -> Unit, onFailure: (String) -> Unit = {})
    fun saveSearchHistory(userId: String, user: User)
    fun getSearchHistory(userId: String, onSuccess: (List<User>) -> Unit)
    fun getFriendshipStatus(userId: String, otherId: String, onSuccess: (String) -> Unit)
}
