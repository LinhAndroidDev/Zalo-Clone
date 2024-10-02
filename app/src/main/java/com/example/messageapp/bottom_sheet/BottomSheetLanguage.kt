package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.messageapp.databinding.BottomSheetLanguageBinding
import com.example.messageapp.fragment.Language
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetLanguage : BottomSheetDialogFragment() {
    private val binding by lazy { BottomSheetLanguageBinding.inflate(LayoutInflater.from(context)) }
    var onSelectLanguage: ((Language) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vietnamese.setOnClickListener {
            onSelectLanguage?.invoke(Language.VIETNAMESE)
        }

        binding.english.setOnClickListener {
            onSelectLanguage?.invoke(Language.ENGLISH)
        }
    }
}