package com.azokle.search.qr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleThemeColors
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onScanResult: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = AzokleTheme.colors

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            ToastManager.show("Camera permission is required to scan QR codes")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Image Picker for decoding QR codes from Gallery
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val resultText = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                QrCodeImageAnalyzer.decodeFromBitmap(bitmap)
                            } else null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                if (!resultText.isNullOrBlank()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onScanResult(resultText)
                } else {
                    ToastManager.show("No QR code or barcode found in the selected image")
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (hasCameraPermission) {
            CameraViewfinderContent(
                onScanResult = { rawData ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onScanResult(rawData)
                },
                onGalleryClick = { galleryPickerLauncher.launch("image/*") },
                onDismiss = onDismiss,
                colors = colors
            )
        } else {
            PermissionDeniedContent(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onGalleryClick = { galleryPickerLauncher.launch("image/*") },
                onDismiss = onDismiss,
                colors = colors
            )
        }
    }
}

@Composable
private fun CameraViewfinderContent(
    onScanResult: (String) -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit,
    colors: AzokleThemeColors
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val scanBoxSize = 260.dp

        // Camera Preview Layer
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                QrCodeImageAnalyzer { result ->
                                    onScanResult(result)
                                }
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraControl = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (_: Exception) {
                        ToastManager.show("Unable to initialize camera preview")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Viewfinder Darkening Mask + Laser HUD
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear to cut out hole
        ) {
            val boxSizePx = scanBoxSize.toPx()
            val left = (size.width - boxSizePx) / 2f
            val top = (size.height - boxSizePx) / 2f - 40.dp.toPx()

            // 1. Dark semi-transparent background
            drawRect(
                color = Color.Black.copy(alpha = 0.62f),
                size = size
            )

            // 2. Punch out transparent rounded rect viewfinder
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxSizePx, boxSizePx),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // 3. Draw Corner Reticles
            val cornerLength = 28.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()
            val cornerRadiusPx = 16.dp.toPx()
            val reticleColor = Color(0xFF38BDF8) // Vibrant Cyber Accent

            // Top-Left Corner
            drawPath(
                path = Path().apply {
                    moveTo(left, top + cornerLength)
                    lineTo(left, top + cornerRadiusPx)
                    quadraticTo(left, top, left + cornerRadiusPx, top)
                    lineTo(left + cornerLength, top)
                },
                color = reticleColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Top-Right Corner
            drawPath(
                path = Path().apply {
                    moveTo(left + boxSizePx - cornerLength, top)
                    lineTo(left + boxSizePx - cornerRadiusPx, top)
                    quadraticTo(left + boxSizePx, top, left + boxSizePx, top + cornerRadiusPx)
                    lineTo(left + boxSizePx, top + cornerLength)
                },
                color = reticleColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Bottom-Left Corner
            drawPath(
                path = Path().apply {
                    moveTo(left, top + boxSizePx - cornerLength)
                    lineTo(left, top + boxSizePx - cornerRadiusPx)
                    quadraticTo(left, top + boxSizePx, left + cornerRadiusPx, top + boxSizePx)
                    lineTo(left + cornerLength, top + boxSizePx)
                },
                color = reticleColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Bottom-Right Corner
            drawPath(
                path = Path().apply {
                    moveTo(left + boxSizePx - cornerLength, top + boxSizePx)
                    lineTo(left + boxSizePx - cornerRadiusPx, top + boxSizePx)
                    quadraticTo(left + boxSizePx, top + boxSizePx, left + boxSizePx, top + boxSizePx - cornerRadiusPx)
                    lineTo(left + boxSizePx, top + boxSizePx - cornerLength)
                },
                color = reticleColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Animated Scanning Laser Line
        val transition = rememberInfiniteTransition(label = "scannerLaser")
        val laserProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laserPosition"
        )

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val boxSizePx = scanBoxSize.toPx()
            val left = (size.width - boxSizePx) / 2f + 8.dp.toPx()
            val width = boxSizePx - 16.dp.toPx()
            val top = (size.height - boxSizePx) / 2f - 40.dp.toPx() + 8.dp.toPx()
            val height = boxSizePx - 16.dp.toPx()

            val currentLaserY = top + (height * laserProgress)

            // Glowing Laser Gradient
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x0038BDF8),
                        Color(0xFF38BDF8),
                        Color(0xFF67E8F9),
                        Color(0xFF38BDF8),
                        Color(0x0038BDF8)
                    ),
                    startX = left,
                    endX = left + width
                ),
                start = Offset(left, currentLaserY),
                end = Offset(left + width, currentLaserY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Top Navigation & Action Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 21.dp, color = Color.White),
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close scanner",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Scan QR / Barcode",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            // Pick from Gallery Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 21.dp, color = Color.White),
                        onClick = onGalleryClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Choose from gallery",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Instruction & Flashlight Controller
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Align QR code within frame to scan",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Flashlight Toggle Button
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTorchOn) Color(0xFF38BDF8) else Color.Black.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (isTorchOn) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 28.dp, color = Color.White),
                            onClick = {
                                isTorchOn = !isTorchOn
                                cameraControl?.cameraControl?.enableTorch(isTorchOn)
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isTorchOn) "Turn off flashlight" else "Turn on flashlight"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null,
                        tint = if (isTorchOn) Color.Black else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit,
    colors: AzokleThemeColors
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Close Button Top-Left
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Camera Permission Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Azokle Browser needs camera access to scan QR codes and barcodes directly from your environment. Your camera feed is processed strictly on-device.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text(
                    text = "Grant Permission",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Open App Settings", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Scan from Gallery Image", fontSize = 15.sp)
            }
        }
    }
}
