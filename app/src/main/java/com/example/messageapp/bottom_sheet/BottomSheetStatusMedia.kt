package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.example.messageapp.databinding.BottomSheetStatusMediaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Chọn ảnh cho màn đăng status / nhật ký (không dùng chung bottom sheet avatar / ảnh bìa).
 */
class BottomSheetStatusMedia : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStatusMediaBinding? = null
    private val binding get() = _binding!!

    var onPreviewSelected: (() -> Unit)? = null
    var onTakePhoto: (() -> Unit)? = null
    var onPickFromGallery: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetStatusMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hasMedia = arguments?.getBoolean(ARG_HAS_MEDIA, false) == true
        binding.layoutPreviewSelected.isVisible = hasMedia
        binding.dividerAfterPreview.isVisible = hasMedia

        binding.layoutPreviewSelected.setOnClickListener {
            dismiss()
            onPreviewSelected?.invoke()
        }
        binding.takeNewPhoto.setOnClickListener {
            dismiss()
            onTakePhoto?.invoke()
        }
        binding.selectPhotoOnDevice.setOnClickListener {
            dismiss()
            onPickFromGallery?.invoke()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "BottomSheetStatusMedia"
        private const val ARG_HAS_MEDIA = "hasMedia"

        fun newInstance(hasSelectedImages: Boolean) = BottomSheetStatusMedia().apply {
            arguments = bundleOf(ARG_HAS_MEDIA to hasSelectedImages)
        }
    }
}
