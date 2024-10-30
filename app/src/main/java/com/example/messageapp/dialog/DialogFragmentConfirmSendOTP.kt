package com.example.messageapp.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.databinding.DialogConfirmSendOtpBinding
import com.example.messageapp.fragment.RegisterFragment
import com.example.messageapp.helper.screenWidth

@SuppressLint("UseGetLayoutInflater")
class DialogFragmentConfirmSendOTP : DialogFragment() {
    private val binding by lazy { DialogConfirmSendOtpBinding.inflate(LayoutInflater.from(context)) }

    companion object {
        const val PHONE_NUMBER_FROM_CONFIRM_SEND_OTP = "PHONE_NUMBER_FROM_CONFIRM_SEND_OTP"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setCancelable(false)
        dialog?.window?.apply {
            setLayout(
                screenWidth - 2 * resources.getDimensionPixelSize(R.dimen.margin_50),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val phoneNumber = arguments?.getString(RegisterFragment.PHONE_NUMBER)
        phoneNumber?.let {
            binding.tvPhoneNumber.text = "(${getString(R.string.header_phone)}) $it"
        }

        binding.btnChange.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(PHONE_NUMBER_FROM_CONFIRM_SEND_OTP, phoneNumber)
            findNavController().navigate(R.id.receiveOTPFragment, bundle)
            dismiss()
        }
    }
}