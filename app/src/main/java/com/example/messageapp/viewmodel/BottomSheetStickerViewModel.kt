package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.usecase.social.GetStickerUrlsUseCase
import com.example.messageapp.model.Sticker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject

@HiltViewModel
class BottomSheetStickerViewModel @Inject constructor(
    private val getStickerUrlsUseCase: GetStickerUrlsUseCase,
) : BaseViewModel() {

    private val _stickerHellos = MutableStateFlow<MutableList<String>?>(null)
    val stickerHellos = _stickerHellos.asStateFlow()
    private val _stickerLoves = MutableStateFlow<MutableList<String>?>(null)
    val stickerLoves = _stickerLoves.asStateFlow()
    private val _stickerCongratulations = MutableStateFlow<MutableList<String>?>(null)
    val stickerCongratulations = _stickerCongratulations.asStateFlow()
    private val _stickerAngries = MutableStateFlow<MutableList<String>?>(null)
    val stickerAngries = _stickerAngries.asStateFlow()
    private val _stickerSads = MutableStateFlow<MutableList<String>?>(null)
    val stickerSads = _stickerSads.asStateFlow()
    private val _stickerSorries = MutableStateFlow<MutableList<String>?>(null)
    val stickerSorries = _stickerSorries.asStateFlow()

    fun initData() {
        viewModelScope.launch {
            val results = awaitAll(
                async { Sticker.HELLO to fetchSticker(Sticker.HELLO) },
                async { Sticker.LOVE to fetchSticker(Sticker.LOVE) },
                async { Sticker.CONGRATULATION to fetchSticker(Sticker.CONGRATULATION) },
                async { Sticker.ANGRY to fetchSticker(Sticker.ANGRY) },
                async { Sticker.SAD to fetchSticker(Sticker.SAD) },
                async { Sticker.SORRY to fetchSticker(Sticker.SORRY) },
            ).toMap()

            _stickerHellos.value = results[Sticker.HELLO]?.toMutableList()
            _stickerLoves.value = results[Sticker.LOVE]?.toMutableList()
            _stickerCongratulations.value = results[Sticker.CONGRATULATION]?.toMutableList()
            _stickerAngries.value = results[Sticker.ANGRY]?.toMutableList()
            _stickerSads.value = results[Sticker.SAD]?.toMutableList()
            _stickerSorries.value = results[Sticker.SORRY]?.toMutableList()
        }
    }

    private suspend fun fetchSticker(type: Sticker): List<String> =
        suspendCoroutine { continuation ->
            getStickerUrlsUseCase(typeName = type.name, onSuccess = { result ->
                continuation.resume(result)
            })
        }
}
