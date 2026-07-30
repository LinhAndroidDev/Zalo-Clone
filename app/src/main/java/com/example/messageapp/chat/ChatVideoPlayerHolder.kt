package com.example.messageapp.chat

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

object ChatVideoPlayerHolder {

    private const val CACHE_DIR_NAME = "chat_video_cache"
    private const val CACHE_MAX_BYTES = 100L * 1024L * 1024L

    private var player: ExoPlayer? = null
    private var cache: SimpleCache? = null
    private var appContext: Context? = null
    private var listResumeVideoUrl: String? = null
    private var listShouldResumePlayback: Boolean = false

    @Synchronized
    fun obtainPlayer(context: Context): ExoPlayer {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        player?.let { return it }

        val simpleCache = getOrCreateCache(applicationContext)
        val upstreamFactory = DefaultHttpDataSource.Factory()
        val cacheFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,
                90_000,
                1_000,
                2_000,
            )
            .build()

        val exoPlayer = ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setLoadControl(loadControl)
            .build()

        player = exoPlayer
        return exoPlayer
    }

    val currentPositionMs: Long
        get() = player?.currentPosition?.coerceAtLeast(0L) ?: 0L

    val currentVideoUrl: String?
        get() = player?.currentMediaItem?.localConfiguration?.uri?.toString()

    fun isSameMediaLoaded(url: String): Boolean {
        val loadedUrl = currentVideoUrl ?: return false
        val state = player?.playbackState ?: Player.STATE_IDLE
        return loadedUrl == url && state != Player.STATE_IDLE
    }

    @Synchronized
    fun markReturnToList(videoUrl: String, shouldResume: Boolean) {
        listResumeVideoUrl = videoUrl
        listShouldResumePlayback = shouldResume
    }

    @Synchronized
    fun consumeListResumeState(): Pair<String?, Boolean> {
        val url = listResumeVideoUrl
        val shouldResume = listShouldResumePlayback
        listResumeVideoUrl = null
        listShouldResumePlayback = false
        return url to shouldResume
    }

    @Synchronized
    fun detachFromAllSurfaces() {
        player?.clearVideoSurface()
    }

    @Synchronized
    fun release() {
        listResumeVideoUrl = null
        listShouldResumePlayback = false
        player?.release()
        player = null
        cache?.release()
        cache = null
        appContext = null
    }

    private fun getOrCreateCache(context: Context): SimpleCache {
        cache?.let { return it }
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val evictor = LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider).also { cache = it }
    }
}
