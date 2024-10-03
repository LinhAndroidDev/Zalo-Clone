package com.example.messageapp.fragment

import android.os.CountDownTimer
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentReceiveOTPBinding
import com.example.messageapp.viewmodel.ReceiveOTPFragmentViewModel

class ReceiveOTPFragment : BaseFragment<FragmentReceiveOTPBinding, ReceiveOTPFragmentViewModel>() {
    private var timeReceiverOTP = 10
    private var timer: CountDownTimer? = null

    override val layoutResId: Int = R.layout.fragment_receive_o_t_p

    override fun initView() {
        super.initView()

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


