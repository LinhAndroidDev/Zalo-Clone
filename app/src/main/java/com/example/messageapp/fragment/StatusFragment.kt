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
import com.example.messageapp.data.DiaryFeedLocalRepository
import com.example.messageapp.databinding.FragmentStatusBinding
import com.example.messageapp.dialog.StatusImagePreviewDialog
import com.example.messageapp.helper.StatusMediaGridLayout
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.StatusMediaItem
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.StatusFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class StatusFragment : BaseFragment<FragmentStatusBinding, StatusFragmentViewModel>() {
    override val layoutResId: Int
        get() = R.layout.fragment_status

    @Inject
    lateinit var diaryFeedLocalRepository: DiaryFeedLocalRepository

    @Inject
    lateinit var shared: SharePreferenceRepository

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
                Toast.makeText(requireActivity(), "Vui lòng nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FireBaseInstance.getUserById(
                userId = shared.getAuth(),
                success = { user ->
                    val post = DiaryPost(
                        id = UUID.randomUUID().toString(),
                        authorUserId = shared.getAuth(),
                        authorName = user.name.orEmpty().ifBlank { shared.getNameUser() },
                        authorAvatarUrl = user.avatar.orEmpty(),
                        content = content,
                        imageUris = selectedMedia.map { it.uri.toString() },
                        createdAtMillis = System.currentTimeMillis()
                    )
                    diaryFeedLocalRepository.addPost(post)
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireActivity(), "Đã đăng bài", Toast.LENGTH_SHORT).show()
                        binding?.edtStatusContent?.setText("")
                        selectedMedia.clear()
                        updatePostState()
                        findNavController().popBackStack()
                    }
                },
                failure = { msg ->
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireActivity(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
        bottomSheet.seeImage = {
            Toast.makeText(requireActivity(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
        bottomSheet.show(parentFragmentManager, "BottomSheetSelectImage")
    }

    private fun addSelectedImages(uris: List<Uri>) {
        val remain = maxSelectedMedia - selectedMedia.size
        if (remain <= 0) {
            Toast.makeText(requireActivity(), "Bạn chỉ có thể chọn tối đa $maxSelectedMedia ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val toAdd = uris.take(remain).map { StatusMediaItem(it) }
        selectedMedia.addAll(toAdd)

        if (uris.size > remain) {
            Toast.makeText(requireActivity(), "Đã giới hạn tối đa $maxSelectedMedia ảnh", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireActivity(), "Đã xoá ảnh", Toast.LENGTH_SHORT).show()
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