package com.example.messageapp.fragment

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetSelectImage
import com.example.messageapp.databinding.FragmentStatusBinding
import com.example.messageapp.dialog.StatusImagePreviewDialog
import com.example.messageapp.helper.StatusMediaGridLayout
import com.example.messageapp.model.StatusMediaItem
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.StatusFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult
            addSelectedImages(uris)
        }

    override fun initView() {
        super.initView()

        binding?.root?.apply {
            binding?.footerViewStatus?.showViewAboveKeyBoard(this)
        }

        updatePostState()
        tryLoadPostForEdit()
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
                    binding?.edtStatusContent?.setText(post.content)
                    selectedMedia.clear()
                    post.imageUris.forEach { url ->
                        selectedMedia.add(StatusMediaItem(Uri.parse(url)))
                    }
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
            openSelectImageBottomSheet()
        }

        binding?.btnPickVideo?.setOnClickListener {
            openSelectImageBottomSheet()
        }

        binding?.btnSend?.setOnClickListener {
            val content = binding?.edtStatusContent?.text?.toString().orEmpty().trim()
            if (content.isEmpty() && selectedMedia.isEmpty()) {
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

    private fun openSelectImageBottomSheet() {
        val bottomSheet = BottomSheetSelectImage()
        bottomSheet.selectPhotoOnDevice = {
            pickImagesLauncher.launch("image/*")
        }
        bottomSheet.takeNewPhoto = {
            Toast.makeText(
                requireContext(),
                getString(R.string.status_feature_developing),
                Toast.LENGTH_SHORT
            ).show()
        }
        bottomSheet.seeImage = {
            Toast.makeText(
                requireContext(),
                getString(R.string.status_feature_developing),
                Toast.LENGTH_SHORT
            ).show()
        }
        bottomSheet.show(parentFragmentManager, "BottomSheetSelectImage")
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

    private fun updatePostState() {
        val text = binding?.edtStatusContent?.text?.toString().orEmpty().trim()
        val enablePost = text.isNotEmpty() || selectedMedia.isNotEmpty()

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
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.status_photo_removed),
                        Toast.LENGTH_SHORT
                    ).show()
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