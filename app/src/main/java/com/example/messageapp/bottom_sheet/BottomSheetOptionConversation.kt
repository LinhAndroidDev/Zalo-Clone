package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.messageapp.databinding.BottomSheetOptionConversationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetOptionConversation : BottomSheetDialogFragment() {
    private val binding by lazy { BottomSheetOptionConversationBinding.inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}