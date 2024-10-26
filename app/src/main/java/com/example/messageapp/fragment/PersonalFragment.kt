package com.example.messageapp.fragment

import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentPersonalBinding
import com.example.messageapp.viewmodel.PersonalFragmentViewModel

class PersonalFragment : BaseFragment<FragmentPersonalBinding, PersonalFragmentViewModel>() {
    override val layoutResId = R.layout.fragment_personal

    override fun initView() {
        super.initView()
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnLogout?.setOnClickListener {
            findNavController().navigate(R.id.loginFragment,null,
                NavOptions.Builder().setPopUpTo(findNavController().graph.startDestinationId, true).build())
        }
    }
}