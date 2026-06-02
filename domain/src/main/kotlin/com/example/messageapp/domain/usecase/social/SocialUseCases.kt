package com.example.messageapp.domain.usecase.social

import com.example.messageapp.domain.model.Friend
import com.example.messageapp.domain.model.FriendRequest
import com.example.messageapp.domain.model.User
import com.example.messageapp.domain.repository.FriendRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.UserRepository
import javax.inject.Inject

class GetFriendsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: (List<Friend>) -> Unit, onFailure: (String) -> Unit = {}) {
        friendRepository.getFriends(sessionRepository.getAuth(), onSuccess, onFailure)
    }
}

class GetIncomingFriendRequestsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: (List<FriendRequest>) -> Unit, onFailure: (String) -> Unit = {}) {
        friendRepository.getIncomingFriendRequests(sessionRepository.getAuth(), onSuccess, onFailure)
    }
}

class GetOutgoingFriendRequestsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: (List<FriendRequest>) -> Unit, onFailure: (String) -> Unit = {}) {
        friendRepository.getOutgoingFriendRequests(sessionRepository.getAuth(), onSuccess, onFailure)
    }
}

class AcceptFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        userRepository.getUserById(
            userId = sessionRepository.getAuth(),
            onSuccess = { me ->
                friendRepository.acceptFriendRequest(
                    request = request,
                    myName = me.name,
                    myAvatar = me.avatar,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class RejectFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    operator fun invoke(requestId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        friendRepository.rejectFriendRequest(requestId, onSuccess, onFailure)
    }
}

class CancelFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(toId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        friendRepository.cancelFriendRequest(sessionRepository.getAuth(), toId, onSuccess, onFailure)
    }
}

class SearchUsersUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    operator fun invoke(query: String, onSuccess: (List<User>) -> Unit, onFailure: (String) -> Unit = {}) {
        friendRepository.searchUsers(query, onSuccess, onFailure)
    }
}

class GetSearchHistoryUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: (List<User>) -> Unit) {
        friendRepository.getSearchHistory(sessionRepository.getAuth(), onSuccess)
    }
}

class SaveSearchHistoryUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(user: User) {
        friendRepository.saveSearchHistory(sessionRepository.getAuth(), user)
    }
}

class GetFriendshipStatusUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(otherId: String, onSuccess: (String) -> Unit) {
        friendRepository.getFriendshipStatus(sessionRepository.getAuth(), otherId, onSuccess)
    }
}

class SendFriendRequestUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        target: User,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        userRepository.getInfoUser(
            userId = sessionRepository.getAuth(),
            onSuccess = { me ->
                friendRepository.sendFriendRequest(
                    fromId = sessionRepository.getAuth(),
                    fromName = me.name,
                    fromAvatar = me.avatar,
                    toId = target.keyAuth,
                    toName = target.name,
                    toAvatar = target.avatar,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: (User) -> Unit, onFailure: (String) -> Unit = {}) {
        userRepository.getInfoUser(sessionRepository.getAuth(), onSuccess, onFailure)
    }
}

class GetUserByIdUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit = {}) {
        userRepository.getUserById(userId, onSuccess, onFailure)
    }
}

class GetUserInfoUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: String, onSuccess: (User) -> Unit, onFailure: (String) -> Unit = {}) {
        userRepository.getInfoUser(userId, onSuccess, onFailure)
    }
}

class RegisterUserUseCase @Inject constructor(
    private val authRepository: com.example.messageapp.domain.repository.AuthRepository,
) {
    operator fun invoke(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        authRepository.isEmailRegistered(
            email = email,
            onResult = { exists ->
                if (exists) {
                    onFailure("Email already exists")
                } else {
                    authRepository.registerUser(
                        user = User(name = name, email = email),
                        password = password,
                        onSuccess = onSuccess,
                        onFailure = onFailure,
                    )
                }
            },
            onFailure = onFailure,
        )
    }
}

class UploadProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        uriString: String,
        isAvatar: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        userRepository.uploadProfileImage(
            uriString = uriString,
            userId = sessionRepository.getAuth(),
            isAvatar = isAvatar,
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class GetStickerUrlsUseCase @Inject constructor(
    private val stickerRepository: com.example.messageapp.domain.repository.StickerRepository,
) {
    operator fun invoke(typeName: String, onSuccess: (List<String>) -> Unit) {
        stickerRepository.getStickerUrls(typeName, onSuccess)
    }
}
