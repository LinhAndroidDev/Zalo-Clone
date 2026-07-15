package com.example.messageapp.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.model.Message

object ForwardMessageHelper {

    fun bindForwardLabel(
        context: Context,
        labelRoot: View?,
        message: Message,
        nameContext: MessageReplyHelper.ReplyNameContext,
    ) {
        if (labelRoot == null) {
            return
        }
        val tvLabel = labelRoot.findViewById<TextView>(R.id.tvForwardLabel)
        if (message.forwardFromId.isBlank()) {
            labelRoot.isVisible = false
            return
        }
        labelRoot.isVisible = true

        val syncName = resolveForwardDisplayName(message, nameContext)
        if (syncName.isNotBlank()) {
            tvLabel.text = context.getString(R.string.forward_from_format, syncName)
            tvLabel.tag = message.forwardFromId
            return
        }

        tvLabel.text = context.getString(R.string.forward_label)
        tvLabel.tag = message.forwardFromId
        val requestUserId = message.forwardFromId
        MessageReplyHelper.fetchUserDisplayName(requestUserId) { fetchedName ->
            if (tvLabel.tag != requestUserId) return@fetchUserDisplayName
            if (fetchedName.isBlank()) return@fetchUserDisplayName
            tvLabel.text = context.getString(R.string.forward_from_format, fetchedName)
        }
    }

    private fun resolveForwardDisplayName(
        message: Message,
        nameContext: MessageReplyHelper.ReplyNameContext,
    ): String {
        if (message.forwardFromName.isNotBlank() &&
            !MessageReplyHelper.isStoredNameUnresolved(message.forwardFromName, message.forwardFromId)
        ) {
            return message.forwardFromName
        }
        return MessageReplyHelper.resolveReplyDisplayName(
            senderId = message.forwardFromId,
            storedName = message.forwardFromName,
            context = nameContext,
        )
    }
}
