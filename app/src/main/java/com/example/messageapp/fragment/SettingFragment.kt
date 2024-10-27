package com.example.messageapp.fragment

import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentSettingBinding
import com.example.messageapp.viewmodel.SettingFragmentViewModel

class SettingFragment : BaseFragment<FragmentSettingBinding, SettingFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_setting

    override fun initView() {
        super.initView()
    }

    override fun onClickView() {
        super.onClickView()

        binding?.logout?.setOnClickListener {
            findNavController().navigate(
                R.id.loginFragment, null,
                NavOptions.Builder().setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        }
    }
}