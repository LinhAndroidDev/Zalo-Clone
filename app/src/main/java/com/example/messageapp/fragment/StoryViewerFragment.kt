package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.messageapp.R
import com.example.messageapp.adapter.StoryViewerPagerAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetStoryCustomFriends
import com.example.messageapp.bottom_sheet.BottomSheetStoryPrivacy
import com.example.messageapp.databinding.FragmentStoryViewerBinding
import com.example.messageapp.databinding.ItemStoryPageBinding
import com.example.messageapp.domain.model.StoryMediaType
import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.model.StoryItem
import com.example.messageapp.model.StoryMediaTransform
import com.example.messageapp.model.StoryViewerPage
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.RelativeTimeFormatter
import com.example.messageapp.viewmodel.StoryViewerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StoryViewerFragment : BaseFragment<FragmentStoryViewerBinding, StoryViewerViewModel>() {

    override val layoutResId: Int = R.layout.fragment_story_viewer

    private val handler = Handler(Looper.getMainLooper())
    private val pagerAdapter = StoryViewerPagerAdapter()
    private var videoPlayer: ExoPlayer? = null
    private var musicPlayer: ExoPlayer? = null
    private var segmentStartMs = 0L
    private var segmentElapsedOffsetMs = 0L
    private var isTimerPaused = false
    private var activeTimerStory: StoryItem? = null
    private var activeTimerPageIndex = -1
    private var progressRunnable: Runnable? = null
    private var touchDownY = 0f
    private var touchDownX = 0f
    private var currentPageIndex = 0
    private var pagerInitialized = false
    private var suppressPageChange = false

    override fun initView() {
        super.initView()
        binding?.btnClose?.setOnClickListener { closeViewer() }
        binding?.btnMore?.setOnClickListener { showOwnerMenu() }
        setupPager()
        setupTapZones()
        setupSwipeDownToClose()
        observeViewModel()
    }

    private fun setupPager() {
        binding?.storyPager?.isUserInputEnabled = false
        binding?.storyPager?.offscreenPageLimit = 1
        binding?.storyPager?.adapter = pagerAdapter
        binding?.storyPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (suppressPageChange) return
                currentPageIndex = position
                onStoryPageSelected(position)
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel?.loading?.collectLatest { loading ->
                if (loading == false && viewModel?.pages?.value.isNullOrEmpty()) {
                    if (!isAdded) return@collectLatest
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
            viewModel?.pages?.collectLatest { pages ->
                pagerAdapter.submitPages(pages)
                if (pages.isEmpty()) return@collectLatest
                if (!pagerInitialized) {
                    val start = (viewModel?.resolveInitialPageIndex(pages) ?: 0)
                        .coerceIn(0, pages.lastIndex)
                    suppressPageChange = true
                    currentPageIndex = start
                    binding?.storyPager?.setCurrentItem(start, false)
                    binding?.storyPager?.post {
                        suppressPageChange = false
                        pagerInitialized = true
                        onStoryPageSelected(start)
                    }
                } else if (currentPageIndex > pages.lastIndex) {
                    currentPageIndex = pages.lastIndex
                    binding?.storyPager?.setCurrentItem(currentPageIndex, false)
                }
            }
        }

        lifecycleScope.launch {
            viewModel?.finished?.collectLatest { finished ->
                if (finished) closeViewer()
            }
        }
    }

    private fun setupTapZones() {
        binding?.tapZoneLeft?.setOnClickListener { handleTapLeft() }
        binding?.tapZoneRight?.setOnClickListener { goNextPage() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeDownToClose() {
        val swipeListener = View.OnTouchListener { _, event ->
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
                        return@OnTouchListener true
                    }
                }
            }
            false
        }
        binding?.storyPager?.setOnTouchListener(swipeListener)
        binding?.tapOverlay?.setOnTouchListener(swipeListener)
        binding?.headerScrim?.setOnTouchListener(swipeListener)
    }

    private fun handleTapLeft() {
        if (!isAdded) return
        val page = viewModel?.pageAt(currentPageIndex) ?: return
        if (viewModel?.isFirstStoryInRing(page) == true) {
            restartCurrentStory()
        } else {
            goPreviousPage()
        }
    }

    private fun onStoryPageSelected(index: Int) {
        if (!isAdded) return
        val page = viewModel?.pageAt(index) ?: return
        stopPlayback()
        viewModel?.markViewed(page.story)
        bindHeader(page)
        updateOwnerMenu(page)
        binding?.storyPager?.post {
            if (!isAdded || currentPageIndex != index) return@post
            bindPageMedia(page.story)
            bindPageMusic(page.story)
            resetPageProgress(index)
            startSegmentTimer(page.story, index)
        }
    }

    private fun bindHeader(page: StoryViewerPage) {
        binding?.tvAuthorName?.text = page.ring.authorName
        binding?.tvStoryTime?.text = context?.let {
            RelativeTimeFormatter.format(it, page.story.createdAtMillis)
        }.orEmpty()
        context?.loadImg(page.ring.authorAvatarUrl, binding?.imgAuthor!!, R.drawable.bg_grey_equal)
    }

    private fun bindPageMusic(story: StoryItem) {
        val pageBinding = pageBindingAt(currentPageIndex) ?: return
        val sticker = pageBinding.musicSticker
        if (story.musicAudioUrl.isBlank() && story.musicImageUrl.isBlank()) {
            sticker.clearSticker()
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
    }

    private fun clearPageMusic(index: Int) {
        pageBindingAt(index)?.musicSticker?.clearSticker()
    }

    private fun resetPageProgress(index: Int) {
        if (index != currentPageIndex) return
        binding?.progressStory?.progress = 0
    }

    private fun bindPageMedia(story: StoryItem) {
        val pageBinding = pageBindingAt(currentPageIndex) ?: return
        pageBinding.mediaTransformContainer.isTransformEnabled = false
        pageBinding.mediaTransformContainer.applyTransformState(
            StoryMediaTransform(
                scale = story.mediaScale,
                rotation = story.mediaRotation,
                translationXNorm = story.mediaTranslationX,
                translationYNorm = story.mediaTranslationY,
            ),
        )
        pageBinding.imgStory.isVisible = story.mediaType == StoryMediaType.IMAGE
        pageBinding.videoStory.isVisible = story.mediaType == StoryMediaType.VIDEO
        if (story.mediaType == StoryMediaType.IMAGE) {
            videoPlayer?.pause()
            pageBinding.videoStory.player = null
            context?.loadImg(story.mediaUrl, pageBinding.imgStory, R.drawable.bg_grey_equal)
        } else {
            ensureVideoPlayer()
            videoPlayer?.setMediaItem(MediaItem.fromUri(story.mediaUrl))
            videoPlayer?.volume = VIDEO_VOLUME
            videoPlayer?.prepare()
            videoPlayer?.playWhenReady = true
            pageBinding.videoStory.player = videoPlayer
        }
        if (story.musicAudioUrl.isNotBlank()) {
            ensureMusicPlayer()
            musicPlayer?.setMediaItem(MediaItem.fromUri(story.musicAudioUrl))
            musicPlayer?.volume = MUSIC_VOLUME
            musicPlayer?.prepare()
            musicPlayer?.playWhenReady = true
        }
    }

    private fun pageBindingAt(index: Int): ItemStoryPageBinding? {
        val pager = binding?.storyPager ?: return null
        val recyclerView = pager.getChildAt(0) as? RecyclerView ?: return null
        val holder = recyclerView.findViewHolderForAdapterPosition(index) as? StoryViewerPagerAdapter.PageViewHolder
        return holder?.binding
    }

    private fun ensureVideoPlayer() {
        if (videoPlayer != null) return
        videoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        goNextPage()
                    }
                }
            })
        }
    }

    private fun ensureMusicPlayer() {
        if (musicPlayer != null) return
        musicPlayer = ExoPlayer.Builder(requireContext()).build()
    }

    private fun startSegmentTimer(story: StoryItem, pageIndex: Int) {
        segmentElapsedOffsetMs = 0L
        isTimerPaused = false
        activeTimerStory = story
        activeTimerPageIndex = pageIndex
        segmentStartMs = System.currentTimeMillis()
        postSegmentTimer(story, pageIndex)
    }

    private fun postSegmentTimer(story: StoryItem, pageIndex: Int) {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = if (story.mediaType == StoryMediaType.VIDEO) {
            object : Runnable {
                override fun run() {
                    if (!isAdded || view == null || currentPageIndex != pageIndex || isTimerPaused) return
                    val elapsed = segmentElapsedMs()
                    val duration = minOf(
                        videoPlayer?.duration?.takeIf { it > 0 } ?: MAX_VIDEO_DURATION_MS,
                        MAX_VIDEO_DURATION_MS,
                    )
                    updatePageProgress(pageIndex, elapsed, duration)
                    if (elapsed >= duration) {
                        goNextPage()
                    } else {
                        handler.postDelayed(this, 32L)
                    }
                }
            }
        } else {
            object : Runnable {
                override fun run() {
                    if (!isAdded || view == null || currentPageIndex != pageIndex || isTimerPaused) return
                    val elapsed = segmentElapsedMs()
                    updatePageProgress(pageIndex, elapsed, IMAGE_DURATION_MS)
                    if (elapsed >= IMAGE_DURATION_MS) {
                        goNextPage()
                    } else {
                        handler.postDelayed(this, 32L)
                    }
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun segmentElapsedMs(): Long =
        segmentElapsedOffsetMs + (System.currentTimeMillis() - segmentStartMs)

    private fun pauseStoryTimer() {
        if (isTimerPaused) return
        progressRunnable?.let { handler.removeCallbacks(it) }
        segmentElapsedOffsetMs = segmentElapsedMs()
        isTimerPaused = true
        videoPlayer?.pause()
        musicPlayer?.pause()
    }

    private fun resumeStoryTimer() {
        if (!isTimerPaused) return
        isTimerPaused = false
        val story = activeTimerStory ?: viewModel?.pageAt(currentPageIndex)?.story ?: return
        val pageIndex = activeTimerPageIndex.takeIf { it >= 0 } ?: currentPageIndex
        segmentStartMs = System.currentTimeMillis()
        if (story.mediaType == StoryMediaType.VIDEO) {
            videoPlayer?.playWhenReady = true
        }
        musicPlayer?.playWhenReady = true
        postSegmentTimer(story, pageIndex)
    }

    private fun updatePageProgress(pageIndex: Int, elapsed: Long, duration: Long) {
        if (pageIndex != currentPageIndex) return
        binding?.progressStory?.progress =
            ((elapsed.toFloat() / duration) * 1000).toInt().coerceIn(0, 1000)
    }

    private fun restartCurrentStory() {
        if (!isAdded) return
        val page = viewModel?.pageAt(currentPageIndex) ?: return
        stopPlayback()
        binding?.storyPager?.post {
            if (!isAdded) return@post
            restartPlayback(page)
        }
    }

    private fun restartPlayback(page: StoryViewerPage) {
        bindPageMedia(page.story)
        bindPageMusic(page.story)
        resetPageProgress(currentPageIndex)
        if (page.story.mediaType == StoryMediaType.VIDEO) {
            videoPlayer?.seekTo(0)
            videoPlayer?.playWhenReady = true
        }
        if (page.story.musicAudioUrl.isNotBlank()) {
            musicPlayer?.seekTo(0)
            musicPlayer?.playWhenReady = true
        }
        startSegmentTimer(page.story, currentPageIndex)
    }

    private fun goNextPage() {
        if (!isAdded || view == null) return
        val lastIndex = viewModel?.pages?.value?.lastIndex ?: -1
        if (currentPageIndex >= lastIndex) {
            closeViewer()
            return
        }
        binding?.storyPager?.setCurrentItem(currentPageIndex + 1, true)
    }

    private fun goPreviousPage() {
        if (!isAdded || view == null || currentPageIndex <= 0) return
        binding?.storyPager?.setCurrentItem(currentPageIndex - 1, true)
    }

    private fun stopPlayback() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
        segmentElapsedOffsetMs = 0L
        isTimerPaused = false
        activeTimerStory = null
        activeTimerPageIndex = -1
        pageBindingAt(currentPageIndex)?.videoStory?.player = null
        clearPageMusic(currentPageIndex)
        videoPlayer?.stop()
        videoPlayer?.clearMediaItems()
        musicPlayer?.stop()
        musicPlayer?.clearMediaItems()
    }

    private fun updateOwnerMenu(page: StoryViewerPage) {
        val isOwner = page.story.authorId == viewModel?.currentUserId()
        binding?.btnMore?.isVisible = isOwner
    }

    private fun showOwnerMenu() {
        val page = viewModel?.pageAt(currentPageIndex) ?: return
        if (page.story.authorId != viewModel?.currentUserId()) return
        val popup = PopupMenu(requireContext(), binding?.btnMore!!)
        popup.menu.add(0, MENU_PRIVACY, 0, R.string.story_menu_privacy)
        popup.menu.add(0, MENU_DELETE, 1, R.string.story_menu_delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_PRIVACY -> {
                    showPrivacySheet(page.story)
                    true
                }
                MENU_DELETE -> {
                    confirmDeleteStory(page.story)
                    true
                }
                else -> false
            }
        }
        popup.setOnDismissListener { resumeStoryTimer() }
        pauseStoryTimer()
        popup.show()
    }

    private fun showPrivacySheet(story: StoryItem) {
        BottomSheetStoryPrivacy.newInstance().apply {
            onPrivacySelected = { privacy ->
                if (privacy == StoryPrivacy.CUSTOM) {
                    showCustomFriendsSheet(story, story.visibleToUserIds)
                } else {
                    updatePrivacy(story.id, privacy, emptyList())
                }
            }
        }.show(childFragmentManager, "BottomSheetStoryPrivacy")
    }

    private fun showCustomFriendsSheet(story: StoryItem, selectedIds: List<String>) {
        BottomSheetStoryCustomFriends.newInstance(selectedIds).apply {
            onFriendsSelected = { ids -> updatePrivacy(story.id, StoryPrivacy.CUSTOM, ids) }
        }.show(childFragmentManager, "BottomSheetStoryCustomFriends")
    }

    private fun updatePrivacy(storyId: String, privacy: StoryPrivacy, visibleToUserIds: List<String>) {
        viewModel?.updateStoryPrivacy(
            storyId = storyId,
            privacy = privacy,
            visibleToUserIds = visibleToUserIds,
            onSuccess = {
                Toast.makeText(requireContext(), R.string.story_privacy_updated, Toast.LENGTH_SHORT).show()
            },
            onFailure = { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            },
        )
    }

    private fun confirmDeleteStory(story: StoryItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.story_delete_title)
            .setMessage(R.string.story_delete_message)
            .setNegativeButton(R.string.diary_post_delete_cancel, null)
            .setPositiveButton(R.string.diary_post_delete_confirm) { _, _ ->
                viewModel?.deleteStory(
                    storyId = story.id,
                    currentPageIndex = currentPageIndex,
                    onSuccess = { newIndex ->
                        Toast.makeText(requireContext(), R.string.story_delete_success, Toast.LENGTH_SHORT).show()
                        if (newIndex == null) {
                            closeViewer()
                        } else {
                            currentPageIndex = newIndex
                            binding?.storyPager?.setCurrentItem(newIndex, false)
                        }
                    },
                    onFailure = { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    },
                )
            }
            .show()
    }

    private fun closeViewer() {
        if (!isAdded) return
        stopPlayback()
        handler.removeCallbacksAndMessages(null)
        try {
            val navController = findNavController()
            if (!navController.navigateUp()) {
                navController.popBackStack(R.id.diaryFragment, false)
            }
        } catch (_: IllegalStateException) {
            // Fragment already detached.
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        stopPlayback()
        videoPlayer?.release()
        videoPlayer = null
        musicPlayer?.release()
        musicPlayer = null
        pagerInitialized = false
        suppressPageChange = false
        super.onDestroyView()
    }

    companion object {
        private const val IMAGE_DURATION_MS = 5_000L
        private const val MAX_VIDEO_DURATION_MS = 30_000L
        private const val VIDEO_VOLUME = 0.7f
        private const val MUSIC_VOLUME = 0.5f
        private const val SWIPE_DOWN_THRESHOLD_PX = 120f
        private const val MENU_PRIVACY = 1
        private const val MENU_DELETE = 2
    }
}
