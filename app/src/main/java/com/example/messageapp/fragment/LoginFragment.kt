package com.example.messageapp.fragment

import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentLoginBinding
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.LoginFragmentViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_login

    private val db by lazy { Firebase.firestore }

    override fun initView() {
        super.initView()

        binding?.root?.apply {
            post {
                binding?.edtEnterEmail?.showKeyboard()
            }
            disableBtnLogin()
            this.let { root -> binding?.btnLogin?.showViewAboveKeyBoard(root) }
        }

        binding?.edtEnterEmail?.doOnTextChanged { text, _, _, _ ->
            checkEnableBtnLogin(text, binding?.edtEnterPassword?.text)
        }

        binding?.edtEnterPassword?.doOnTextChanged { text, _, _, _ ->
            checkEnableBtnLogin(text, binding?.edtEnterEmail?.text)
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
            binding?.loading?.isVisible = true
            val docRef = db.collection("users").get()

            docRef.addOnSuccessListener { result ->
                binding?.loading?.isVisible = false

                var isExistAccount = false
                for (document in result) {
                    val data = document.data as Map<String, String?>
                    val email = binding?.edtEnterEmail?.text?.toString()
                    val password = binding?.edtEnterPassword?.text?.toString()
                    if(data["email"] == email && data["password"] == password) {
                        isExistAccount = true
                        findNavController().navigate(R.id.homeFragment)
                        break
                    }
                }

                if(!isExistAccount) {
                    Toast.makeText(requireActivity(), "Dont Exist Account", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                binding?.loading?.isVisible = false
                Toast.makeText(requireActivity(), e.message, Toast.LENGTH_SHORT).show()
            }
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