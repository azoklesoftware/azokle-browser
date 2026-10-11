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

package com.azokle.search.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Text-to-Speech manager for Reader Mode ("Read Aloud").
 * Supports sequential paragraph synthesis, play, pause, resume, stop, and rate adjustment.
 */
class ReaderTextToSpeech(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var paragraphs: List<String> = emptyList()
    private var currentIndex = 0

    enum class State {
        IDLE,
        PLAYING,
        PAUSED
    }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            /**
             * Handles on start events.
             */
            override fun onStart(utteranceId: String?) {
                _state.value = State.PLAYING
            }

            /**
             * Handles on done events.
             */
            override fun onDone(utteranceId: String?) {
                currentIndex++
                if (currentIndex < paragraphs.size) {
                    speakParagraph(currentIndex)
                } else {
                    _state.value = State.IDLE
                    currentIndex = 0
                }
            }

            /**
             * Handles on error events.
             */
            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                Log.e("ReaderTextToSpeech", "TTS utterance error on ID: $utteranceId")
                _state.value = State.IDLE
            }

            /**
             * Handles on error events.
             */
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("ReaderTextToSpeech", "TTS utterance error on ID: $utteranceId, code: $errorCode")
                _state.value = State.IDLE
            }
        })
    }

    /**
     * Handles on init events.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(1.0f)
        } else {
            Log.e("ReaderTextToSpeech", "TextToSpeech init failed with status: $status")
        }
    }

    /**
     * Executes the start reading operation.
     */
    fun startReading(article: ArticleContent) {
        val allText = buildList {
            if (article.title.isNotBlank()) add(article.title)
            if (article.blocks.isNotEmpty()) {
                article.blocks.forEach { block ->
                    if (block.text.isNotBlank()) {
                        add(block.text)
                    }
                }
            } else {
                article.paragraphs.forEach { paragraph ->
                    if (paragraph.isNotBlank()) {
                        add(paragraph)
                    }
                }
            }
        }
        paragraphs = allText
        currentIndex = 0
        if (isInitialized && paragraphs.isNotEmpty()) {
            speakParagraph(currentIndex)
        }
    }

    /**
     * Executes the speak paragraph operation.
     */
    private fun speakParagraph(index: Int) {
        if (!isInitialized || index !in paragraphs.indices) return
        val text = paragraphs[index]
        _state.value = State.PLAYING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reader_p_$index")
    }

    /**
     * Executes the pause operation.
     */
    fun pause() {
        if (_state.value == State.PLAYING) {
            tts?.stop()
            _state.value = State.PAUSED
        }
    }

    /**
     * Executes the resume operation.
     */
    fun resume() {
        if (_state.value == State.PAUSED) {
            speakParagraph(currentIndex)
        }
    }

    /**
     * Executes the stop operation.
     */
    fun stop() {
        tts?.stop()
        currentIndex = 0
        _state.value = State.IDLE
    }

    /**
     * Executes the release operation.
     */
    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
