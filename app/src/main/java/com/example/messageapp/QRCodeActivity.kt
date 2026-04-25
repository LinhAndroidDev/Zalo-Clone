package com.example.messageapp

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.databinding.ActivityQrCodeBinding
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.viewmodel.QRCodeViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class QRCodeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityQrCodeBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<QRCodeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                if (user == null) return@collect
                binding.tvUserName.text = user.name
                binding.tvUserId.text = "ID: ${user.keyAuth}"
                loadImg(user.avatar.orEmpty(), binding.imgAvatar, R.drawable.bg_grey_equal)
                val qr = generateQRCode(user.keyAuth.orEmpty(), 600)
                binding.imgQrCode.setImageBitmap(qr)
            }
        }

        viewModel.loadCurrentUser()
    }

    private suspend fun generateQRCode(content: String, sizePx: Int): Bitmap =
        withContext(Dispatchers.Default) {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        }
}
