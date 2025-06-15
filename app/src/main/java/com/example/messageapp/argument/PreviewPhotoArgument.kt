package com.example.messageapp.argument

import android.os.Parcelable
import com.example.messageapp.model.Message
import kotlinx.parcelize.Parcelize

@Parcelize
data class PreviewPhotoArgument(
    val message: Message,
    val indexOfPhoto: Int,
    val keyId: String,
    val photoData: ArrayList<String>,
) : Parcelable