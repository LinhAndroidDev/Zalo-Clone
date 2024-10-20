package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.databinding.BottomSheetRegisterBinding
import com.example.messageapp.viewmodel.BottomSheetRegisterViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BottomSheetRegister : BottomSheetDialogFragment() {
    private val binding by lazy { BottomSheetRegisterBinding.inflate(LayoutInflater.from(context)) }
    private val viewModel by viewModels<BottomSheetRegisterViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    private fun bindData() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.loadingState.collect { isLoading ->
                binding.loading.isVisible = isLoading
                binding.btnRegister.isVisible = !isLoading
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.registerSuccessful.collect { isSuccess ->
                if(isSuccess) {
                    dismiss()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.messageState.collect {
                if(it.isNotEmpty()) {
                    Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.errorState.collect {
                if(it.isNotEmpty()) {
                    Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindData()

        binding.btnRegister.setOnClickListener {
            val name = binding.edtName.text.toString()
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()
            val passwordAgain = binding.edtPasswordAgain.text.toString()
            if(name.isEmpty() && email.isEmpty() && password.isEmpty() && passwordAgain.isEmpty()) {
                Toast.makeText(requireActivity(), "Bạn chưa nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            } else if(password != passwordAgain) {
                Toast.makeText(requireActivity(), "Mật khẩu nhập lại không chính xác", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.handlerActionRegister(name, email, password)
            }

        }
    }
}