package com.example.messageapp.utils

import android.content.Context
import com.example.messageapp.R

object RelativeTimeFormatter {

    fun format(context: Context, createdAtMillis: Long): String {
        if (createdAtMillis <= 0L) return ""
        val diff = System.currentTimeMillis() - createdAtMillis
        if (diff < 0) return context.getString(R.string.time_just_now)

        val sec = diff / 1000
        return when {
            sec < 60 -> context.getString(R.string.time_just_now)
            sec < 3600 -> context.getString(R.string.time_minutes_ago, sec / 60)
            sec < 86_400 -> context.getString(R.string.time_hours_ago, sec / 3600)
            else -> {
                val days = sec / 86_400
                when {
                    days < 7 -> context.getString(R.string.time_days_ago, days)
                    days < 30 -> context.getString(R.string.time_weeks_ago, days / 7)
                    days < 365 -> context.getString(R.string.time_months_ago, days / 30)
                    else -> context.getString(R.string.time_years_ago, days / 365)
                }
            }
        }
    }
}
