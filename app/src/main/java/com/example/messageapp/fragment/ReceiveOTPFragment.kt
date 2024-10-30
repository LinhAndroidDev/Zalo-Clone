package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.CountDownTimer
import android.text.InputFilter
import android.util.Log
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentReceiveOTPBinding
import com.example.messageapp.dialog.DialogFragmentConfirmSendOTP
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.viewmodel.ReceiveOTPFragmentViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

class ReceiveOTPFragment : BaseFragment<FragmentReceiveOTPBinding, ReceiveOTPFragmentViewModel>() {
    private var timeReceiverOTP = 60
    private var timer: CountDownTimer? = null

    override val layoutResId: Int = R.layout.fragment_receive_o_t_p

    private val auth by lazy { Firebase.auth }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        super.initView()

        auth.useAppLanguage()
        binding?.edtEnterOTP?.filters = arrayOf(InputFilter.LengthFilter(6))
        binding?.edtEnterOTP?.setOnFocusChangeListener { view, b ->
            binding?.viewCoverOtp?.isSelected = b
        }
        binding?.root?.post { binding?.edtEnterOTP?.showKeyboard() }

        val phoneNumber =
            arguments?.getString(DialogFragmentConfirmSendOTP.PHONE_NUMBER_FROM_CONFIRM_SEND_OTP)
        phoneNumber?.let { verificationPhoneNumber(getString(R.string.header_phone) + phoneNumber.substring(1)) }
        binding?.tvPhoneNumber?.text = "(${getString(R.string.header_phone)}) $phoneNumber"
        binding?.tvTimeReceiveOTP?.isEnabled = false
        binding?.tvTimeReceiveOTP?.text =
            String.format(getString(R.string.receive_otp_in_time), timeReceiverOTP.toString())
        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(p0: Long) {
                timeReceiverOTP--
                binding?.tvTimeReceiveOTP?.text = String.format(
                    getString(R.string.receive_otp_in_time),
                    timeReceiverOTP.toString()
                )
            }

            override fun onFinish() {
                binding?.tvTimeReceiveOTP?.isEnabled = true
                binding?.tvTimeReceiveOTP?.text = getString(R.string.send_otp_again)
            }

        }.start()
    }

    private fun verificationPhoneNumber(phoneNumber: String) {
        Log.d(this.javaClass.simpleName, "phoneNumber:$phoneNumber")
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(this.javaClass.simpleName, "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.d(this.javaClass.simpleName, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                    // reCAPTCHA verification attempted with null Activity
                }

                // Show a message and update the UI
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(this.javaClass.simpleName, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
//                storedVerificationId = verificationId
//                resendToken = token
            }
        }


        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity()) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
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


