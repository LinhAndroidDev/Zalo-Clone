package com.example.messageapp

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.helper.screenWidth
import com.example.messageapp.utils.PresenceManager
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var presenceManager: PresenceManager

    @Inject
    lateinit var shared: SharePreferenceRepository

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        setUpScreenSize()
        registerPresenceLifecycle()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun registerPresenceLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (shared.getStatusLoggedIn()) {
                    presenceManager.connect(shared.getAuth())
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                val userId = shared.getAuth()
                if (shared.getStatusLoggedIn() && userId.isNotBlank()) {
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
