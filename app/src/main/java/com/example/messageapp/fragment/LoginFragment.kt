package com.example.messageapp.fragment

import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentLoginBinding
import com.example.messageapp.viewmodel.LoginFragmentViewModel

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_login

    override fun initView() {
        super.initView()
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding?.btnLogin?.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }
}