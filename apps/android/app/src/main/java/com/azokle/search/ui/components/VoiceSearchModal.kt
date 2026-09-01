package com.azokle.search.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

@Composable
fun VoiceSearchModal(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors

    var recognizedText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Listening...") }
    var audioLevel by remember { mutableFloatStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            statusText = "Microphone permission required"
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            statusText = "Speech recognition unavailable on this device"
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    statusText = "Listening..."
                }

                override fun onBeginningOfSpeech() {
                    statusText = "Hearing you..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    audioLevel = (rmsdB.coerceIn(0f, 10f) / 10f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    statusText = "Processing..."
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap to retry."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error. Tap to retry."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap to retry."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        else -> "Tap microphone to try again."
                    }
                    statusText = errorMsg
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        recognizedText = text
                        statusText = "Searching..."
                        onResult(text)
                        onDismiss()
                    } else {
                        statusText = "No speech detected"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        recognizedText = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            statusText = "Voice recognizer initialization failed"
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }

    // Pulse animation for listening indicator
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // BackHandler for instant dismiss on system gesture / hardware back
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    // Full-Screen Edge-to-Edge Overlay (Zero Window Gap / 100% Solid Backdrop)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) Color(0xFF0D0D0D) else Color(0xFFFAFAFA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(560.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 18.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
                // 1. Top Header Bar (Close button & Mode badge) with Safe Margin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, AzokleRadius.full)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = colors.statusAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "VOICE SEARCH",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // 48dp Close Target
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 20.dp, color = colors.textSecondary),
                                onClick = onDismiss
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Close voice search"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Center Transcription Text Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (recognizedText.isNotBlank()) {
                        Text(
                            text = recognizedText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp
                        )
                    } else {
                        Text(
                            text = statusText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isListening) colors.textPrimary else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Say what you want to find on the web",
                            fontSize = 13.5.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3. Bottom Controls & Microphone Orb with Generous Clearance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Minimal Sound Waves Bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(28.dp)
                    ) {
                        val barCount = 7
                        val barMultipliers = listOf(0.4f, 0.7f, 1.0f, 1.3f, 1.0f, 0.7f, 0.4f)

                        barMultipliers.forEach { mult ->
                            val height = if (isListening) {
                                (8.dp + (audioLevel * 20 * mult).dp).coerceIn(6.dp, 28.dp)
                            } else {
                                4.dp
                            }

                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isListening) colors.statusAccent else colors.border)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Center Mic Orb Button (Clean, Glowing, Apple-Calm)
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale + (audioLevel * 0.15f))
                                    .clip(CircleShape)
                                    .background(colors.actionPrimaryBg.copy(alpha = 0.18f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(if (isListening) colors.actionPrimaryBg else colors.surfaceElevated)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isListening) colors.actionPrimaryBg else colors.border,
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 37.dp, color = colors.actionPrimaryText.copy(alpha = 0.2f))
                                ) {
                                    if (isListening) {
                                        speechRecognizer?.stopListening()
                                    } else {
                                        startListening()
                                    }
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (isListening) "Stop listening" else "Start voice listening"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isListening) colors.actionPrimaryText else colors.textPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Minimal Suggestion Pills (Well above system navigation pill)
                    Text(
                        text = "Search • Open URLs • Ask questions",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
