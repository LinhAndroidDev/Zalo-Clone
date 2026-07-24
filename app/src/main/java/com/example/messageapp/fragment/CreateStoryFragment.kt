package com.example.messageapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.messageapp.R
import com.example.messageapp.adapter.GalleryAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetStoryCustomFriends
import com.example.messageapp.bottom_sheet.BottomSheetStoryMusic
import com.example.messageapp.bottom_sheet.BottomSheetStoryPrivacy
import com.example.messageapp.databinding.FragmentCreateStoryBinding
import com.example.messageapp.domain.model.StoryPrivacy
import com.example.messageapp.model.GalleryItem
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.GalleryUtils
import com.example.messageapp.viewmodel.CreateStoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateStoryFragment : BaseFragment<FragmentCreateStoryBinding, CreateStoryViewModel>() {
    override val layoutResId: Int = R.layout.fragment_create_story

    private var previewPlayer: ExoPlayer? = null
    private var galleryAdapter: GalleryAdapter? = null

    private val requestMediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) {
                loadGalleryItems()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.story_gallery_permission_required,
                    Toast.LENGTH_LONG,
                ).show()
                findNavController().navigateUp()
            }
        }

    override fun initView() {
        super.initView()
        setupGalleryPicker()
        setupPreviewActions()
        setupBackNavigation()
        observeViewModel()

        if (viewModel?.mediaUri?.value == null) {
            showGalleryPicker()
            ensureGalleryPermissionAndLoad()
        } else {
            showPreview()
        }
    }

    private fun setupGalleryPicker() {
        galleryAdapter = GalleryAdapter(
            context = requireContext(),
            singleSelect = true,
            onItemSelected = { item -> onGalleryItemSelected(item) },
        )
        binding?.rcvGallery?.layoutManager = GridLayoutManager(requireContext(), 3)
        binding?.rcvGallery?.adapter = galleryAdapter
        binding?.btnCloseGallery?.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupPreviewActions() {
        binding?.btnClose?.setOnClickListener { backToGalleryPicker() }
        binding?.btnChangeMedia?.setOnClickListener { backToGalleryPicker() }
        binding?.btnPrivacy?.setOnClickListener { showPrivacySheet() }
        binding?.btnAddMusic?.setOnClickListener { showMusicSheet() }
        binding?.btnPublish?.setOnClickListener { viewModel?.publishStory() }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding?.previewContainer?.isVisible == true) {
                        backToGalleryPicker()
                    } else {
                        findNavController().navigateUp()
                    }
                }
            },
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel?.mediaUri?.collectLatest { uri ->
                if (uri == null) return@collectLatest
                bindPreview(uri, viewModel?.isVideo?.value == true)
            }
        }

        lifecycleScope.launch {
            viewModel?.privacy?.collectLatest { privacy ->
                binding?.btnPrivacy?.text = privacyLabel(privacy)
            }
        }

        lifecycleScope.launch {
            viewModel?.selectedMusic?.collectLatest { track ->
                binding?.btnAddMusic?.text = track?.let {
                    getString(R.string.story_music_selected, it.name, it.artistName)
                } ?: getString(R.string.story_add_music)
                bindMusicSticker(track)
            }
        }

        lifecycleScope.launch {
            viewModel?.uploadProgress?.collectLatest { progress ->
                binding?.uploadProgress?.isVisible = progress != null
                progress?.let { binding?.uploadProgress?.progress = it.toInt() }
            }
        }

        lifecycleScope.launch {
            viewModel?.published?.collectLatest { published ->
                if (published) {
                    Toast.makeText(requireContext(), R.string.story_publish_success, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun ensureGalleryPermissionAndLoad() {
        if (hasMediaPermission()) {
            loadGalleryItems()
        } else {
            requestMediaPermissionLauncher.launch(requiredMediaPermissions())
        }
    }

    private fun hasMediaPermission(): Boolean =
        requiredMediaPermissions().any {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredMediaPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun loadGalleryItems() {
        binding?.galleryLoading?.isVisible = true
        binding?.rcvGallery?.isVisible = false
        binding?.tvGalleryEmpty?.isVisible = false

        lifecycleScope.launch {
            try {
                val items = GalleryUtils.getGalleryItems(requireContext())
                galleryAdapter?.updateItems(items)
                binding?.galleryLoading?.isVisible = false
                if (items.isEmpty()) {
                    binding?.tvGalleryEmpty?.isVisible = true
                } else {
                    binding?.rcvGallery?.isVisible = true
                }
            } catch (_: Exception) {
                binding?.galleryLoading?.isVisible = false
                binding?.tvGalleryEmpty?.isVisible = true
                Toast.makeText(
                    requireContext(),
                    R.string.story_gallery_empty,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun onGalleryItemSelected(item: GalleryItem) {
        viewModel?.setMedia(item.contentUri, item.isVideo)
        showPreview()
    }

    private fun showGalleryPicker() {
        binding?.galleryPickerContainer?.isVisible = true
        binding?.previewContainer?.isVisible = false
        releasePreviewPlayer()
        binding?.musicSticker?.pausePlayback()
    }

    private fun showPreview() {
        binding?.galleryPickerContainer?.isVisible = false
        binding?.previewContainer?.isVisible = true
    }

    private fun backToGalleryPicker() {
        viewModel?.clearMedia()
        releasePreviewPlayer()
        showGalleryPicker()
        if (galleryAdapter?.itemCount == 0) {
            ensureGalleryPermissionAndLoad()
        }
    }

    private fun bindPreview(uri: Uri, isVideo: Boolean) {
        binding?.mediaTransformContainer?.resetTransform()
        binding?.imgPreview?.isVisible = !isVideo
        binding?.videoPreview?.isVisible = isVideo
        if (isVideo) {
            ensurePreviewPlayer()
            previewPlayer?.setMediaItem(MediaItem.fromUri(uri))
            previewPlayer?.prepare()
            previewPlayer?.playWhenReady = true
            binding?.videoPreview?.player = previewPlayer
        } else {
            releasePreviewPlayer()
            context?.loadImg(uri.toString(), binding?.imgPreview!!, R.drawable.bg_grey_equal)
        }
    }

    private fun ensurePreviewPlayer() {
        if (previewPlayer != null) return
        previewPlayer = ExoPlayer.Builder(requireContext()).build()
    }

    private fun releasePreviewPlayer() {
        previewPlayer?.release()
        previewPlayer = null
        binding?.videoPreview?.player = null
    }

    private fun showPrivacySheet() {
        BottomSheetStoryPrivacy.newInstance().apply {
            onPrivacySelected = { privacy ->
                viewModel?.setPrivacy(privacy)
                if (privacy == StoryPrivacy.CUSTOM) {
                    showCustomFriendsSheet()
                }
            }
        }.show(childFragmentManager, "BottomSheetStoryPrivacy")
    }

    private fun showCustomFriendsSheet() {
        BottomSheetStoryCustomFriends.newInstance(viewModel?.visibleToUserIds?.value.orEmpty()).apply {
            onFriendsSelected = { ids -> viewModel?.setVisibleFriends(ids) }
        }.show(childFragmentManager, "BottomSheetStoryCustomFriends")
    }

    private fun bindMusicSticker(track: MusicTrackItem?) {
        val sticker = binding?.musicSticker ?: return
        if (track == null) {
            sticker.clearSticker()
            return
        }
        sticker.bindTrack(track)
        sticker.centerInParent()
    }

    private fun showMusicSheet() {
        BottomSheetStoryMusic.newInstance().apply {
            onTrackSelected = { track -> viewModel?.setMusic(track) }
        }.show(childFragmentManager, "BottomSheetStoryMusic")
    }

    private fun privacyLabel(privacy: StoryPrivacy): String = when (privacy) {
        StoryPrivacy.EVERYONE -> getString(R.string.story_privacy_everyone)
        StoryPrivacy.FRIENDS -> getString(R.string.story_privacy_friends)
        StoryPrivacy.CUSTOM -> getString(R.string.story_privacy_custom)
    }

    override fun onPause() {
        binding?.musicSticker?.pausePlayback()
        super.onPause()
    }

    override fun onDestroyView() {
        releasePreviewPlayer()
        binding?.musicSticker?.release()
        galleryAdapter = null
        super.onDestroyView()
    }
}
