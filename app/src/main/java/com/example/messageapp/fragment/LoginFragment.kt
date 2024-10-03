package com.example.messageapp.fragment

import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentLoginBinding
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.LoginFragmentViewModel

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_login

    override fun initView() {
        super.initView()

        binding?.root?.apply {
            post {
                binding?.edtEnterPhone?.showKeyboard()
            }
            disableBtnLogin()
            this.let { root -> binding?.btnLogin?.showViewAboveKeyBoard(root) }
        }

        binding?.edtEnterPhone?.doOnTextChanged { text, _, _, _ ->
            checkEnableBtnLogin(text, binding?.edtEnterPassword?.text)
        }

        binding?.edtEnterPassword?.doOnTextChanged { text, _, _, _ ->
            checkEnableBtnLogin(text, binding?.edtEnterPhone?.text)
        }
    }

    private fun checkEnableBtnLogin(txtPhone: CharSequence?, txtPassword: CharSequence?) {
        if(txtPhone?.isNotEmpty() == true && txtPassword?.isNotEmpty() == true) {
            enableBtnLogin()
        } else {
            disableBtnLogin()
        }
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

    private fun enableBtnLogin() {
        binding?.btnLogin?.isEnabled = true
        binding?.btnLogin?.alpha = 1f
    }

    private fun disableBtnLogin() {
        binding?.btnLogin?.isEnabled = false
        binding?.btnLogin?.alpha = 0.3f
    }
}