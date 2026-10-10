package com.azokle.search.tor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorCircuitVisualizerSheet(
    url: String,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by TorCircuitManager.connectionState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .adaptiveContentWidth(AzokleBreakpoints.MaxSheetWidth)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                TorSheetHeader(
                    connectionState = state,
                    onClose = onDismiss
                )

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
                    },
                    label = "TorCircuitStateContent"
                ) { targetState ->
                    when (targetState) {
                        is TorConnectionState.Connected -> {
                            ConnectedCircuitContent(
                                conn = targetState,
                                url = url
                            )
                        }
                        is TorConnectionState.Bootstrapping -> {
                            BootstrappingCircuitContent(conn = targetState)
                        }
                        else -> {
                            InactiveCircuitContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorSheetHeader(
    connectionState: TorConnectionState,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { heading() }
        ) {
            Text(
                text = "Tor Circuit",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))

            when (connectionState) {
                is TorConnectionState.Connected -> {
                    StatusBadge(
                        text = "ACTIVE",
                        dotColor = colors.statusTor,
                        badgeBg = colors.statusTor.copy(alpha = 0.12f),
                        textColor = colors.statusTor,
                        isPulse = true
                    )
                }
                is TorConnectionState.Bootstrapping -> {
                    StatusBadge(
                        text = "${connectionState.progress}%",
                        dotColor = colors.statusTor,
                        badgeBg = colors.statusTor.copy(alpha = 0.12f),
                        textColor = colors.statusTor,
                        isPulse = false
                    )
                }
                else -> {
                    StatusBadge(
                        text = "OFFLINE",
                        dotColor = colors.textMuted,
                        badgeBg = colors.surfaceElevated,
                        textColor = colors.textMuted,
                        isPulse = false
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                    onClick = onClose
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Close Tor Circuit visualizer"
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    dotColor: Color,
    badgeBg: Color,
    textColor: Color,
    isPulse: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_badge")
    val pulseAlpha by if (isPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_dot_alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
private fun ConnectedCircuitContent(
    conn: TorConnectionState.Connected,
    url: String
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var isRotating by remember { mutableStateOf(false) }
    var copyConfirmed by remember { mutableStateOf(false) }

    val hostDisplay = remember(url) {
        extractCleanHost(url)
    }
    val isOnion = remember(url) {
        url.contains(".onion", ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circuit Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AzokleRadius.lg)
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.statusTor.copy(alpha = 0.25f), AzokleRadius.lg)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            // Node 1: Client Device
            CircuitNodeItem(
                icon = Icons.Outlined.Smartphone,
                tag = "CLIENT",
                title = "This Device",
                subtitle = "SOCKS5 127.0.0.1:9050",
                isLast = false
            )

            // Node 2: Guard Relay (Entry)
            CircuitNodeItem(
                icon = Icons.Outlined.Shield,
                tag = "GUARD",
                title = conn.guardNode.ifBlank { "Guard Relay" },
                subtitle = conn.guardCountry.ifBlank { "Encrypted Relay" },
                isLast = false
            )

            // Node 3: Middle Relay
            CircuitNodeItem(
                icon = Icons.AutoMirrored.Outlined.AltRoute,
                tag = "MIDDLE",
                title = conn.middleNode.ifBlank { "Middle Relay" },
                subtitle = conn.middleCountry.ifBlank { "Encrypted Relay" },
                isLast = false
            )

            // Node 4: Exit Relay
            CircuitNodeItem(
                icon = Icons.Outlined.Public,
                tag = "EXIT",
                title = conn.exitNode.ifBlank { "Exit Relay" },
                subtitle = if (conn.exitIp.isNotBlank()) "${conn.exitCountry} • ${conn.exitIp}" else conn.exitCountry.ifBlank { "Tor Exit Network" },
                isLast = false
            )

            // Node 5: Destination
            CircuitNodeItem(
                icon = Icons.Outlined.Lock,
                tag = if (isOnion) "ONION" else "DESTINATION",
                title = hostDisplay,
                subtitle = if (isOnion) "Hidden Service v3" else "TLS Encrypted",
                isLast = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Telemetry Row: Latency & Circuit ID
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AzokleRadius.md)
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.statusTor.copy(alpha = 0.2f), AzokleRadius.md)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Latency
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = colors.statusTor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LATENCY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted
                    )
                    Text(
                        text = if (conn.latencyMs > 0) "${conn.latencyMs} ms" else "Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }

            // Circuit ID (1-Tap Copy)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.statusTor.copy(alpha = 0.15f)),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clipboardManager.setText(AnnotatedString(conn.circuitId))
                            copyConfirmed = true
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (copyConfirmed) Icons.Outlined.Check else Icons.Outlined.Key,
                    contentDescription = "Circuit ID",
                    modifier = Modifier.size(15.dp),
                    tint = colors.statusTor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (copyConfirmed) "COPIED" else "CIRCUIT ID",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (copyConfirmed) colors.statusTor else colors.textMuted
                    )
                    Text(
                        text = conn.circuitId.take(8).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }
            }
        }

        LaunchedEffect(copyConfirmed) {
            if (copyConfirmed) {
                delay(2000)
                copyConfirmed = false
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rotate Circuit Action Button
        val infiniteTransition = rememberInfiniteTransition(label = "rotate_anim")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )

        ElevatedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isRotating = true
                TorCircuitManager.requestNewCircuit()
            },
            enabled = !isRotating,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = AzokleRadius.md,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = colors.statusTor,
                contentColor = Color.White,
                disabledContainerColor = colors.statusTor.copy(alpha = 0.6f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (isRotating) rotationAngle else 0f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRotating) "Building New Circuit..." else "New Tor Circuit",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
        }
    }
}

@Composable
private fun CircuitNodeItem(
    icon: ImageVector,
    tag: String,
    title: String,
    subtitle: String,
    isLast: Boolean
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Left: Node Icon + Stepper Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            // Icon Bubble
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.statusTor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.statusTor,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Stepper Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(18.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    colors.statusTor.copy(alpha = 0.45f),
                                    colors.borderSubtle
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right: Node Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, bottom = if (isLast) 0.dp else 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = tag,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.statusTor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.statusTor.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BootstrappingCircuitContent(
    conn: TorConnectionState.Bootstrapping
) {
    val colors = AzokleTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.statusTor.copy(alpha = 0.25f), AzokleRadius.lg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = { conn.progress / 100f },
            color = colors.statusTor,
            trackColor = colors.borderSubtle,
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = conn.statusText.ifBlank { "Connecting to Tor..." },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "Building 3-hop circuit (${conn.progress}%)",
            fontSize = 11.5.sp,
            color = colors.textMuted
        )
    }
}

@Composable
private fun InactiveCircuitContent() {
    val colors = AzokleTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.VpnLock,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tor Circuit Inactive",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "Open a Tor Tab to route traffic through onion relays",
            fontSize = 11.5.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

private fun extractCleanHost(url: String): String {
    if (url.isBlank() || url == "about:blank") return "Webpage"
    return try {
        val clean = url.removePrefix("https://").removePrefix("http://")
        clean.substringBefore('/')
    } catch (_: Exception) {
        url.take(32)
    }
}
