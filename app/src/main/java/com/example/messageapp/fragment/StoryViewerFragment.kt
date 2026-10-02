package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.example.messageapp.adapter.StoryRingPagerAdapter
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
import com.example.messageapp.model.StoryRingItem
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
    private val ringPagerAdapter = StoryRingPagerAdapter()
    private val progressSegmentBars = mutableListOf<ProgressBar>()
    private var videoPlayer: ExoPlayer? = null
    private var musicPlayer: ExoPlayer? = null
    private var segmentStartMs = 0L
    private var segmentElapsedOffsetMs = 0L
    private var isTimerPaused = false
    private var activeTimerStory: StoryItem? = null
    private var activeTimerRingIndex = -1
    private var activeTimerStoryIndex = -1
    private var progressRunnable: Runnable? = null
    private var touchDownY = 0f
    private var touchDownX = 0f
    private var currentRingIndex = 0
    private var currentStoryIndex = 0
    private var pagerInitialized = false
    private var suppressRingPageChange = false
    private var progressBoundRingIndex = -1
    private var lastRingsStructure: List<Pair<String, List<String>>> = emptyList()
    private var pendingStoryIndex = -1
    private var pendingInnerNav: Pair<Int, Int>? = null

    override fun initView() {
        super.initView()
        binding?.btnClose?.setOnClickListener { closeViewer() }
        binding?.btnMore?.setOnClickListener { showOwnerMenu() }
        setupPager()
        setupTapZones()
        setupGestureNavigation()
        observeViewModel()
    }

    private fun setupPager() {
        ringPagerAdapter.onInnerPageSelected = { ringIndex, storyIndex ->
            currentRingIndex = ringIndex
            currentStoryIndex = storyIndex
            onStorySelected(ringIndex, storyIndex)
        }
        ringPagerAdapter.onRingBound = { ringIndex, innerPager ->
            val pending = pendingInnerNav
            if (pending != null && pending.first == ringIndex) {
                applyInnerNavigation(innerPager, pending.first, pending.second, smooth = false)
            }
        }
        binding?.ringPager?.isUserInputEnabled = true
        binding?.ringPager?.offscreenPageLimit = 1
        binding?.ringPager?.adapter = ringPagerAdapter
        binding?.ringPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (suppressRingPageChange) return
                onOuterRingSelected(position)
            }
        })
    }

    private fun onOuterRingSelected(ringIndex: Int) {
        currentRingIndex = ringIndex
        bindProgressSegments(ringIndex)
        val lastStoryIndex = viewModel?.lastStoryIndexInRing(ringIndex) ?: 0
        val storyIndex = if (pendingStoryIndex >= 0) {
            val index = pendingStoryIndex.coerceIn(0, lastStoryIndex)
            pendingStoryIndex = -1
            index
        } else {
            currentStoryIndex.coerceIn(0, lastStoryIndex)
        }
        navigateInnerTo(ringIndex, storyIndex, smooth = false)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel?.loading?.collectLatest { loading ->
                if (loading == false && viewModel?.rings?.value.isNullOrEmpty()) {
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
            viewModel?.rings?.collectLatest { rings ->
                if (rings.isEmpty()) return@collectLatest
                val structure = ringsStructure(rings)
                val structureChanged = structure != lastRingsStructure
                if (!structureChanged) return@collectLatest

                lastRingsStructure = structure
                submitRingsSafely(rings) {
                    if (!isAdded) return@submitRingsSafely
                    if (!pagerInitialized) {
                        initializePagerPosition(rings)
                    } else if (currentRingIndex > rings.lastIndex) {
                        navigateToStory(
                            rings.lastIndex,
                            viewModel?.lastStoryIndexInRing(rings.lastIndex) ?: 0,
                            smooth = false,
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel?.finished?.collectLatest { finished ->
                if (finished) closeViewer()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTapZones() {
        setupSideTapZone(binding?.tapZoneLeft, isLeft = true)
        setupSideTapZone(binding?.tapZoneRight, isLeft = false)
        setupSwipeZone(binding?.tapOverlay?.getChildAt(1))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSideTapZone(zone: View?, isLeft: Boolean) {
        zone ?: return
        zone.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownX = event.rawX
                    touchDownY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> handleStoryTouchEnd(event.rawX, event.rawY, isLeft)
                else -> true
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeZone(zone: View?) {
        zone ?: return
        zone.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownX = event.rawX
                    touchDownY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> handleRingSwipeEnd(event.rawX, event.rawY)
                else -> true
            }
        }
    }

    private fun handleStoryTouchEnd(endX: Float, endY: Float, isLeft: Boolean): Boolean {
        val deltaX = endX - touchDownX
        val deltaY = endY - touchDownY
        val absX = kotlin.math.abs(deltaX)
        val absY = kotlin.math.abs(deltaY)
        return when {
            absX > SWIPE_HORIZONTAL_THRESHOLD_PX && absX > absY * 1.5f -> {
                if (deltaX > 0) goNextRing() else goPreviousRing()
                true
            }
            absX <= TAP_SLOP_PX && absY <= TAP_SLOP_PX -> {
                if (isLeft) handleTapLeft() else goNextStory()
                true
            }
            else -> true
        }
    }

    private fun handleRingSwipeEnd(endX: Float, endY: Float): Boolean {
        val deltaX = endX - touchDownX
        val deltaY = endY - touchDownY
        val absX = kotlin.math.abs(deltaX)
        val absY = kotlin.math.abs(deltaY)
        if (absX > SWIPE_HORIZONTAL_THRESHOLD_PX && absX > absY * 1.5f) {
            if (deltaX > 0) goNextRing() else goPreviousRing()
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureNavigation() {
        val swipeDownListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownY = event.rawY
                    touchDownX = event.rawX
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - touchDownY
                    val deltaX = kotlin.math.abs(event.rawX - touchDownX)
                    if (deltaY > SWIPE_DOWN_THRESHOLD_PX && deltaY > deltaX * 1.5f) {
                        closeViewer()
                        return@OnTouchListener true
                    }
                }
            }
            false
        }
        binding?.headerScrim?.setOnTouchListener(swipeDownListener)
    }

    private fun goNextRing() {
        if (!isAdded) return
        val vm = viewModel ?: return
        if (vm.hasNextRing(currentRingIndex)) {
            navigateToStory(currentRingIndex + 1, 0, smooth = true)
        } else {
            closeViewer()
        }
    }

    private fun goPreviousRing() {
        if (!isAdded) return
        val vm = viewModel ?: return
        if (vm.hasPreviousRing(currentRingIndex)) {
            val prevRingIndex = currentRingIndex - 1
            navigateToStory(prevRingIndex, vm.lastStoryIndexInRing(prevRingIndex), smooth = true)
        } else {
            restartCurrentStory()
        }
    }

    private fun handleTapLeft() {
        if (!isAdded) return
        val vm = viewModel ?: return
        if (!vm.isFirstStoryInRing(currentRingIndex, currentStoryIndex)) {
            goPreviousStory()
        } else if (vm.hasPreviousRing(currentRingIndex)) {
            val prevRingIndex = currentRingIndex - 1
            val prevStoryIndex = vm.lastStoryIndexInRing(prevRingIndex)
            navigateToStory(prevRingIndex, prevStoryIndex, smooth = true)
        } else {
            restartCurrentStory()
        }
    }

    private fun goNextStory() {
        if (!isAdded || view == null) return
        val vm = viewModel ?: return
        if (!vm.isLastStoryInRing(currentRingIndex, currentStoryIndex)) {
            navigateToStory(currentRingIndex, currentStoryIndex + 1, smooth = true)
        } else if (vm.hasNextRing(currentRingIndex)) {
            navigateToStory(currentRingIndex + 1, 0, smooth = true)
        } else {
            closeViewer()
        }
    }

    private fun goPreviousStory() {
        if (!isAdded || view == null || currentStoryIndex <= 0) return
        navigateToStory(currentRingIndex, currentStoryIndex - 1, smooth = true)
    }

    private fun initializePagerPosition(rings: List<StoryRingItem>) {
        val ringIndex = (viewModel?.resolveInitialRingIndex() ?: 0).coerceIn(0, rings.lastIndex)
        val ring = rings[ringIndex]
        val storyIndex = (viewModel?.resolveInitialStoryIndex(ring) ?: 0)
            .coerceIn(0, ring.stories.lastIndex)
        currentRingIndex = ringIndex
        currentStoryIndex = storyIndex
        pendingStoryIndex = storyIndex
        pagerInitialized = true
        suppressRingPageChange = false
        val outerPager = binding?.ringPager ?: return
        outerPager.post {
            if (!isAdded) return@post
            if (outerPager.currentItem == ringIndex) {
                onOuterRingSelected(ringIndex)
            } else {
                outerPager.setCurrentItem(ringIndex, false)
            }
        }
    }

    private fun navigateToStory(ringIndex: Int, storyIndex: Int, smooth: Boolean) {
        val vm = viewModel ?: return
        val outerPager = binding?.ringPager ?: return
        if (ringIndex !in vm.rings.value.indices) return
        val ring = vm.ringAt(ringIndex) ?: return
        val safeStoryIndex = storyIndex.coerceIn(0, ring.stories.lastIndex)
        currentRingIndex = ringIndex
        currentStoryIndex = safeStoryIndex
        pendingStoryIndex = safeStoryIndex

        if (outerPager.currentItem != ringIndex) {
            suppressRingPageChange = false
            outerPager.setCurrentItem(ringIndex, smooth)
        } else {
            navigateInnerTo(ringIndex, safeStoryIndex, smooth)
        }
    }

    private fun navigateInnerTo(ringIndex: Int, storyIndex: Int, smooth: Boolean, retryCount: Int = 0) {
        val outerPager = binding?.ringPager ?: return
        if (!isAdded) return
        pendingInnerNav = ringIndex to storyIndex

        fun tryNavigate() {
            val pending = pendingInnerNav
            if (pending == null || pending.first != ringIndex || pending.second != storyIndex) {
                return
            }
            val innerPager = ringPagerAdapter.findInnerPager(outerPager, ringIndex)
            if (innerPager == null) {
                if (retryCount < 15) {
                    outerPager.post { navigateInnerTo(ringIndex, storyIndex, smooth, retryCount + 1) }
                }
                return
            }
            applyInnerNavigation(innerPager, ringIndex, storyIndex, smooth)
        }

        if (retryCount == 0) {
            outerPager.post { tryNavigate() }
        } else {
            tryNavigate()
        }
    }

    private fun applyInnerNavigation(
        innerPager: ViewPager2,
        ringIndex: Int,
        storyIndex: Int,
        smooth: Boolean,
    ) {
        pendingInnerNav = null
        currentStoryIndex = storyIndex
        ringPagerAdapter.suppressInnerPageChange = true
        innerPager.setCurrentItem(storyIndex, smooth)
        ringPagerAdapter.suppressInnerPageChange = false
        onStorySelected(ringIndex, storyIndex)
    }

    private fun ringsStructure(rings: List<StoryRingItem>): List<Pair<String, List<String>>> =
        rings.map { ring -> ring.authorId to ring.stories.map { it.id } }

    private fun submitRingsSafely(rings: List<StoryRingItem>, onCommitted: (() -> Unit)? = null) {
        val pager = binding?.ringPager ?: return
        val applySubmit: () -> Unit = submit@{
            if (!isAdded) return@submit
            val recyclerView = pager.getChildAt(0) as? RecyclerView
            ringPagerAdapter.submitRings(rings, recyclerView) {
                if (isAdded) onCommitted?.invoke()
            }
        }
        pager.post {
            val recyclerView = pager.getChildAt(0) as? RecyclerView
            if (recyclerView != null &&
                (recyclerView.isComputingLayout || recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE)
            ) {
                recyclerView.post(applySubmit)
            } else {
                applySubmit()
            }
        }
    }

    private fun onStorySelected(ringIndex: Int, storyIndex: Int) {
        if (!isAdded) return
        val vm = viewModel ?: return
        val ring = vm.ringAt(ringIndex) ?: return
        val story = vm.storyAt(ringIndex, storyIndex) ?: return
        currentRingIndex = ringIndex
        currentStoryIndex = storyIndex
        bindProgressSegments(ringIndex)
        stopPlayback()
        bindHeader(ring, story)
        updateOwnerMenu(story)
        binding?.ringPager?.post {
            if (!isAdded || currentRingIndex != ringIndex || currentStoryIndex != storyIndex) return@post
            vm.markViewed(story)
            bindPageMedia(ringIndex, storyIndex, story)
            bindPageMusic(ringIndex, storyIndex, story)
            resetPageProgress(storyIndex)
            startSegmentTimer(story, ringIndex, storyIndex)
        }
    }

    private fun bindHeader(ring: StoryRingItem, story: StoryItem) {
        binding?.tvAuthorName?.text = ring.authorName
        binding?.tvStoryTime?.text = context?.let {
            RelativeTimeFormatter.format(it, story.createdAtMillis)
        }.orEmpty()
        context?.loadImg(ring.authorAvatarUrl, binding?.imgAuthor!!, R.drawable.bg_grey_equal)
    }

    private fun bindProgressSegments(ringIndex: Int) {
        val ring = viewModel?.ringAt(ringIndex) ?: return
        val container = binding?.storyProgressContainer ?: return
        val storyCount = ring.stories.size
        if (progressBoundRingIndex == ringIndex &&
            progressSegmentBars.size == storyCount &&
            progressSegmentBars.isNotEmpty()
        ) {
            updateProgressSegments(currentStoryIndex, 0)
            return
        }
        container.removeAllViews()
        progressSegmentBars.clear()
        progressBoundRingIndex = ringIndex
        container.isVisible = storyCount > 0
        val segmentGap = (4 * resources.displayMetrics.density).toInt()
        val segmentHeight = (3 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        repeat(storyCount) { index ->
            val bar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(0, segmentHeight, 1f).apply {
                    if (index < storyCount - 1) marginEnd = segmentGap
                }
                max = 1000
                progress = 0
                progressDrawable = context.getDrawable(R.drawable.story_progress_segment)
            }
            container.addView(bar)
            progressSegmentBars.add(bar)
        }
        updateProgressSegments(currentStoryIndex, 0)
    }

    private fun updateProgressSegments(activeIndex: Int, activeProgress: Int) {
        progressSegmentBars.forEachIndexed { index, bar ->
            bar.progress = when {
                index < activeIndex -> 1000
                index == activeIndex -> activeProgress
                else -> 0
            }
        }
    }

    private fun bindPageMusic(ringIndex: Int, storyIndex: Int, story: StoryItem) {
        val pageBinding = pageBindingAt(ringIndex, storyIndex) ?: return
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

    private fun clearPageMusic(ringIndex: Int, storyIndex: Int) {
        pageBindingAt(ringIndex, storyIndex)?.musicSticker?.clearSticker()
    }

    private fun resetPageProgress(storyIndex: Int) {
        if (storyIndex != currentStoryIndex) return
        updateProgressSegments(storyIndex, 0)
    }

    private fun bindPageMedia(ringIndex: Int, storyIndex: Int, story: StoryItem) {
        val pageBinding = pageBindingAt(ringIndex, storyIndex) ?: return
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

    private fun pageBindingAt(ringIndex: Int, storyIndex: Int): ItemStoryPageBinding? {
        val outerPager = binding?.ringPager ?: return null
        val innerPager = ringPagerAdapter.findInnerPager(outerPager, ringIndex) ?: return null
        val recyclerView = innerPager.getChildAt(0) as? RecyclerView ?: return null
        val holder = recyclerView.findViewHolderForAdapterPosition(storyIndex)
            as? StoryViewerPagerAdapter.PageViewHolder
        return holder?.binding
    }

    private fun ensureVideoPlayer() {
        if (videoPlayer != null) return
        videoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        goNextStory()
                    }
                }
            })
        }
    }

    private fun ensureMusicPlayer() {
        if (musicPlayer != null) return
        musicPlayer = ExoPlayer.Builder(requireContext()).build()
    }

    private fun startSegmentTimer(story: StoryItem, ringIndex: Int, storyIndex: Int) {
        segmentElapsedOffsetMs = 0L
        isTimerPaused = false
        activeTimerStory = story
        activeTimerRingIndex = ringIndex
        activeTimerStoryIndex = storyIndex
        segmentStartMs = System.currentTimeMillis()
        postSegmentTimer(story, ringIndex, storyIndex)
    }

    private fun postSegmentTimer(story: StoryItem, ringIndex: Int, storyIndex: Int) {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = if (story.mediaType == StoryMediaType.VIDEO) {
            object : Runnable {
                override fun run() {
                    if (!isAdded || view == null || isTimerPaused) return
                    if (currentRingIndex != ringIndex || currentStoryIndex != storyIndex) return
                    val elapsed = segmentElapsedMs()
                    val duration = minOf(
                        videoPlayer?.duration?.takeIf { it > 0 } ?: MAX_VIDEO_DURATION_MS,
                        MAX_VIDEO_DURATION_MS,
                    )
                    updatePageProgress(storyIndex, elapsed, duration)
                    if (elapsed >= duration) {
                        goNextStory()
                    } else {
                        handler.postDelayed(this, 32L)
                    }
                }
            }
        } else {
            object : Runnable {
                override fun run() {
                    if (!isAdded || view == null || isTimerPaused) return
                    if (currentRingIndex != ringIndex || currentStoryIndex != storyIndex) return
                    val elapsed = segmentElapsedMs()
                    updatePageProgress(storyIndex, elapsed, IMAGE_DURATION_MS)
                    if (elapsed >= IMAGE_DURATION_MS) {
                        goNextStory()
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
        val story = activeTimerStory
            ?: viewModel?.storyAt(currentRingIndex, currentStoryIndex)
            ?: return
        val ringIndex = activeTimerRingIndex.takeIf { it >= 0 } ?: currentRingIndex
        val storyIndex = activeTimerStoryIndex.takeIf { it >= 0 } ?: currentStoryIndex
        segmentStartMs = System.currentTimeMillis()
        if (story.mediaType == StoryMediaType.VIDEO) {
            videoPlayer?.playWhenReady = true
        }
        musicPlayer?.playWhenReady = true
        postSegmentTimer(story, ringIndex, storyIndex)
    }

    private fun updatePageProgress(storyIndex: Int, elapsed: Long, duration: Long) {
        if (storyIndex != currentStoryIndex) return
        val progress = ((elapsed.toFloat() / duration) * 1000).toInt().coerceIn(0, 1000)
        updateProgressSegments(storyIndex, progress)
    }

    private fun restartCurrentStory() {
        if (!isAdded) return
        val story = viewModel?.storyAt(currentRingIndex, currentStoryIndex) ?: return
        stopPlayback()
        binding?.ringPager?.post {
            if (!isAdded) return@post
            restartPlayback(story)
        }
    }

    private fun restartPlayback(story: StoryItem) {
        bindPageMedia(currentRingIndex, currentStoryIndex, story)
        bindPageMusic(currentRingIndex, currentStoryIndex, story)
        resetPageProgress(currentStoryIndex)
        if (story.mediaType == StoryMediaType.VIDEO) {
            videoPlayer?.seekTo(0)
            videoPlayer?.playWhenReady = true
        }
        if (story.musicAudioUrl.isNotBlank()) {
            musicPlayer?.seekTo(0)
            musicPlayer?.playWhenReady = true
        }
        startSegmentTimer(story, currentRingIndex, currentStoryIndex)
    }

    private fun stopPlayback() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
        segmentElapsedOffsetMs = 0L
        isTimerPaused = false
        activeTimerStory = null
        activeTimerRingIndex = -1
        activeTimerStoryIndex = -1
        pageBindingAt(currentRingIndex, currentStoryIndex)?.videoStory?.player = null
        clearPageMusic(currentRingIndex, currentStoryIndex)
        videoPlayer?.stop()
        videoPlayer?.clearMediaItems()
        musicPlayer?.stop()
        musicPlayer?.clearMediaItems()
    }

    private fun updateOwnerMenu(story: StoryItem) {
        val isOwner = story.authorId == viewModel?.currentUserId()
        binding?.btnMore?.isVisible = isOwner
    }

    private fun showOwnerMenu() {
        val story = viewModel?.storyAt(currentRingIndex, currentStoryIndex) ?: return
        if (story.authorId != viewModel?.currentUserId()) return
        val popup = PopupMenu(requireContext(), binding?.btnMore!!)
        popup.menu.add(0, MENU_PRIVACY, 0, R.string.story_menu_privacy)
        popup.menu.add(0, MENU_DELETE, 1, R.string.story_menu_delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_PRIVACY -> {
                    showPrivacySheet(story)
                    true
                }
                MENU_DELETE -> {
                    confirmDeleteStory(story)
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
                    currentRingIndex = currentRingIndex,
                    currentStoryIndex = currentStoryIndex,
                    onSuccess = { position ->
                        Toast.makeText(requireContext(), R.string.story_delete_success, Toast.LENGTH_SHORT).show()
                        if (position == null) {
                            closeViewer()
                        } else {
                            val (newRingIndex, newStoryIndex) = position
                            binding?.ringPager?.post {
                                navigateToStory(newRingIndex, newStoryIndex, smooth = false)
                            }
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
        suppressRingPageChange = false
        progressSegmentBars.clear()
        progressBoundRingIndex = -1
        lastRingsStructure = emptyList()
        pendingStoryIndex = -1
        pendingInnerNav = null
        super.onDestroyView()
    }

    companion object {
        private const val IMAGE_DURATION_MS = 5_000L
        private const val MAX_VIDEO_DURATION_MS = 30_000L
        private const val VIDEO_VOLUME = 0.7f
        private const val MUSIC_VOLUME = 0.5f
        private const val SWIPE_DOWN_THRESHOLD_PX = 120f
        private const val SWIPE_HORIZONTAL_THRESHOLD_PX = 80f
        private const val TAP_SLOP_PX = 24f
        private const val MENU_PRIVACY = 1
        private const val MENU_DELETE = 2
    }
}
