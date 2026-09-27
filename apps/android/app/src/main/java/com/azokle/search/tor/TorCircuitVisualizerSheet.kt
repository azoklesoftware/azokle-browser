package com.azokle.search.tor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import kotlinx.coroutines.delay

/**
 * Premium Sovereign Tor Circuit Visualizer Modal.
 * Renders the live 3-hop cryptographic onion circuit (Client -> Guard -> Middle -> Exit -> Destination)
 * with real vector iconography, elegant continuous stepper paths, node roles, latency telemetry, and 1-tap circuit rotation.
 */
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
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.6f))
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
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                TorSheetHeader(
                    connectionState = state,
                    onClose = onDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                    },
                    label = "TorCircuitStateContent"
                ) { targetState ->
                    when (targetState) {
                        is TorConnectionState.Connected -> {
                            // Connected 3-Hop Circuit Visualizer
                            ConnectedCircuitContent(
                                conn = targetState,
                                url = url
                            )
                        }
                        is TorConnectionState.Bootstrapping -> {
                            // Bootstrapping State
                            BootstrappingCircuitContent(conn = targetState)
                        }
                        else -> {
                            // Inactive State
                            InactiveCircuitContent()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Executive Header with Tor emblem, title, pulsing live status badge, and accessible close button.
 */
@Composable
private fun TorSheetHeader(
    connectionState: TorConnectionState,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Tor Emblem with Soft Halo
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.statusTor.copy(alpha = 0.12f))
                    .border(1.dp, colors.statusTor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tor),
                    contentDescription = "Tor Network",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tor Onion Circuit",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Live Status Pill
                    when (connectionState) {
                        is TorConnectionState.Connected -> {
                            StatusBadge(
                                text = "ACTIVE",
                                dotColor = colors.statusTor,
                                badgeBg = colors.statusTor.copy(alpha = 0.14f),
                                textColor = colors.statusTor,
                                isPulse = true
                            )
                        }
                        is TorConnectionState.Bootstrapping -> {
                            StatusBadge(
                                text = "${connectionState.progress}%",
                                dotColor = colors.statusTor,
                                badgeBg = colors.statusTor.copy(alpha = 0.14f),
                                textColor = colors.statusTor,
                                isPulse = false
                            )
                        }
                        else -> {
                            StatusBadge(
                                text = "OFFLINE",
                                dotColor = colors.textMuted,
                                badgeBg = colors.borderSubtle,
                                textColor = colors.textMuted,
                                isPulse = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "3-Hop Cryptographic Onion Tunnel",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        // 44dp Accessible Close Action
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 20.dp, color = colors.textSecondary),
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(15.dp)
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
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .padding(horizontal = 6.5.dp, vertical = 2.5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.5.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.5.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = textColor
        )
    }
}

/**
 * The main 5-node continuous circuit diagram when Tor is connected.
 */
@Composable
private fun ConnectedCircuitContent(
    conn: TorConnectionState.Connected,
    url: String
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
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
        // Circuit Card with continuous vertical path
        val cardShape = AzokleRadius.xl
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.cardBg)
                .border(1.dp, colors.statusTor.copy(alpha = 0.2f), cardShape)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Node 1: Client Device
            CircuitNodeItem(
                icon = Icons.Outlined.Smartphone,
                iconTint = colors.statusTor,
                iconBg = colors.statusTor.copy(alpha = 0.12f),
                tag = "CLIENT",
                title = "This Device",
                subtitle = "Local SOCKS5 (127.0.0.1:9050)",
                isLast = false
            )

            // Node 2: Guard Relay (Entry)
            CircuitNodeItem(
                icon = Icons.Outlined.Shield,
                iconTint = colors.statusTor,
                iconBg = colors.statusTor.copy(alpha = 0.12f),
                tag = "ENTRY GUARD",
                title = conn.guardNode.ifBlank { "Guard Relay (Layer 1)" },
                subtitle = conn.guardCountry.ifBlank { "Encrypted Onion Relay" },
                isLast = false
            )

            // Node 3: Middle Relay
            CircuitNodeItem(
                icon = Icons.AutoMirrored.Outlined.AltRoute,
                iconTint = colors.statusTor,
                iconBg = colors.statusTor.copy(alpha = 0.12f),
                tag = "MIDDLE RELAY",
                title = conn.middleNode.ifBlank { "Middle Relay (Layer 2)" },
                subtitle = conn.middleCountry.ifBlank { "Encrypted Onion Relay" },
                isLast = false
            )

            // Node 4: Exit Relay
            CircuitNodeItem(
                icon = Icons.Outlined.Public,
                iconTint = colors.statusTor,
                iconBg = colors.statusTor.copy(alpha = 0.12f),
                tag = "EXIT RELAY",
                title = conn.exitNode.ifBlank { "Exit Relay (Layer 3)" },
                subtitle = if (conn.exitIp.isNotBlank()) "${conn.exitCountry} • ${conn.exitIp}" else conn.exitCountry.ifBlank { "Verified Tor Exit Network" },
                isLast = false
            )

            // Node 5: Destination
            CircuitNodeItem(
                icon = Icons.Outlined.Lock,
                iconTint = colors.statusTor,
                iconBg = colors.statusTor.copy(alpha = 0.12f),
                tag = if (isOnion) "ONION SERVICE" else "DESTINATION",
                title = hostDisplay,
                subtitle = if (isOnion) "End-to-End Hidden Service v3" else "End-to-End Encrypted (TLS 1.3)",
                isLast = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metadata Chips Row (Latency & Circuit ID)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Latency Chip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(AzokleRadius.md)
                    .background(colors.cardBg)
                    .border(1.dp, colors.statusTor.copy(alpha = 0.18f), AzokleRadius.md)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.statusTor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.statusTor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "LATENCY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (conn.latencyMs > 0) "${conn.latencyMs} ms" else "Active",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            // Circuit ID Chip (Click-to-Copy)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(AzokleRadius.md)
                    .background(colors.cardBg)
                    .border(1.dp, if (copyConfirmed) colors.statusTor.copy(alpha = 0.5f) else colors.statusTor.copy(alpha = 0.18f), AzokleRadius.md)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.statusTor.copy(alpha = 0.2f)),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboardManager.setText(AnnotatedString(conn.circuitId))
                            copyConfirmed = true
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.statusTor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.statusTor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (copyConfirmed) "COPIED" else "CIRCUIT ID",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (copyConfirmed) colors.statusTor else colors.textMuted,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = conn.circuitId.take(10).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = if (copyConfirmed) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = "Copy Circuit ID",
                    modifier = Modifier
                        .size(16.dp)
                        .padding(start = 2.dp),
                    tint = if (copyConfirmed) colors.statusTor else colors.textMuted
                )
            }
        }

        LaunchedEffect(copyConfirmed) {
            if (copyConfirmed) {
                delay(2000)
                copyConfirmed = false
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                .height(48.dp)
                .shadow(
                    elevation = if (isRotating) 0.dp else 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = colors.statusTor.copy(alpha = 0.35f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = colors.statusTor,
                contentColor = Color.White,
                disabledContainerColor = colors.statusTor.copy(alpha = 0.65f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isRotating) rotationAngle else 0f)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = if (isRotating) "Building New Onion Circuit..." else "New Tor Circuit for this Site",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.1.sp
            )
        }
    }
}

/**
 * Individual Node Row in the vertical stepper.
 * Clean, icon-driven, with smooth continuous connecting paths and role badges.
 */
@Composable
private fun CircuitNodeItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
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
            modifier = Modifier.width(36.dp)
        ) {
            // Icon Bubble
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg)
                    .border(0.8.dp, iconTint.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Stepper Connecting Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    iconTint.copy(alpha = 0.45f),
                                    colors.borderSubtle
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(13.dp))

        // Right: Node Metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Role Tag
                Text(
                    text = tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(iconBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bootstrapping progress view while building a new circuit.
 */
@Composable
private fun BootstrappingCircuitContent(
    conn: TorConnectionState.Bootstrapping
) {
    val colors = AzokleTheme.colors
    val cardShape = AzokleRadius.xl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.cardBg)
            .border(1.dp, colors.statusTor.copy(alpha = 0.2f), cardShape)
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = { conn.progress / 100f },
            color = colors.statusTor,
            trackColor = colors.borderSubtle,
            modifier = Modifier.size(50.dp),
            strokeWidth = 3.8.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = conn.statusText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Establishing 3-Hop Encrypted Onion Circuit (${conn.progress}%)",
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

/**
 * Inactive state when Tor browsing is not currently active.
 */
@Composable
private fun InactiveCircuitContent() {
    val colors = AzokleTheme.colors
    val cardShape = AzokleRadius.xl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.cardBg)
            .border(1.dp, colors.borderSubtle, cardShape)
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.VpnLock,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tor Circuit Inactive",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Open a Tor Tab or toggle onion mode to establish a leakproof encrypted circuit.",
            fontSize = 12.sp,
            color = colors.textSecondary,
            lineHeight = 17.sp
        )
    }
}

/**
 * Extracts a clean, human-readable host name from a URL without protocol prefixes or paths.
 */
private fun extractCleanHost(url: String): String {
    if (url.isBlank() || url == "about:blank") return "Encrypted Webpage"
    return try {
        val clean = url.removePrefix("https://").removePrefix("http://")
        clean.substringBefore('/')
    } catch (_: Exception) {
        url.take(32)
    }
}
