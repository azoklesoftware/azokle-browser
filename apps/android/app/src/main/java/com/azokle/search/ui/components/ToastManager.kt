package com.azokle.search.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ToastType {
    INFO,
    SUCCESS,
    ERROR,
    DOWNLOAD,
    TOR,
    FIRE,
    COPY
}

data class ToastMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 2500L
)

object ToastManager {
    private val _currentToast = MutableStateFlow<ToastMessage?>(null)
    val currentToast = _currentToast.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var dismissJob: Job? = null

    fun show(message: String, type: ToastType = ToastType.INFO, durationMs: Long = 2500L) {
        dismissJob?.cancel()
        _currentToast.value = ToastMessage(message = message, type = type, durationMs = durationMs)
        dismissJob = scope.launch {
            delay(durationMs)
            _currentToast.value = null
        }
    }

    fun showSuccess(message: String) = show(message, ToastType.SUCCESS)
    fun showError(message: String) = show(message, ToastType.ERROR)
    fun showTor(message: String) = show(message, ToastType.TOR)
    fun showFire(message: String) = show(message, ToastType.FIRE)
    fun showDownload(message: String) = show(message, ToastType.DOWNLOAD)
    fun showCopy(message: String) = show(message, ToastType.COPY)
}
