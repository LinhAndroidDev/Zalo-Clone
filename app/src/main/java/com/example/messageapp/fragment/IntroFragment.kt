package com.example.messageapp.fragment

import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.ViewPagerAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetLanguage
import com.example.messageapp.databinding.FragmentIntroBinding
import com.example.messageapp.viewmodel.IntroFragmentViewModel

enum class Language(val rawValue: Int) {
    VIETNAMESE(0),
    ENGLISH(1);

    companion object {
        fun of(value: Int): Language {
            return entries.firstOrNull { it.rawValue == value}
                ?: throw IllegalArgumentException("Unknown Data")
        }
    }
}

class IntroFragment : BaseFragment<FragmentIntroBinding, IntroFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_intro

    override fun initView() {
        super.initView()

        binding?.let { binding ->
            binding.vpgIntro.adapter = ViewPagerAdapter(requireActivity())
            binding.indicator.setViewPager(binding.vpgIntro)
        }
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnCreateNewAccount?.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        binding?.btnLogin?.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        binding?.btnChangeLanguage?.setOnClickListener {
            val bottomSheet = BottomSheetLanguage()
            bottomSheet.show(parentFragmentManager, "")
        }
    }
}