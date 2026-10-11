/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.common.components

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

    /**
     * Executes the show operation.
     */
    fun show(message: String, type: ToastType = ToastType.INFO, durationMs: Long = 2500L) {
        dismissJob?.cancel()
        _currentToast.value = ToastMessage(message = message, type = type, durationMs = durationMs)
        dismissJob = scope.launch {
            delay(durationMs)
            _currentToast.value = null
        }
    }

    /**
     * Executes the show success operation.
     */
    fun showSuccess(message: String) = show(message, ToastType.SUCCESS)
    /**
     * Executes the show error operation.
     */
    fun showError(message: String) = show(message, ToastType.ERROR)
    /**
     * Executes the show tor operation.
     */
    fun showTor(message: String) = show(message, ToastType.TOR)
    /**
     * Executes the show fire operation.
     */
    fun showFire(message: String) = show(message, ToastType.FIRE)
    /**
     * Executes the show download operation.
     */
    fun showDownload(message: String) = show(message, ToastType.DOWNLOAD)
    /**
     * Executes the show copy operation.
     */
    fun showCopy(message: String) = show(message, ToastType.COPY)
}
