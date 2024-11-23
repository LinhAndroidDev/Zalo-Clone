package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.messageapp.R
import com.example.messageapp.databinding.BottomSheetSelectImageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetSelectImage : BottomSheetDialogFragment() {
    private val binding by lazy { BottomSheetSelectImageBinding.inflate(layoutInflater) }
    var seeImage: (() -> Unit)? = null
    var takeNewPhoto: (() -> Unit)? = null
    var selectPhotoOnDevice: (() -> Unit)? = null

    companion object {
        const val BOTTOM_SHEET_AVATAR = "BOTTOM_SHEET_AVATAR"
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

        val isAvatar = arguments?.getBoolean(BOTTOM_SHEET_AVATAR, true)

        Log.e("isAvatar", "$isAvatar")

        binding.tvShowImage.text = if (isAvatar == true) {
            getString(R.string.view_profile_picture)
        } else {
            getString(R.string.view_cover_photo)
        }

        binding.seeImage.setOnClickListener {
            seeImage?.invoke()
            dismiss()
        }

        binding.takeNewPhoto.setOnClickListener {
            takeNewPhoto?.invoke()
            dismiss()
        }

        binding.selectPhotoOnDevice.setOnClickListener {
            selectPhotoOnDevice?.invoke()
            dismiss()
        }
    }
}