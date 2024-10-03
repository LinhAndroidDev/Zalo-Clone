package com.example.messageapp.di

import android.content.Context
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.SharePreferenceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharePreferenceRepository {
        return SharePreferenceRepositoryImpl(context)
    }
}