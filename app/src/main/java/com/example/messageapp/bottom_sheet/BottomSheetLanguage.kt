package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.messageapp.databinding.BottomSheetLanguageBinding
import com.example.messageapp.fragment.Language
import com.example.messageapp.viewmodel.BottomSheetLanguageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetLanguage : BottomSheetDialogFragment() {
    private val binding by lazy { BottomSheetLanguageBinding.inflate(LayoutInflater.from(context)) }
    private val viewModel by viewModels<BottomSheetLanguageViewModel>()
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

        viewModel.shared.getLanguageSelected()
            .let { language -> setViewLanguage(language) }

        binding.vietnamese.setOnClickListener {
            viewModel.shared.saveLanguageSelected(Language.VIETNAMESE)
            onSelectLanguage?.invoke(Language.VIETNAMESE)
            dismiss()
        }

        binding.english.setOnClickListener {
            viewModel.shared.saveLanguageSelected(Language.ENGLISH)
            onSelectLanguage?.invoke(Language.ENGLISH)
            dismiss()
        }
    }

    private fun setViewLanguage(language: Language) {
        when (language) {
            Language.VIETNAMESE -> {
                binding.checkVietnamese.isVisible = true
                binding.checkEnglish.isVisible = false
            }

            else -> {
                binding.checkVietnamese.isVisible = false
                binding.checkEnglish.isVisible = true
            }
        }
    }
}