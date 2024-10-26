package com.example.messageapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val DATE_TIME = "yyyy_MM_dd_HH_mm_ss"
    private const val HOUR_TIME = "HH:mm"

    fun getTimeCurrent(): String {
        val simpleDateFormat = SimpleDateFormat(DATE_TIME, Locale.getDefault())
        return simpleDateFormat.format(Date())
    }

    fun convertTimeToHour(time: String): String {
        val input = SimpleDateFormat(DATE_TIME, Locale.getDefault())
        val output = SimpleDateFormat(HOUR_TIME, Locale.getDefault())
        val date = input.parse(time)
        return output.format(date ?: "")
    }
}