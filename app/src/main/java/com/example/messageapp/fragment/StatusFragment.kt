package com.example.messageapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetStatusMedia
import com.example.messageapp.databinding.FragmentStatusBinding
import com.example.messageapp.dialog.StatusImagePreviewDialog
import com.example.messageapp.helper.StatusMediaGridLayout
import com.example.messageapp.model.DiaryLinkPreview
import com.example.messageapp.model.StatusMediaItem
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.LinkPreviewFetcher
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.StatusFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class StatusFragment : BaseFragment<FragmentStatusBinding, StatusFragmentViewModel>() {
    override val layoutResId: Int
        get() = R.layout.fragment_status

    @Inject
    lateinit var shared: SharePreferenceRepository

    /** Khác null khi đang sửa bài có sẵn (đi từ Nhật ký). */
    private var editingPostId: String? = null

    private val selectedMedia = mutableListOf<StatusMediaItem>()
    private val maxSelectedMedia = 10
    private var pendingCameraUri: Uri? = null

    /** Liên kết đính kèm (preview OG) — một bài tối đa một link. */
    private var attachedLink: DiaryLinkPreview? = null

    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult
            addSelectedImages(uris)
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingCameraUri
            if (success && uri != null) {
                addSelectedImages(listOf(uri))
            }
            pendingCameraUri = null
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraCaptureInternal()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.status_camera_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun initView() {
        super.initView()

        binding?.root?.apply {
            binding?.footerViewStatus?.showViewAboveKeyBoard(this)
        }

        applyHeaderTitleFromArgs()
        updatePostState()
        tryLoadPostForEdit()
    }

    private fun applyHeaderTitleFromArgs() {
        val argPostId = arguments?.getString("postId").orEmpty()
        binding?.tvStatusTitle?.text = if (argPostId.isNotBlank()) {
            getString(R.string.status_title_edit_post)
        } else {
            getString(R.string.status_title_create_post)
        }
    }

    private fun tryLoadPostForEdit() {
        val id = arguments?.getString("postId").orEmpty().ifBlank { return }
        FireBaseInstance.getDiaryPost(
            postId = id,
            success = { post ->
                requireActivity().runOnUiThread {
                    val myId = shared.getAuth()
                    if (post.authorUserId != myId) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_update_post),
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                        return@runOnUiThread
                    }
                    editingPostId = id
                    binding?.tvStatusTitle?.text = getString(R.string.status_title_edit_post)
                    binding?.edtStatusContent?.setText(post.content)
                    selectedMedia.clear()
                    post.imageUris.forEach { url ->
                        selectedMedia.add(StatusMediaItem(url.toUri()))
                    }
                    attachedLink = post.linkPreview
                    bindLinkPreviewUi()
                    updatePostState()
                }
            },
            failure = { msg ->
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        )
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnClose?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding?.btnPickImage?.setOnClickListener {
            openStatusMediaBottomSheet()
        }

        binding?.btnPickVideo?.setOnClickListener {
            openStatusMediaBottomSheet()
        }

        binding?.btnAttachLink?.setOnClickListener {
            showLinkInputDialog()
        }

        binding?.statusLinkPreviewCard?.btnRemoveLinkPreview?.setOnClickListener {
            attachedLink = null
            bindLinkPreviewUi()
            updatePostState()
        }

        binding?.btnSend?.setOnClickListener {
            val content = binding?.edtStatusContent?.text?.toString().orEmpty().trim()
            if (content.isEmpty() && selectedMedia.isEmpty() && attachedLink == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.status_need_content_or_image),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            binding?.btnSend?.isEnabled = false
            val editId = editingPostId
            FireBaseInstance.getUserById(
                userId = shared.getAuth(),
                success = { user ->
                    val localUris = selectedMedia.map { it.uri }
                    if (editId != null) {
                        FireBaseInstance.updateDiaryPost(
                            context = requireContext(),
                            postId = editId,
                            editorUserId = shared.getAuth(),
                            content = content,
                            imageUris = localUris,
                            linkPreview = attachedLink,
                            success = {
                                requireActivity().runOnUiThread {
                                    binding?.btnSend?.isEnabled = true
                                    editingPostId = null
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.status_post_updated),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding?.edtStatusContent?.setText("")
                                    selectedMedia.clear()
                                    attachedLink = null
                                    bindLinkPreviewUi()
                                    updatePostState()
                                    findNavController().popBackStack()
                                }
                            },
                            failure = { msg ->
                                requireActivity().runOnUiThread {
                                    binding?.btnSend?.isEnabled = true
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        FireBaseInstance.createDiaryPost(
                            context = requireContext(),
                            authorId = shared.getAuth(),
                            authorName = user.name.orEmpty().ifBlank { shared.getNameUser() },
                            authorAvatarUrl = user.avatar.orEmpty(),
                            content = content,
                            localImageUris = localUris,
                            linkPreview = attachedLink,
                            success = {
                                requireActivity().runOnUiThread {
                                    binding?.btnSend?.isEnabled = true
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.status_post_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    binding?.edtStatusContent?.setText("")
                                    selectedMedia.clear()
                                    attachedLink = null
                                    bindLinkPreviewUi()
                                    updatePostState()
                                    findNavController().popBackStack()
                                }
                            },
                            failure = { msg ->
                                requireActivity().runOnUiThread {
                                    binding?.btnSend?.isEnabled = true
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                },
                failure = { msg ->
                    requireActivity().runOnUiThread {
                        binding?.btnSend?.isEnabled = true
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        binding?.edtStatusContent?.doOnTextChanged { _, _, _, _ ->
            updatePostState()
        }
    }

    private fun openStatusMediaBottomSheet() {
        val sheet = BottomSheetStatusMedia.newInstance(hasSelectedImages = selectedMedia.isNotEmpty())
        sheet.onPreviewSelected = {
            if (selectedMedia.isNotEmpty()) {
                openFullPreview(0)
            }
        }
        sheet.onTakePhoto = { launchCamera() }
        sheet.onPickFromGallery = {
            pickImagesLauncher.launch("image/*")
        }
        sheet.show(parentFragmentManager, BottomSheetStatusMedia.TAG)
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCameraCaptureInternal()
        }
    }

    private fun startCameraCaptureInternal() {
        val ctx = requireContext()
        val file = File(ctx.cacheDir, "status_cam_${System.currentTimeMillis()}.jpg")
        try {
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.status_camera_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addSelectedImages(uris: List<Uri>) {
        val remain = maxSelectedMedia - selectedMedia.size
        if (remain <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.status_max_photos_limit, maxSelectedMedia),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val toAdd = uris.take(remain).map { StatusMediaItem(it) }
        selectedMedia.addAll(toAdd)

        if (uris.size > remain) {
            Toast.makeText(
                requireContext(),
                getString(R.string.status_max_photos_capped, maxSelectedMedia),
                Toast.LENGTH_SHORT
            ).show()
        }
        updatePostState()
    }

    private fun bindLinkPreviewUi() {
        val b = binding ?: return
        val link = attachedLink
        if (link == null) {
            b.layoutStatusLinkPreview.isVisible = false
            return
        }
        b.layoutStatusLinkPreview.isVisible = true
        b.statusLinkPreviewCard.tvLinkTitle.text = link.title
        b.statusLinkPreviewCard.tvLinkHost.text = link.url.toUri().host ?: link.url
        val img = b.statusLinkPreviewCard.imgLinkPreview
        if (!link.imageUrl.isNullOrBlank()) {
            requireContext().loadImg(link.imageUrl, img, R.drawable.bg_grey_equal)
        } else {
            img.setImageResource(R.drawable.bg_grey_equal)
        }
    }

    private fun showLinkInputDialog() {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        val padH = (20 * d).toInt()
        val padV = (12 * d).toInt()
        val input = EditText(ctx).apply {
            hint = getString(R.string.status_link_hint)
            setPadding(padH, padV, padH, padV)
            setText(attachedLink?.url.orEmpty())
        }
        val dialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.status_link_dialog_title)
            .setView(input)
            .setPositiveButton(R.string.status_link_ok, null)
            .setNegativeButton(R.string.status_link_cancel) { dlg, _ -> dlg.dismiss() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = input.text?.toString().orEmpty()
                if (raw.isBlank()) {
                    Toast.makeText(ctx, R.string.status_link_enter_url, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (LinkPreviewFetcher.normalizeUrl(raw) == null) {
                    Toast.makeText(ctx, R.string.status_link_invalid, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                input.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        LinkPreviewFetcher.fetch(raw)
                    }
                    if (!isAdded) return@launch
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    input.isEnabled = true
                    result.fold(
                        onSuccess = { preview ->
                            if (!isAdded) return@fold
                            attachedLink = preview
                            dialog.dismiss()
                            bindLinkPreviewUi()
                            updatePostState()
                        },
                        onFailure = {
                            Toast.makeText(
                                ctx,
                                getString(R.string.status_link_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }
        dialog.show()
    }

    private fun updatePostState() {
        val text = binding?.edtStatusContent?.text?.toString().orEmpty().trim()
        val enablePost = text.isNotEmpty() || selectedMedia.isNotEmpty() || attachedLink != null

        binding?.btnSend?.alpha = if (enablePost) 1f else 0.45f
        binding?.btnSend?.isEnabled = enablePost

        val hasMedia = selectedMedia.isNotEmpty()
        binding?.layoutMediaPreviewContainer?.isVisible = hasMedia
        renderMediaPreview()
    }

    private fun renderMediaPreview() {
        val container = binding?.layoutMediaPreviewContainer ?: return
        val ctx = requireContext()
        StatusMediaGridLayout.render(
            context = ctx,
            container = container,
            uris = selectedMedia.map { it.uri },
            spacingPx = StatusMediaGridLayout.spacingPxDefault(ctx),
            showRemoveControls = true,
            onRemove = { index ->
                if (index in selectedMedia.indices) {
                    selectedMedia.removeAt(index)
                    updatePostState()
                }
            },
            onOpenPreview = { index -> openFullPreview(index) }
        )
    }

    private fun openFullPreview(index: Int) {
        if (selectedMedia.isEmpty() || index !in selectedMedia.indices) return
        val uris = selectedMedia.map { it.uri }
        StatusImagePreviewDialog.newInstance(uris, index)
            .show(parentFragmentManager, "StatusImagePreviewDialog")
    }
}
