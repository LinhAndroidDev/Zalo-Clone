package com.example.messageapp.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import java.text.Normalizer
import java.util.regex.Pattern

fun EditText.showKeyboard() {
    this.isFocusable = true
    this.requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun Fragment.hideKeyboard() {
    val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = requireActivity().currentFocus
    view?.let {
        inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

fun View.showViewAboveKeyBoard(rootView: View) {
    rootView.viewTreeObserver.addOnGlobalLayoutListener {
        val r = Rect()
        rootView.getWindowVisibleDisplayFrame(r)
        val screenHeight = rootView.height
        val keypadHeight = screenHeight - r.bottom

        if (keypadHeight > screenHeight * 0.15) {
            // Bàn phím đã xuất hiện
            this.translationY = -keypadHeight.toFloat() - this.height - 15 // Đẩy view lên
        } else {
            // Bàn phím đã ẩn
            this.translationY = 0f
        }
    }
}

fun Context.vibratePhone(duration: Long = 100) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Dành cho Android 8.0 trở lên
        val vibrationEffect =
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    } else {
        // Dành cho các phiên bản Android cũ hơn
        vibrator.vibrate(duration)
    }
}

fun Context?.getFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Fragment.backRemoveFragmentCurrent(toId: Int) {
    val navController = findNavController()
    val currentDestination = navController.currentDestination
    currentDestination?.let { cDes ->
        findNavController().navigate(toId, null,
            navOptions {
                popUpTo(cDes.id) { inclusive = true }
            })
    }
}

fun getImageDimensions(context: Context, imageUri: Uri): Pair<Int, Int>? {
    val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
    return inputStream.use { stream ->
        // Chỉ lấy thông tin kích thước của ảnh
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(stream, null, options)

        // Trả về chiều rộng và chiều cao của ảnh
        Pair(options.outWidth, options.outHeight)
    }
}

fun removeAccent(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(normalized).replaceAll("")
}

fun getFileNameFromUrl(url: String): String {
    return url.substringAfterLast("/")
}
