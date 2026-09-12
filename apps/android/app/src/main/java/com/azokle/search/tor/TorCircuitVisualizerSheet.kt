package com.azokle.search.tor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import kotlinx.coroutines.delay

/**
 * Premium Sovereign Tor Circuit Visualizer Modal.
 * Renders the live 3-hop cryptographic onion circuit (Client -> Guard -> Middle -> Exit -> Destination)
 * with real vector iconography, elegant continuous stepper paths, node roles, latency, and 1-tap circuit rotation.
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
        containerColor = colors.modalBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.8f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .adaptiveContentWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            TorSheetHeader(
                connectionState = state,
                onClose = onDismiss
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (val conn = state) {
                is TorConnectionState.Connected -> {
                    // Connected 3-Hop Circuit Visualizer
                    ConnectedCircuitContent(
                        conn = conn,
                        url = url
                    )
                }
                is TorConnectionState.Bootstrapping -> {
                    // Bootstrapping State
                    BootstrappingCircuitContent(conn = conn)
                }
                else -> {
                    // Inactive State
                    InactiveCircuitContent()
                }
            }
        }
    }
}

/**
 * Clean Executive Header with Tor emblem, title, live status badge, and close button.
 */
@Composable
private fun TorSheetHeader(
    connectionState: TorConnectionState,
    onClose: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Tor Emblem
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.statusTor.copy(alpha = 0.14f))
                    .border(1.dp, colors.statusTor.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tor),
                    contentDescription = "Tor Network",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tor Onion Circuit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Pill
                    when (connectionState) {
                        is TorConnectionState.Connected -> {
                            StatusBadge(
                                text = "ACTIVE",
                                dotColor = Color(0xFF10B981),
                                badgeBg = Color(0xFF10B981).copy(alpha = 0.15f),
                                textColor = Color(0xFF10B981)
                            )
                        }
                        is TorConnectionState.Bootstrapping -> {
                            StatusBadge(
                                text = "${connectionState.progress}%",
                                dotColor = Color(0xFFF59E0B),
                                badgeBg = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                textColor = Color(0xFFF59E0B)
                            )
                        }
                        else -> {
                            StatusBadge(
                                text = "OFFLINE",
                                dotColor = colors.textMuted,
                                badgeBg = colors.borderSubtle,
                                textColor = colors.textMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "3-Hop Cryptographic Onion Tunnel",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
        }

        // Close Icon Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    dotColor: Color,
    badgeBg: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
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

    // Circuit Card with continuous vertical path
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        // Node 1: Client Device
        CircuitNodeItem(
            icon = Icons.Outlined.Smartphone,
            iconTint = Color(0xFFA855F7),
            iconBg = Color(0xFFA855F7).copy(alpha = 0.12f),
            tag = "CLIENT",
            title = "This Device",
            subtitle = "Local SOCKS5 (127.0.0.1:9050)",
            isLast = false
        )

        // Node 2: Guard Relay (Entry)
        CircuitNodeItem(
            icon = Icons.Outlined.Shield,
            iconTint = Color(0xFF38BDF8),
            iconBg = Color(0xFF38BDF8).copy(alpha = 0.12f),
            tag = "ENTRY GUARD",
            title = conn.guardNode.ifBlank { "Guard Relay (Layer 1)" },
            subtitle = conn.guardCountry,
            isLast = false
        )

        // Node 3: Middle Relay
        CircuitNodeItem(
            icon = Icons.AutoMirrored.Outlined.AltRoute,
            iconTint = Color(0xFFC084FC),
            iconBg = Color(0xFFC084FC).copy(alpha = 0.12f),
            tag = "MIDDLE RELAY",
            title = conn.middleNode.ifBlank { "Middle Relay (Layer 2)" },
            subtitle = conn.middleCountry,
            isLast = false
        )

        // Node 4: Exit Relay
        CircuitNodeItem(
            icon = Icons.Outlined.Public,
            iconTint = Color(0xFFF59E0B),
            iconBg = Color(0xFFF59E0B).copy(alpha = 0.12f),
            tag = "EXIT RELAY",
            title = conn.exitNode.ifBlank { "Exit Relay (Layer 3)" },
            subtitle = if (conn.exitIp.isNotBlank()) "${conn.exitCountry} • ${conn.exitIp}" else conn.exitCountry,
            isLast = false
        )

        // Node 5: Destination
        CircuitNodeItem(
            icon = Icons.Outlined.Lock,
            iconTint = Color(0xFF10B981),
            iconBg = Color(0xFF10B981).copy(alpha = 0.12f),
            tag = if (isOnion) "ONION SERVICE" else "DESTINATION",
            title = hostDisplay,
            subtitle = if (isOnion) "End-to-End Hidden Service v3" else "End-to-End Encrypted (TLS 1.3)",
            isLast = true
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Metadata Chips Row (Latency & Circuit ID)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Latency Chip
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (conn.latencyMs in 1..400) Color(0xFF10B981) else Color(0xFFF59E0B)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "LATENCY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${conn.latencyMs} ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        }

        // Circuit ID Chip (Click-to-Copy)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colors.statusTor.copy(alpha = 0.2f)),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(conn.circuitId))
                        copyConfirmed = true
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = colors.statusTor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CIRCUIT ID",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = conn.circuitId.take(10).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }
            }

            Icon(
                imageVector = if (copyConfirmed) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = "Copy Circuit ID",
                modifier = Modifier.size(14.dp),
                tint = if (copyConfirmed) Color(0xFF10B981) else colors.textMuted
            )
        }
    }

    LaunchedEffect(copyConfirmed) {
        if (copyConfirmed) {
            delay(2000)
            copyConfirmed = false
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

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
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color(0xFF8B5CF6),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.6f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .size(19.dp)
                .rotate(if (isRotating) rotationAngle else 0f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isRotating) "Building New Onion Circuit..." else "New Tor Circuit for this Site",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            letterSpacing = 0.15.sp
        )
    }
}

/**
 * Individual Node Row in the vertical stepper.
 * Clean, icon-driven, with no text emojis.
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
                    .border(0.75.dp, iconTint.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Stepper Connecting Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(26.dp)
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

        Spacer(modifier = Modifier.width(14.dp))

        // Right: Node Metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, bottom = if (isLast) 0.dp else 14.dp)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = { conn.progress / 100f },
            color = Color(0xFF8B5CF6),
            trackColor = colors.borderSubtle,
            modifier = Modifier.size(52.dp),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = conn.statusText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Establishing 3-Hop Encrypted Onion Circuit (${conn.progress}%)",
            fontSize = 12.sp,
            color = colors.textMuted
        )
    }
}

/**
 * Inactive state when Tor browsing is not currently active.
 */
@Composable
private fun InactiveCircuitContent() {
    val colors = AzokleTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.borderSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.VpnLock,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tor Circuit Inactive",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Open a Tor Tab or toggle onion mode to establish a leakproof encrypted circuit.",
            fontSize = 12.sp,
            color = colors.textMuted,
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
