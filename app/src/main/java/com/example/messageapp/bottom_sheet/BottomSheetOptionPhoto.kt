package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.messageapp.databinding.BottomSheetOptionPhotoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetOptionPhoto : BottomSheetDialogFragment() {
    private val binding by lazy { BottomSheetOptionPhotoBinding.inflate(LayoutInflater.from(context)) }
    private var onOption: OnClickOptionPhoto? = null

    fun setOnClickOptionPho(mOnClickOption: OnClickOptionPhoto) {
        this.onOption = mOnClickOption
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSavePhotoVideo.setOnClickListener {
            onOption?.savePhotoOrVideo()
            dismiss()
        }

        binding.tvRemove.setOnClickListener {
            onOption?.remove()
            dismiss()
        }
    }

    interface OnClickOptionPhoto {
        fun savePhotoOrVideo()
        fun remove()
    }
}