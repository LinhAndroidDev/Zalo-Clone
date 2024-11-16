package com.example.messageapp.fragment

import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentLoginBinding
import com.example.messageapp.utils.backRemoveFragmentCurrent
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.LoginFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding, LoginFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_login

    override fun initView() {
        super.initView()

        binding?.root?.apply {
            post {
                binding?.edtEnterEmail?.showKeyboard()
            }
            disableBtnLogin()
            binding?.btnLogin?.showViewAboveKeyBoard(this)
        }

        binding?.edtEnterEmail?.doOnTextChanged { text, _, _, _ ->
            checkEnableBtnLogin(text, binding?.edtEnterPassword?.text)
        }

        binding?.edtEnterPassword?.doOnTextChanged { text, _, _, _ ->
            checkEnableBtnLogin(text, binding?.edtEnterEmail?.text)
        }
    }

    override fun bindData() {
        super.bindData()

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.loadingState?.collect {
                binding?.loading?.isVisible = it
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.loginSuccessful?.collect { isSuccess ->
                if (isSuccess) {
                    findNavController().navigate(R.id.homeFragment)
                }
            }
        }
    }

    private fun checkEnableBtnLogin(txtPhone: CharSequence?, txtPassword: CharSequence?) {
        if (txtPhone?.isNotEmpty() == true && txtPassword?.isNotEmpty() == true) {
            enableBtnLogin()
        } else {
            disableBtnLogin()
        }
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnBack?.setOnClickListener {
            backRemoveFragmentCurrent(R.id.introFragment)
        }

        binding?.btnLogin?.setOnClickListener {
            val email = binding?.edtEnterEmail?.text?.toString() ?: ""
            val password = binding?.edtEnterPassword?.text?.toString() ?: ""
            viewModel?.handlerActionLogin(email = email, password = password)
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