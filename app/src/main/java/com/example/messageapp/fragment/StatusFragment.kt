package com.example.messageapp.fragment

import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetSelectImage
import com.example.messageapp.databinding.FragmentStatusBinding
import com.example.messageapp.dialog.StatusImagePreviewDialog
import com.example.messageapp.model.StatusMediaItem
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.StatusFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatusFragment : BaseFragment<FragmentStatusBinding, StatusFragmentViewModel>() {
    override val layoutResId: Int
        get() = R.layout.fragment_status
    private val selectedMedia = mutableListOf<StatusMediaItem>()
    private val maxSelectedMedia = 10
    private val spacingPx by lazy { dp(4) }
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
            Toast.makeText(requireActivity(), "Bài viết đã sẵn sàng để đăng", Toast.LENGTH_SHORT).show()
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
        container.removeAllViews()
        if (selectedMedia.isEmpty()) return

        when (selectedMedia.size) {
            1 -> renderOne(container)
            2 -> renderTwo(container)
            3 -> renderThree(container)
            4 -> renderFour(container)
            else -> renderFiveOrMore(container)
        }
    }

    private fun renderOne(container: FrameLayout) {
        val layout = rowLayout()
        layout.addView(createSingleAdaptiveCell(0))
        container.addView(layout)
    }

    private fun renderTwo(container: FrameLayout) {
        val layout = rowLayout()
        layout.addView(createCell(0, 180, 1f))
        layout.addView(spaceView())
        layout.addView(createCell(1, 180, 1f))
        container.addView(layout)
    }

    private fun renderThree(container: FrameLayout) {
        val root = columnLayout()
        root.addView(createCell(0, 190))
        root.addView(spaceView(vertical = true))
        val row = rowLayout()
        row.addView(createCell(1, 130, 1f))
        row.addView(spaceView())
        row.addView(createCell(2, 130, 1f))
        root.addView(row)
        container.addView(root)
    }

    private fun renderFour(container: FrameLayout) {
        val root = columnLayout()
        val top = rowLayout()
        top.addView(createCell(0, 130, 1f))
        top.addView(spaceView())
        top.addView(createCell(1, 130, 1f))
        val bottom = rowLayout().apply { topMargin(spacingPx) }
        bottom.addView(createCell(2, 130, 1f))
        bottom.addView(spaceView())
        bottom.addView(createCell(3, 130, 1f))
        root.addView(top)
        root.addView(bottom)
        container.addView(root)
    }

    private fun renderFiveOrMore(container: FrameLayout) {
        val root = columnLayout()
        val top = rowLayout()
        top.addView(createCell(0, 120, 1f))
        top.addView(spaceView())
        top.addView(createCell(1, 120, 1f))
        val bottom = rowLayout().apply { topMargin(spacingPx) }
        bottom.addView(createCell(2, 120, 1f))
        bottom.addView(spaceView())
        bottom.addView(createCell(3, 120, 1f))
        bottom.addView(spaceView())
        val extra = selectedMedia.size - 5
        bottom.addView(createCell(4, 120, 1f, overlayMoreCount = if (extra > 0) extra else null))
        root.addView(top)
        root.addView(bottom)
        container.addView(root)
    }

    private fun createCell(
        index: Int,
        heightDp: Int,
        weight: Float? = null,
        overlayMoreCount: Int? = null
    ): ViewGroup {
        val frame = FrameLayout(requireContext()).apply {
            val heightPx = dp(heightDp)
            layoutParams = if (weight != null) {
                LinearLayout.LayoutParams(0, heightPx, weight)
            } else {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            }
            background = requireContext().getDrawable(R.drawable.bg_grey_equal)
            clipToOutline = true
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        frame.addView(imageView)

        Glide.with(this)
            .load(selectedMedia[index].uri)
            .placeholder(R.drawable.bg_grey_equal)
            .error(R.drawable.bg_grey_equal)
            .into(imageView)

        if (overlayMoreCount != null && overlayMoreCount > 0) {
            val overlay = TextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                text = "+$overlayMoreCount"
                setTextColor(resources.getColor(R.color.white, null))
                textSize = 26f
                setBackgroundColor(0x66000000)
            }
            frame.addView(overlay)
        }

        val removeBtn = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(6)
                marginEnd = dp(6)
            }
            background = requireContext().getDrawable(R.drawable.bg_circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(0x99000000.toInt())
            setPadding(dp(5), dp(5), dp(5), dp(5))
            setImageResource(R.drawable.ic_close)
            imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.white, null))
        }
        frame.addView(removeBtn)

        removeBtn.setOnClickListener {
            if (index in selectedMedia.indices) {
                selectedMedia.removeAt(index)
                updatePostState()
                Toast.makeText(requireActivity(), "Đã xoá ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        frame.setOnClickListener {
            openFullPreview(index)
        }

        return frame
    }

    private fun createSingleAdaptiveCell(index: Int): ViewGroup {
        val frame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = requireContext().getDrawable(R.drawable.bg_grey_equal)
            clipToOutline = true
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        frame.addView(imageView)

        Glide.with(this)
            .load(selectedMedia[index].uri)
            .placeholder(R.drawable.bg_grey_equal)
            .error(R.drawable.bg_grey_equal)
            .into(imageView)

        val removeBtn = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(6)
                marginEnd = dp(6)
            }
            background = requireContext().getDrawable(R.drawable.bg_circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(0x99000000.toInt())
            setPadding(dp(5), dp(5), dp(5), dp(5))
            setImageResource(R.drawable.ic_close)
            imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.white, null))
        }
        frame.addView(removeBtn)

        removeBtn.setOnClickListener {
            if (index in selectedMedia.indices) {
                selectedMedia.removeAt(index)
                updatePostState()
                Toast.makeText(requireActivity(), "Đã xoá ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        frame.setOnClickListener {
            openFullPreview(index)
        }

        return frame
    }

    private fun rowLayout() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun columnLayout() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun spaceView(vertical: Boolean = false): ViewGroup {
        return FrameLayout(requireContext()).apply {
            layoutParams = if (vertical) {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacingPx)
            } else {
                LinearLayout.LayoutParams(spacingPx, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
    }

    private fun ViewGroup.topMargin(value: Int) {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        params.topMargin = value
        layoutParams = params
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun openFullPreview(index: Int) {
        if (selectedMedia.isEmpty() || index !in selectedMedia.indices) return
        val uris = selectedMedia.map { it.uri }
        StatusImagePreviewDialog.newInstance(uris, index)
            .show(parentFragmentManager, "StatusImagePreviewDialog")
    }
}