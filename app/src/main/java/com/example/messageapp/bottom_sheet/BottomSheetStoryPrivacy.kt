package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.messageapp.databinding.BottomSheetStoryPrivacyBinding
import com.example.messageapp.domain.model.StoryPrivacy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetStoryPrivacy : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStoryPrivacyBinding? = null
    private val binding get() = _binding!!

    var onPrivacySelected: ((StoryPrivacy) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetStoryPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.optionEveryone.setOnClickListener {
            onPrivacySelected?.invoke(StoryPrivacy.EVERYONE)
            dismiss()
        }
        binding.optionFriends.setOnClickListener {
            onPrivacySelected?.invoke(StoryPrivacy.FRIENDS)
            dismiss()
        }
        binding.optionCustom.setOnClickListener {
            onPrivacySelected?.invoke(StoryPrivacy.CUSTOM)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BottomSheetStoryPrivacy()
    }
}
