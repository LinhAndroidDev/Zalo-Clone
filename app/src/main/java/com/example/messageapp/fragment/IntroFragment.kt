package com.example.messageapp.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.ViewPagerAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentIntroBinding
import com.example.messageapp.viewmodel.IntroFragmentViewModel

class IntroFragment : BaseFragment<FragmentIntroBinding, IntroFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_intro

    override fun initView() {
        super.initView()

        binding?.let { binding ->
            binding.vpgIntro.adapter = ViewPagerAdapter(requireActivity())
            binding.indicator.setViewPager(binding.vpgIntro)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btnCreateNewAccount?.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        binding?.btnLogin?.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onClickView() {
        super.onClickView()
    }
}