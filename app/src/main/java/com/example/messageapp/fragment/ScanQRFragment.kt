package com.example.messageapp.fragment

import android.os.Handler
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentScanQRBinding
import com.example.messageapp.viewmodel.ScanQRFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanQRFragment : BaseFragment<FragmentScanQRBinding, ScanQRFragmentViewModel>() {
    override val layoutResId: Int
        get() = R.layout.fragment_scan_q_r

    private lateinit var codeScanner: CodeScanner

    override fun initView() {
        super.initView()

        Handler().postDelayed({
            binding?.scannerView?.let { codeScanner = CodeScanner(requireActivity(), it) }

            // Parameters (default values)
            codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
            codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
            // ex. listOf(BarcodeFormat.QR_CODE)
            codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
            codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
            codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
            codeScanner.isFlashEnabled = false // Whether to enable flash or not

            codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
                activity?.runOnUiThread {
                    Toast.makeText(requireActivity(), "Camera initialization error: ${it.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }, 300)

        // Callbacks
//        codeScanner.decodeCallback = DecodeCallback {
//            activity?.runOnUiThread {
//                val resultIntent = Intent()
//                resultIntent.putExtra(DATA_CCCD, it.toString())
//                activity?.setResult(RESULT_OK, resultIntent)
//                finish()
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        Handler().postDelayed({ codeScanner.startPreview() }, 300)
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}

