package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.text.InputFilter
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentReceiveOTPBinding
import com.example.messageapp.dialog.DialogFragmentConfirmSendOTP
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.viewmodel.ReceiveOTPFragmentViewModel

class ReceiveOTPFragment : BaseFragment<FragmentReceiveOTPBinding, ReceiveOTPFragmentViewModel>() {
    private var timeReceiverOTP = 10
    private var timer: CountDownTimer? = null

    override val layoutResId: Int = R.layout.fragment_receive_o_t_p

    @SuppressLint("SetTextI18n")
    override fun initView() {
        super.initView()

        binding?.edtEnterOTP?.filters = arrayOf(InputFilter.LengthFilter(6))
        binding?.edtEnterOTP?.setOnFocusChangeListener { view, b ->
            binding?.viewCoverOtp?.isSelected = b
        }
        binding?.root?.post { binding?.edtEnterOTP?.showKeyboard() }

        val phoneNumber = arguments?.getString(DialogFragmentConfirmSendOTP.PHONE_NUMBER_FROM_CONFIRM_SEND_OTP)
        binding?.tvPhoneNumber?.text = "(${getString(R.string.header_phone)}) $phoneNumber"
        binding?.tvTimeReceiveOTP?.isEnabled = false
        binding?.tvTimeReceiveOTP?.text =
            String.format(getString(R.string.receive_otp_in_time), timeReceiverOTP.toString())
        timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(p0: Long) {
                timeReceiverOTP--
                binding?.tvTimeReceiveOTP?.text = String.format(getString(R.string.receive_otp_in_time), timeReceiverOTP.toString())
            }

            override fun onFinish() {
                binding?.tvTimeReceiveOTP?.isEnabled = true
                binding?.tvTimeReceiveOTP?.text = getString(R.string.send_otp_again)
            }

        }.start()
    }

    override fun onClickView() {
        super.onClickView()

        binding?.tvTimeReceiveOTP?.setOnClickListener {
            timeReceiverOTP = 10
            binding?.tvTimeReceiveOTP?.isEnabled = false
            binding?.tvTimeReceiveOTP?.text =
                String.format(getString(R.string.receive_otp_in_time), timeReceiverOTP.toString())
            timer?.start()
        }
    }

    override fun onStop() {
        super.onStop()
        timer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}


