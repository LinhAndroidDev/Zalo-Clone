package com.example.messageapp.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private const val DATE_TIME = "yyyy_MM_dd_HH_mm_ss"
    private const val DATE_TIME_APP = "dd-MM-yyyy • HH:mm"
    private const val HOUR_TIME = "HH:mm"
    const val MINUTE_TIME = "mm:ss"

    fun getTimeCurrent(): String {
        val sdf = SimpleDateFormat(DATE_TIME, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh") // Múi giờ Việt Nam
        return sdf.format(Date())
    }

    fun convertTimeToHour(time: String): String {
        val input = SimpleDateFormat(DATE_TIME, Locale.getDefault())
        val output = SimpleDateFormat(HOUR_TIME, Locale.getDefault())
        val date = input.parse(time)
        return output.format(date ?: "")
    }

    fun formatDateTimeApp(input: String): String {
        val inputFormat = SimpleDateFormat(DATE_TIME, Locale.getDefault())
        val outputFormat = SimpleDateFormat(DATE_TIME_APP, Locale.getDefault())

        return try {
            val date = inputFormat.parse(input)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    fun formatTime(input: String): String {
        // Định dạng chuỗi đầu vào
        val inputFormat = SimpleDateFormat(DATE_TIME, Locale.getDefault())
        val date = inputFormat.parse(input) ?: return ""
        val now = Date()
        val diffMs = now.time - date.time

        // Tính toán hiệu số thời gian
        val diffSeconds = diffMs / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60

        return when {
            diffSeconds < 60 -> {
                // Nhỏ hơn 1 phút
                "Vừa xong"
            }

            diffMinutes < 60 -> {
                // Nhỏ hơn 1 tiếng, hiển thị số phút
                "$diffMinutes phút"
            }

            diffHours < 24 -> {
                // Nhỏ hơn 24 giờ, hiển thị số giờ
                "$diffHours giờ"
            }

            diffHours < 48 -> {
                // Từ 24 đến dưới 48 giờ
                "Hôm qua"
            }

            else -> {
                // Trường hợp >= 48 giờ
                val calendarNow = Calendar.getInstance()
                val calendarInput = Calendar.getInstance().apply { time = date }

                // Kiểm tra xem có cùng tuần (cùng năm và cùng tuần của năm hay không)
                if (calendarNow.get(Calendar.YEAR) == calendarInput.get(Calendar.YEAR) &&
                    calendarNow.get(Calendar.WEEK_OF_YEAR) == calendarInput.get(Calendar.WEEK_OF_YEAR)
                ) {
                    // Trả về thứ trong tuần, giả sử thứ Hai là "T2", ..., Chủ nhật là "CN"
                    when (calendarInput.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "T2"
                        Calendar.TUESDAY -> "T3"
                        Calendar.WEDNESDAY -> "T4"
                        Calendar.THURSDAY -> "T5"
                        Calendar.FRIDAY -> "T6"
                        Calendar.SATURDAY -> "T7"
                        Calendar.SUNDAY -> "CN"
                        else -> ""
                    }
                } else {
                    // Nếu không cùng tuần, kiểm tra năm
                    if (calendarNow.get(Calendar.YEAR) == calendarInput.get(Calendar.YEAR)) {
                        // Cùng năm: định dạng "dd/MM"
                        val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                        outputFormat.format(date)
                    } else {
                        // Năm trước: định dạng "dd/MM/yy"
                        val outputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                        outputFormat.format(date)
                    }
                }
            }
        }
    }
}