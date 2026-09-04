package com.azokle.search.browser.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.security.TorCircuitManager
import com.azokle.search.security.TorConnectionState
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

/**
 * Sovereign Tor Circuit Visualizer Modal.
 * Renders the live 3-hop onion circuit (Guard -> Middle -> Exit Relay)
 * with node latency, exit IP, and one-tap circuit rotation.
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .adaptiveContentWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.statusTor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tor),
                            contentDescription = "Tor Onion Circuit",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tor Onion Circuit",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "3-Hop Cryptographic Tunneling",
                            fontSize = 12.sp,
                            color = colors.statusTor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (val conn = state) {
                is TorConnectionState.Connected -> {
                    // Circuit Nodes Timeline
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, AzokleRadius.lg)
                            .padding(16.dp)
                    ) {
                        CircuitNodeRow(
                            step = "1",
                            title = "This Browser",
                            subtitle = "Local SOCKS5 (127.0.0.1:9050)",
                            icon = "📱",
                            isLast = false
                        )
                        CircuitNodeRow(
                            step = "2",
                            title = "Guard Node (Entry)",
                            subtitle = "${conn.guardNode} • ${conn.guardCountry}",
                            icon = "🛡️",
                            isLast = false
                        )
                        CircuitNodeRow(
                            step = "3",
                            title = "Middle Relay",
                            subtitle = "${conn.middleNode} • ${conn.middleCountry}",
                            icon = "🔄",
                            isLast = false
                        )
                        CircuitNodeRow(
                            step = "4",
                            title = "Exit Node",
                            subtitle = "${conn.exitNode} • ${conn.exitCountry} (IP: ${conn.exitIp})",
                            icon = "🌐",
                            isLast = false
                        )
                        CircuitNodeRow(
                            step = "5",
                            title = "Destination",
                            subtitle = url.take(40).ifBlank { "Encrypted Webpage" },
                            icon = "🎯",
                            isLast = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Latency & Circuit ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Latency: ~${conn.latencyMs} ms",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        Text(
                            text = "Circuit: ${conn.circuitId.take(12)}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Rotate Circuit Button
                    ElevatedButton(
                        onClick = { TorCircuitManager.requestNewCircuit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = colors.statusTor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Tor Circuit for this Site",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                is TorConnectionState.Bootstrapping -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = colors.statusTor,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = conn.statusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Progress: ${conn.progress}%",
                            fontSize = 12.sp,
                            color = colors.statusTor
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tor Circuit Inactive",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start Tor browsing to build an isolated onion circuit.",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircuitNodeRow(
    step: String,
    title: String,
    subtitle: String,
    icon: String,
    isLast: Boolean
) {
    val colors = AzokleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.cardBg)
                .border(0.5.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted,
                maxLines = 1
            )
        }
    }
    if (!isLast) {
        Box(
            modifier = Modifier
                .padding(start = 13.dp)
                .width(2.dp)
                .height(10.dp)
                .background(colors.border)
        )
    }
}
