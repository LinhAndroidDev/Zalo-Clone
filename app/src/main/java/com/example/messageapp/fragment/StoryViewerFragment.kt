package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentStoryViewerBinding
import com.example.messageapp.domain.model.StoryMediaType
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.model.StoryItem
import com.example.messageapp.model.StoryMediaTransform
import com.example.messageapp.model.StoryRingItem
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.viewmodel.StoryViewerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StoryViewerFragment : BaseFragment<FragmentStoryViewerBinding, StoryViewerViewModel>() {

    override val layoutResId: Int = R.layout.fragment_story_viewer

    private val handler = Handler(Looper.getMainLooper())
    private var videoPlayer: ExoPlayer? = null
    private var musicPlayer: ExoPlayer? = null
    private var ringIndex = 0
    private var storyIndex = 0
    private var progressBars = mutableListOf<ProgressBar>()
    private var segmentStartMs = 0L
    private var segmentDurationMs = IMAGE_DURATION_MS
    private var progressRunnable: Runnable? = null
    private var touchDownY = 0f
    private var touchDownX = 0f

    override fun initView() {
        super.initView()
        binding?.btnClose?.setOnClickListener { closeViewer() }
        setupTapOverlay()

        lifecycleScope.launch {
            viewModel?.loading?.collectLatest { loading ->
                if (loading == false && viewModel?.rings?.value.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.story_viewer_empty,
                        Toast.LENGTH_SHORT,
                    ).show()
                    closeViewer()
                }
            }
        }

        lifecycleScope.launch {
            viewModel?.rings?.collectLatest { rings ->
                if (rings.isEmpty()) return@collectLatest
                ringIndex = viewModel?.startRingIndex() ?: 0
                storyIndex = 0
                showCurrentStory(rings)
            }
        }

        lifecycleScope.launch {
            viewModel?.finished?.collectLatest { finished ->
                if (finished) closeViewer()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTapOverlay() {
        binding?.tapOverlay?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownY = event.y
                    touchDownX = event.x
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.y - touchDownY
                    val deltaX = kotlin.math.abs(event.x - touchDownX)
                    if (deltaY > SWIPE_DOWN_THRESHOLD_PX && deltaY > deltaX * 1.5f) {
                        closeViewer()
                        return@setOnTouchListener true
                    }
                    val width = binding?.tapOverlay?.width?.toFloat() ?: return@setOnTouchListener true
                    if (event.y > (binding?.tapOverlay?.height ?: 0) * 0.85f) {
                        return@setOnTouchListener true
                    }
                    when {
                        event.x < width * 0.35f -> goPrevious()
                        event.x > width * 0.65f -> goNext()
                    }
                }
            }
            true
        }
    }

    private fun showCurrentStory(rings: List<StoryRingItem>) {
        stopPlayback()
        if (ringIndex >= rings.size) {
            closeViewer()
            return
        }
        val ring = rings[ringIndex]
        if (storyIndex >= ring.stories.size) {
            goNextRing(rings)
            return
        }
        val story = ring.stories[storyIndex]
        viewModel?.markViewed(story)
        bindHeader(ring, story)
        bindMusicSticker(story)
        bindProgressBars(ring.stories.size)
        bindMedia(story)
        startSegmentTimer(story)
    }

    private fun bindHeader(ring: StoryRingItem, story: StoryItem) {
        binding?.tvAuthorName?.text = ring.authorName
        binding?.tvStoryTime?.text = formatStoryAge(story.createdAtMillis)
        context?.loadImg(ring.authorAvatarUrl, binding?.imgAuthor!!, R.drawable.bg_grey_equal)
    }

    private fun bindMusicSticker(story: StoryItem) {
        val sticker = binding?.musicSticker ?: return
        if (story.musicAudioUrl.isBlank() && story.musicImageUrl.isBlank()) {
            sticker.clearSticker()
            binding?.tvMusicLabel?.isVisible = false
            return
        }
        sticker.isDraggable = false
        sticker.previewAudioEnabled = false
        sticker.bindTrack(
            MusicTrackItem(
                id = story.musicTrackId,
                name = story.musicName,
                artistName = story.musicArtist,
                audioUrl = story.musicAudioUrl,
                imageUrl = story.musicImageUrl,
                durationSeconds = 0,
            ),
        )
        sticker.applyNormalizedPosition(story.musicStickerX, story.musicStickerY)
        binding?.tvMusicLabel?.isVisible = false
    }

    private fun bindProgressBars(count: Int) {
        val container = binding?.progressContainer ?: return
        container.removeAllViews()
        progressBars.clear()
        repeat(count) { index ->
            val bar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, 4.dp(), 1f).apply {
                    marginEnd = if (index == count - 1) 0 else 4.dp()
                }
                max = 1000
                progress = when {
                    index < storyIndex -> 1000
                    index == storyIndex -> 0
                    else -> 0
                }
            }
            progressBars.add(bar)
            container.addView(bar)
        }
    }

    private fun bindMedia(story: StoryItem) {
        binding?.mediaTransformContainer?.isTransformEnabled = false
        binding?.mediaTransformContainer?.applyTransformState(
            StoryMediaTransform(
                scale = story.mediaScale,
                rotation = story.mediaRotation,
                translationXNorm = story.mediaTranslationX,
                translationYNorm = story.mediaTranslationY,
            ),
        )
        binding?.imgStory?.isVisible = story.mediaType == StoryMediaType.IMAGE
        binding?.videoStory?.isVisible = story.mediaType == StoryMediaType.VIDEO
        if (story.mediaType == StoryMediaType.IMAGE) {
            context?.loadImg(story.mediaUrl, binding?.imgStory!!, R.drawable.bg_grey_equal)
            segmentDurationMs = IMAGE_DURATION_MS
        } else {
            ensureVideoPlayer()
            videoPlayer?.setMediaItem(MediaItem.fromUri(story.mediaUrl))
            videoPlayer?.volume = VIDEO_VOLUME
            videoPlayer?.prepare()
            videoPlayer?.playWhenReady = true
            segmentDurationMs = MAX_VIDEO_DURATION_MS
        }
        if (story.musicAudioUrl.isNotBlank()) {
            ensureMusicPlayer()
            musicPlayer?.setMediaItem(MediaItem.fromUri(story.musicAudioUrl))
            musicPlayer?.volume = MUSIC_VOLUME
            musicPlayer?.prepare()
            musicPlayer?.playWhenReady = true
        }
    }

    private fun ensureVideoPlayer() {
        if (videoPlayer != null) return
        videoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            binding?.videoStory?.player = player
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        goNext()
                    }
                }
            })
        }
    }

    private fun ensureMusicPlayer() {
        if (musicPlayer != null) return
        musicPlayer = ExoPlayer.Builder(requireContext()).build()
    }

    private fun startSegmentTimer(story: StoryItem) {
        segmentStartMs = System.currentTimeMillis()
        if (story.mediaType == StoryMediaType.VIDEO) {
            progressRunnable = object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - segmentStartMs
                    val duration = minOf(videoPlayer?.duration?.takeIf { it > 0 } ?: MAX_VIDEO_DURATION_MS, MAX_VIDEO_DURATION_MS)
                    updateProgress(elapsed, duration)
                    if (elapsed >= duration) {
                        goNext()
                    } else {
                        handler.postDelayed(this, 32L)
                    }
                }
            }
        } else {
            progressRunnable = object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - segmentStartMs
                    updateProgress(elapsed, IMAGE_DURATION_MS)
                    if (elapsed >= IMAGE_DURATION_MS) {
                        goNext()
                    } else {
                        handler.postDelayed(this, 32L)
                    }
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun updateProgress(elapsed: Long, duration: Long) {
        val bar = progressBars.getOrNull(storyIndex) ?: return
        val progress = ((elapsed.toFloat() / duration) * 1000).toInt().coerceIn(0, 1000)
        bar.progress = progress
    }

    private fun goNext() {
        val rings = viewModel?.rings?.value.orEmpty()
        if (rings.isEmpty()) {
            closeViewer()
            return
        }
        val ring = rings.getOrNull(ringIndex) ?: run {
            closeViewer()
            return
        }
        if (storyIndex < ring.stories.lastIndex) {
            storyIndex++
            showCurrentStory(rings)
        } else {
            goNextRing(rings)
        }
    }

    private fun goPrevious() {
        val rings = viewModel?.rings?.value.orEmpty()
        if (rings.isEmpty()) return
        if (storyIndex > 0) {
            storyIndex--
            showCurrentStory(rings)
        } else if (ringIndex > 0) {
            ringIndex--
            storyIndex = (rings[ringIndex].stories.size - 1).coerceAtLeast(0)
            showCurrentStory(rings)
        }
    }

    private fun goNextRing(rings: List<StoryRingItem>) {
        if (ringIndex < rings.lastIndex) {
            ringIndex++
            storyIndex = 0
            showCurrentStory(rings)
        } else {
            closeViewer()
        }
    }

    private fun stopPlayback() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
        videoPlayer?.stop()
        videoPlayer?.clearMediaItems()
        musicPlayer?.stop()
        musicPlayer?.clearMediaItems()
        binding?.musicSticker?.clearSticker()
    }

    private fun closeViewer() {
        stopPlayback()
        val navController = findNavController()
        if (!navController.navigateUp()) {
            navController.popBackStack(R.id.diaryFragment, false)
        }
    }

    private fun formatStoryAge(createdAtMillis: Long): String {
        val diffHours = ((System.currentTimeMillis() - createdAtMillis) / (1000 * 60 * 60)).coerceAtLeast(0)
        return getString(R.string.story_hours_ago, diffHours)
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        stopPlayback()
        videoPlayer?.release()
        videoPlayer = null
        musicPlayer?.release()
        musicPlayer = null
        binding?.videoStory?.player = null
        binding?.musicSticker?.release()
        super.onDestroyView()
    }

    companion object {
        private const val IMAGE_DURATION_MS = 5_000L
        private const val MAX_VIDEO_DURATION_MS = 30_000L
        private const val VIDEO_VOLUME = 0.7f
        private const val MUSIC_VOLUME = 0.5f
        private const val SWIPE_DOWN_THRESHOLD_PX = 120f
    }
}
