package com.example.messageapp.fragment

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.messageapp.PersonalActivity
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentScanQRBinding
import com.example.messageapp.viewmodel.ScanQRFragmentViewModel
import com.example.messageapp.viewmodel.ScanResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanQRFragment : BaseFragment<FragmentScanQRBinding, ScanQRFragmentViewModel>() {
    override val layoutResId: Int
        get() = R.layout.fragment_scan_q_r

    private lateinit var codeScanner: CodeScanner

    override fun initView() {
        super.initView()

        Handler(Looper.getMainLooper()).postDelayed({
            binding?.scannerView?.let { codeScanner = CodeScanner(requireActivity(), it) }

            codeScanner.camera = CodeScanner.CAMERA_BACK
            codeScanner.formats = CodeScanner.ALL_FORMATS
            codeScanner.autoFocusMode = AutoFocusMode.SAFE
            codeScanner.scanMode = ScanMode.SINGLE
            codeScanner.isAutoFocusEnabled = true
            codeScanner.isFlashEnabled = false

            codeScanner.errorCallback = ErrorCallback {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireActivity(),
                        "Lỗi camera: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            codeScanner.decodeCallback = DecodeCallback { result ->
                activity?.runOnUiThread {
                    viewModel?.verifyScannedId(result.text)
                }
            }
        }, 300)
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.scanResult?.collect { state ->
                when (state) {
                    is ScanResult.Idle -> setLoading(false)

                    is ScanResult.Loading -> setLoading(true)

                    is ScanResult.UserFound -> {
                        setLoading(false)
                        openPersonalActivity(state.user.keyAuth.orEmpty())
                        viewModel?.resetState()
                    }

                    is ScanResult.Error -> {
                        setLoading(false)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        // Resume scanning after error so user can try again
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (::codeScanner.isInitialized) codeScanner.startPreview()
                            viewModel?.resetState()
                        }, 1500)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            if (::codeScanner.isInitialized) codeScanner.startPreview()
        }, 300)
    }

    override fun onPause() {
        if (::codeScanner.isInitialized) codeScanner.releaseResources()
        super.onPause()
    }

    private fun setLoading(show: Boolean) {
        binding?.progressBar?.isVisible = show
        binding?.scannerView?.isVisible = !show
    }

    private fun openPersonalActivity(userId: String) {
        val intent = Intent(requireActivity(), PersonalActivity::class.java)
        intent.putExtra(PersonalActivity.FRIEND_ID_KEY, userId)
        requireActivity().startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        requireActivity().onBackPressed()
    }
}
