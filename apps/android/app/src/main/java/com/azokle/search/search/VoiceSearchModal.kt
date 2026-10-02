package com.azokle.search.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.VoiceSearchHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sanitizes voice transcriptions to protect against control characters, null bytes,
 * and oversized string buffer anomalies.
 */
private fun sanitizeSpeechQuery(raw: String): String {
    return raw
        .filter { it >= ' ' && it != '\u007F' }
        .trim()
        .take(500)
}

@Composable
fun VoiceSearchModal(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var recognizedText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Listening...") }
    var audioLevel by remember { mutableFloatStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(false) }
    var isDispatched by remember { mutableStateOf(false) }
    var isDismissed by remember { mutableStateOf(false) }
    var dispatchJob: Job? by remember { mutableStateOf(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    fun destroyRecognizer() {
        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Throwable) {
        } finally {
            speechRecognizer = null
            isListening = false
        }
    }

    fun stopListening() {
        try {
            isListening = false
            speechRecognizer?.stopListening()
        } catch (_: Throwable) {}
    }

    fun handleDismiss() {
        if (isDismissed) return
        isDismissed = true
        dispatchJob?.cancel()
        dispatchJob = null
        destroyRecognizer()
        onDismiss()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            statusText = "Microphone access is required"
        }
    }

    fun startListening() {
        if (isDismissed || isInitializing) return

        if (!VoiceSearchHelper.isVoiceSearchAvailable(context)) {
            statusText = "Voice search is not supported on this device"
            hasError = true
            return
        }

        isInitializing = true
        try {
            hasError = false
            isSuccess = false
            destroyRecognizer()

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (isDismissed) return
                    isListening = true
                    hasError = false
                    statusText = "Listening..."
                }

                override fun onBeginningOfSpeech() {
                    if (isDismissed) return
                    statusText = "Hearing you..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (isDismissed) return
                    // Safeguard against NaN / -Infinity from HAL audio streams to prevent Compose animation crashes
                    audioLevel = if (!rmsdB.isNaN() && !rmsdB.isInfinite()) {
                        (rmsdB.coerceIn(0f, 10f) / 10f)
                    } else {
                        0f
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    if (isDismissed) return
                    statusText = "Processing..."
                    isListening = false
                }

                override fun onError(error: Int) {
                    if (isDismissed) return
                    isListening = false
                    hasError = true
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap to retry."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue. Tap to retry."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap to retry."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Tap to retry."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            hasPermission = false
                            "Microphone access required"
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            destroyRecognizer()
                            "Speech service busy. Tap to retry."
                        }
                        SpeechRecognizer.ERROR_SERVER -> "Server error. Tap to retry."
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
                        else -> "Tap microphone to try again"
                    }
                    statusText = errorMsg
                }

                override fun onResults(results: Bundle?) {
                    if (isDismissed || isDispatched) return
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val rawText = matches?.firstOrNull()
                    val sanitized = rawText?.let { sanitizeSpeechQuery(it) }

                    if (!sanitized.isNullOrBlank()) {
                        isDispatched = true
                        recognizedText = sanitized
                        statusText = "Searching..."
                        isSuccess = true
                        isListening = false
                        dispatchJob = coroutineScope.launch {
                            delay(280)
                            if (!isDismissed) {
                                isDismissed = true
                                destroyRecognizer()
                                onResult(sanitized)
                                onDismiss()
                            }
                        }
                    } else {
                        statusText = "No speech detected"
                        hasError = true
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (isDismissed || isDispatched) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val rawText = matches?.firstOrNull()
                    val sanitized = rawText?.let { sanitizeSpeechQuery(it) }
                    if (!sanitized.isNullOrBlank()) {
                        recognizedText = sanitized
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
        } catch (_: Throwable) {
            statusText = "Could not initialize voice recognizer"
            hasError = true
        } finally {
            isInitializing = false
        }
    }

    // Lifecycle Observer: Immediately halts recording when app is paused/stopped (e.g. backgrounded or phone call)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                stopListening()
                destroyRecognizer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            dispatchJob?.cancel()
            dispatchJob = null
            destroyRecognizer()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Gentle pulsing animation for microphone glowing halo
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Secondary soft breathing ring
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    // Hardware/gesture back handler
    BackHandler(onBack = { handleDismiss() })

    // Full-Screen Edge-to-Edge Solid Backdrop with touch consumption
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) Color(0xFF0D0D0D) else Color(0xFFFAFAFA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(560.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Minimalist Clean Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.actionPrimaryBg.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = colors.statusAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Voice Search",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.2).sp
                    )
                }

                // Tactile Close Button
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 20.dp, color = colors.textSecondary),
                            onClick = { handleDismiss() }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close voice search"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // 2. Central Area (Permission Prompt or Live Transcription)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!hasPermission) {
                    // Permission Required State
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Microphone Access Needed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To search with your voice, Azokle requires microphone access. Audio is processed directly and never stored.",
                        fontSize = 13.5.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        shape = AzokleRadius.full,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.actionPrimaryBg,
                            contentColor = colors.actionPrimaryText
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Allow Microphone",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (recognizedText.isNotBlank()) {
                    // Live Partial or Final Speech Transcription
                    if (isSuccess) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(AzokleRadius.full)
                                .background(colors.statusAccent.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.statusAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Searching",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.statusAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = recognizedText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // Listening or Error Status
                    Text(
                        text = statusText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasError) colors.statusDestructive else if (isListening) colors.textPrimary else colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (hasError) "Tap the microphone below to retry" else "Speak to search privately on the web",
                        fontSize = 13.5.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Bottom Controls & Microphone Orb
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Organic Fluid Sound Waves Visualizer (5 bars)
                if (hasPermission) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(30.dp)
                    ) {
                        val barMultipliers = listOf(0.45f, 0.85f, 1.25f, 0.85f, 0.45f)
                        barMultipliers.forEachIndexed { index, mult ->
                            val targetHeight = if (isListening) {
                                val dynamicLevel = (audioLevel * 22f * mult)
                                (7.dp + dynamicLevel.dp).coerceIn(6.dp, 28.dp)
                            } else {
                                4.dp
                            }
                            val animatedBarHeight by animateDpAsState(
                                targetValue = targetHeight,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "waveBar_$index"
                            )

                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(animatedBarHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isListening) colors.statusAccent else colors.border
                                    )
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(30.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Microphone Orb with Glowing Dynamic Halo
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening && hasPermission) {
                        // Outer soft aura
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .scale(auraScale + (audioLevel * 0.18f))
                                .clip(CircleShape)
                                .background(colors.actionPrimaryBg.copy(alpha = 0.08f))
                        )
                        // Inner pulsing ring
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .scale(pulseScale + (audioLevel * 0.12f))
                                .clip(CircleShape)
                                .background(colors.actionPrimaryBg.copy(alpha = 0.18f))
                        )
                    }

                    // Interactive Center Mic Orb
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) colors.actionPrimaryBg else colors.surfaceElevated
                            )
                            .border(
                                width = 1.dp,
                                color = if (isListening) colors.actionPrimaryBg else colors.border,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(
                                    bounded = false,
                                    radius = 37.dp,
                                    color = if (isListening) colors.actionPrimaryText.copy(alpha = 0.25f) else colors.textPrimary.copy(alpha = 0.15f)
                                )
                            ) {
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else if (isListening) {
                                    stopListening()
                                } else {
                                    startListening()
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = if (isListening) "Stop voice listening" else "Start voice listening"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasError) Icons.Default.Refresh else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isListening) colors.actionPrimaryText else colors.textPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Understated Privacy Cue
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Privacy Protected • Audio is never stored",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
