package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentRegisterBinding
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.RegisterFragmentViewModel

class RegisterFragment : BaseFragment<FragmentRegisterBinding, RegisterFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_register

    @SuppressLint("SetTextI18n")
    override fun initView() {
        super.initView()

        binding?.root?.post {
            binding?.edtEnterPhone?.showKeyboard()
        }
        binding?.edtEnterPhone?.setOnFocusChangeListener { _, b ->
            binding?.viewEnterPhone?.isSelected = b
            binding?.imgDown?.imageTintList = ContextCompat.getColorStateList(
                requireActivity(),
                if (b) R.color.blue else R.color.black
            )
        }

        binding?.root?.let { root -> binding?.viewLoginNow?.showViewAboveKeyBoard(root) }

        binding?.edtEnterPhone?.filters = arrayOf(InputFilter.LengthFilter(19))
        binding?.edtEnterPhone?.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                binding?.btnClearText?.isVisible = s?.isNotEmpty() == true

                if (isUpdating) {
                    return
                }

                isUpdating = true
                val phoneNumber = s.toString().replace(" ", "")
                if (phoneNumber.length < 5) {
                    binding?.btnContinueRegister?.disableBtnContinue()
                } else if (binding?.cbConditionUse?.isChecked == true && binding?.cbConditionInternet?.isChecked == true) {
                    binding?.btnContinueRegister?.enableBtnContinue()
                }

                // Xử lý từng nhóm số
                val formattedNumber = StringBuilder()
                for (i in phoneNumber.indices) {
                    formattedNumber.append(phoneNumber[i])
                    // Thêm khoảng trắng sau các nhóm 4, 4, 4, 1 số
                    if ((i == 3 || i == 7 || i == 11) && i != phoneNumber.length - 1) {
                        formattedNumber.append(" ")
                    }
                }

                // Cập nhật lại EditText mà không làm ảnh hưởng đến con trỏ nhập liệu
                binding?.edtEnterPhone?.removeTextChangedListener(this)
                binding?.edtEnterPhone?.setText(formattedNumber.toString())
                binding?.edtEnterPhone?.setSelection(formattedNumber.length)
                binding?.edtEnterPhone?.addTextChangedListener(this)

                isUpdating = false
            }
        })
    }

    @SuppressLint("SetTextI18n")
    override fun onClickView() {
        super.onClickView()
        binding?.backRegister?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding?.btnClearText?.setOnClickListener {
            binding?.edtEnterPhone?.setText("")
        }

        binding?.cbConditionUse?.setOnCheckedChangeListener { _, b ->
            val phoneNumber = binding?.edtEnterPhone?.text.toString().length
            if (b && binding?.cbConditionInternet?.isChecked == true && phoneNumber > 4) {
                binding?.btnContinueRegister?.enableBtnContinue()
            } else {
                binding?.btnContinueRegister?.disableBtnContinue()
            }
        }

        binding?.cbConditionInternet?.setOnCheckedChangeListener { _, b ->
            val phoneNumber = binding?.edtEnterPhone?.text.toString().length
            if (b && binding?.cbConditionUse?.isChecked == true && phoneNumber > 4) {
                binding?.btnContinueRegister?.enableBtnContinue()
            } else {
                binding?.btnContinueRegister?.disableBtnContinue()
            }
        }
    }

    private fun TextView.enableBtnContinue() {
        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
    }

    private fun TextView.disableBtnContinue() {
        setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_1))
        backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_bg)
    }

}