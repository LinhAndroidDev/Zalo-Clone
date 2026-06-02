package com.example.messageapp

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.messageapp.data.DataContextHolder
import com.example.messageapp.data.legacy.PresenceManager
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.helper.screenWidth
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var presenceManager: PresenceManager

    @Inject
    lateinit var sessionRepository: SessionRepository

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        DataContextHolder.init(this)
        setUpScreenSize()
        registerPresenceLifecycle()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun registerPresenceLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (sessionRepository.getStatusLoggedIn()) {
                    presenceManager.connect(sessionRepository.getAuth())
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                val userId = sessionRepository.getAuth()
                if (sessionRepository.getStatusLoggedIn() && userId.isNotBlank()) {
                    presenceManager.disconnect(userId)
                }
            }
        })
    }

    private fun setUpScreenSize() {
        resources.displayMetrics.run {
            screenWidth = this.widthPixels
            screenHeight = this.heightPixels
        }
    }
}
