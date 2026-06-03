package com.example.messageapp.bottom_sheet

import androidx.fragment.app.Fragment
import com.example.messageapp.fragment.ChatFragment

internal fun Fragment.requireChatFragment(): ChatFragment {
    generateSequence(parentFragment) { it.parentFragment }
        .filterIsInstance<ChatFragment>()
        .firstOrNull()
        ?.let { return it }

    parentFragment?.childFragmentManager?.fragments
        ?.filterIsInstance<ChatFragment>()
        ?.firstOrNull()
        ?.let { return it }

    error("${javaClass.simpleName} must be shown from ChatFragment")
}
