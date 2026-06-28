package com.example.messageapp.data.di

import com.example.messageapp.data.legacy.PresenceManager
import com.example.messageapp.data.repository.AuthRepositoryImpl
import com.example.messageapp.data.repository.ChatRepositoryImpl
import com.example.messageapp.data.repository.ConversationRepositoryImpl
import com.example.messageapp.data.repository.DiaryNotificationRepositoryImpl
import com.example.messageapp.data.repository.DiaryRepositoryImpl
import com.example.messageapp.data.repository.FriendRepositoryImpl
import com.example.messageapp.data.repository.GroupChatRepositoryImpl
import com.example.messageapp.data.repository.MediaUploadRepositoryImpl
import com.example.messageapp.data.repository.NotificationRepositoryImpl
import com.example.messageapp.data.repository.StickerRepositoryImpl
import com.example.messageapp.data.repository.PresenceRepositoryImpl
import com.example.messageapp.data.repository.UserRepositoryImpl
import com.example.messageapp.data.session.SessionRepositoryImpl
import com.example.messageapp.domain.repository.AuthRepository
import com.example.messageapp.domain.repository.ChatRepository
import com.example.messageapp.domain.repository.ConversationRepository
import com.example.messageapp.domain.repository.DiaryNotificationRepository
import com.example.messageapp.domain.repository.DiaryRepository
import com.example.messageapp.domain.repository.FriendRepository
import com.example.messageapp.domain.repository.GroupChatRepository
import com.example.messageapp.domain.repository.MediaUploadRepository
import com.example.messageapp.domain.repository.NotificationRepository
import com.example.messageapp.domain.repository.PresenceRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.StickerRepository
import com.example.messageapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
    @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindGroupChatRepository(impl: GroupChatRepositoryImpl): GroupChatRepository
    @Binds @Singleton abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository
    @Binds @Singleton abstract fun bindPresenceRepository(impl: PresenceRepositoryImpl): PresenceRepository
    @Binds @Singleton abstract fun bindMediaUploadRepository(impl: MediaUploadRepositoryImpl): MediaUploadRepository
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository
    @Binds @Singleton abstract fun bindDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository
    @Binds @Singleton abstract fun bindDiaryNotificationRepository(impl: DiaryNotificationRepositoryImpl): DiaryNotificationRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindStickerRepository(impl: StickerRepositoryImpl): StickerRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun providePresenceManager(): PresenceManager = PresenceManager()
}
