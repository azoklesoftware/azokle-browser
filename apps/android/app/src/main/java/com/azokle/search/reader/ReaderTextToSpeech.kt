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
            override fun onStart(utteranceId: String?) {
                _state.value = State.PLAYING
            }

            override fun onDone(utteranceId: String?) {
                currentIndex++
                if (currentIndex < paragraphs.size) {
                    speakParagraph(currentIndex)
                } else {
                    _state.value = State.IDLE
                    currentIndex = 0
                }
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                Log.e("ReaderTextToSpeech", "TTS utterance error on ID: $utteranceId")
                _state.value = State.IDLE
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("ReaderTextToSpeech", "TTS utterance error on ID: $utteranceId, code: $errorCode")
                _state.value = State.IDLE
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(1.0f)
        } else {
            Log.e("ReaderTextToSpeech", "TextToSpeech init failed with status: $status")
        }
    }

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

    private fun speakParagraph(index: Int) {
        if (!isInitialized || index !in paragraphs.indices) return
        val text = paragraphs[index]
        _state.value = State.PLAYING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reader_p_$index")
    }

    fun pause() {
        if (_state.value == State.PLAYING) {
            tts?.stop()
            _state.value = State.PAUSED
        }
    }

    fun resume() {
        if (_state.value == State.PAUSED) {
            speakParagraph(currentIndex)
        }
    }

    fun stop() {
        tts?.stop()
        currentIndex = 0
        _state.value = State.IDLE
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
