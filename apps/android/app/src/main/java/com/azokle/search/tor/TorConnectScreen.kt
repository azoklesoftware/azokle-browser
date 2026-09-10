package com.azokle.search.tor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.tor.TorCircuitManager
import com.azokle.search.tor.TorConnectionState
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth

@Composable
fun TorConnectScreen(
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = AzokleTheme.colors
    val connectionState by TorCircuitManager.connectionState.collectAsState()
    val focusManager = LocalFocusManager.current
    val preferences = remember { com.azokle.search.common.util.PreferencesManager(context) }
    val searchEngineConfig = remember(preferences) {
        val engineId = preferences.getSearchEngine()
        com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS.find { it.id == engineId }
            ?: com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS.find { it.id == "duckduckgo" }
            ?: com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS[0]
    }
    var searchQuery by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isDark = colors.isDark
    val pageBg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF0D0D0D) else Color(0xFFFAFAFA)
    val elevatedBg = if (isDark) Color(0xFF141414) else Color(0xFFF4F4F5)
    val borderCol = if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)
    val borderSubtle = if (isDark) Color(0xFF1A1A1A) else Color(0xFFECECEE)

    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val secondaryTextColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val mutedTextColor = if (isDark) Color(0xFF71717A) else Color(0xFF71717A)
    val primaryIconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    val btnPrimaryBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val btnPrimaryText = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)

    val btnSecondaryBg = if (isDark) Color(0xFF141414) else Color(0xFFF4F4F5)
    val btnSecondaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val btnSecondaryBorder = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE4E4E7)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(600.dp)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(AzokleRadius.full)
                        .background(elevatedBg)
                        .border(1.dp, borderCol, AzokleRadius.full)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = primaryIconColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TOR ONION ROUTING",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = primaryTextColor,
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
                            indication = ripple(bounded = false, radius = 18.dp, color = primaryTextColor.copy(alpha = 0.15f)),
                            onClick = onClose
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close Tor window"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(elevatedBg)
                            .border(1.dp, borderCol, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = primaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hero Emblem (Zero Gradient, Solid Container)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(if (connectionState is TorConnectionState.Starting || connectionState is TorConnectionState.Bootstrapping) pulseScale else 1.0f)
                    .clip(AzokleRadius.xl)
                    .background(elevatedBg)
                    .border(1.dp, borderCol, AzokleRadius.xl),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tor),
                    contentDescription = "Tor Emblem",
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tor Private Window",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Encrypted 3-hop onion circuit • No trace browsing",
                fontSize = 12.5.sp,
                color = secondaryTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Status Section
            when (val state = connectionState) {
                is TorConnectionState.Starting -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(cardBg)
                            .border(1.dp, borderCol, AzokleRadius.lg)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = primaryTextColor,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Initializing native daemon and proxy bridge",
                            fontSize = 11.sp,
                            color = mutedTextColor
                        )
                    }
                }

                is TorConnectionState.Bootstrapping -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(cardBg)
                            .border(1.dp, borderCol, AzokleRadius.lg)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = primaryTextColor,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = primaryTextColor,
                            trackColor = elevatedBg
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.progress}% bootstrapped",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = mutedTextColor
                        )
                    }
                }

                is TorConnectionState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.lg)
                            .background(cardBg)
                            .border(1.dp, borderCol, AzokleRadius.lg)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = primaryIconColor,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connection Failed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(AzokleRadius.md)
                                .background(btnPrimaryBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = btnPrimaryText.copy(alpha = 0.2f)),
                                    onClick = { TorCircuitManager.startTorConnection() }
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Retry Tor connection"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Retry Connection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = btnPrimaryText)
                        }
                    }
                }

                is TorConnectionState.Connected -> {
                    // Circuit Diagnostics Card
                    val circuitShape = AzokleRadius.lg
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(circuitShape)
                            .background(cardBg)
                            .border(1.dp, borderCol, circuitShape)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CIRCUIT ESTABLISHED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = primaryTextColor,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(AzokleRadius.sm)
                                    .background(elevatedBg)
                                    .border(1.dp, borderCol, AzokleRadius.sm)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = primaryTextColor.copy(alpha = 0.15f)),
                                        onClick = { TorCircuitManager.requestNewCircuit() }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Request new Tor circuit"
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New Circuit",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(
                            color = borderSubtle,
                            thickness = 0.5.dp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CleanCircuitRow(
                            icon = Icons.Default.Smartphone,
                            role = "Device",
                            detail = "Local Device",
                            textColor = primaryTextColor,
                            mutedColor = secondaryTextColor,
                            bg = elevatedBg,
                            border = borderCol
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CleanCircuitRow(
                            icon = Icons.Default.Shield,
                            role = "Guard Node",
                            detail = state.guardCountry,
                            textColor = primaryTextColor,
                            mutedColor = secondaryTextColor,
                            bg = elevatedBg,
                            border = borderCol
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CleanCircuitRow(
                            icon = Icons.Default.Sync,
                            role = "Relay Node",
                            detail = state.middleCountry,
                            textColor = primaryTextColor,
                            mutedColor = secondaryTextColor,
                            bg = elevatedBg,
                            border = borderCol
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CleanCircuitRow(
                            icon = Icons.Default.VpnLock,
                            role = "Exit Node",
                            detail = "${state.exitCountry} (${state.exitIp})",
                            textColor = primaryTextColor,
                            mutedColor = secondaryTextColor,
                            bg = elevatedBg,
                            border = borderCol
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search ${searchEngineConfig.name} or enter .onion URL...", color = mutedTextColor, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = primaryTextColor, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(AzokleRadius.sm)
                                        .background(btnPrimaryBg)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = btnPrimaryText.copy(alpha = 0.2f)),
                                            onClick = {
                                                val target = TorCircuitManager.resolveTorUrl(searchQuery, searchEngineConfig.queryUrl)
                                                onNavigate(target)
                                            }
                                        )
                                        .padding(6.dp)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = "Navigate to entered URL"
                                        }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = btnPrimaryText, modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.isNotBlank()) {
                                focusManager.clearFocus()
                                val target = TorCircuitManager.resolveTorUrl(searchQuery, searchEngineConfig.queryUrl)
                                onNavigate(target)
                            }
                        }),
                        shape = AzokleRadius.md,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = elevatedBg,
                            unfocusedContainerColor = elevatedBg,
                            focusedBorderColor = primaryTextColor,
                            unfocusedBorderColor = borderCol,
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2x2 Directory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CleanShortcutCard(
                            title = "DuckDuckGo",
                            desc = "Private Search",
                            icon = Icons.Default.Search,
                            primaryTextColor = primaryTextColor,
                            mutedTextColor = mutedTextColor,
                            cardBg = cardBg,
                            elevatedBg = elevatedBg,
                            borderCol = borderCol,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("https://duckduckgo.com") }
                        )
                        CleanShortcutCard(
                            title = "Tor Project",
                            desc = "Check Status",
                            icon = Icons.Default.Security,
                            primaryTextColor = primaryTextColor,
                            mutedTextColor = mutedTextColor,
                            cardBg = cardBg,
                            elevatedBg = elevatedBg,
                            borderCol = borderCol,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("https://check.torproject.org") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CleanShortcutCard(
                            title = "ProPublica",
                            desc = "News Onion",
                            icon = Icons.AutoMirrored.Filled.Article,
                            primaryTextColor = primaryTextColor,
                            mutedTextColor = mutedTextColor,
                            cardBg = cardBg,
                            elevatedBg = elevatedBg,
                            borderCol = borderCol,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("https://www.propublica.org") }
                        )
                        CleanShortcutCard(
                            title = "Brave Search",
                            desc = "Private Index",
                            icon = Icons.Default.Language,
                            primaryTextColor = primaryTextColor,
                            mutedTextColor = mutedTextColor,
                            cardBg = cardBg,
                            elevatedBg = elevatedBg,
                            borderCol = borderCol,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("https://search.brave.com") }
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(AzokleRadius.md)
                            .background(btnPrimaryBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = btnPrimaryText.copy(alpha = 0.2f)),
                                onClick = {
                                    onNavigate("https://check.torproject.org")
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Start Tor browsing and check connection status"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = btnPrimaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Tor Browsing (Check Status)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = btnPrimaryText
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(AzokleRadius.md)
                            .background(btnPrimaryBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = btnPrimaryText.copy(alpha = 0.2f)),
                                onClick = { TorCircuitManager.startTorConnection() }
                            )
                            .padding(horizontal = 24.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Connect to Tor network"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connect to Tor",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = btnPrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun CleanCircuitRow(
    icon: ImageVector,
    role: String,
    detail: String,
    textColor: Color,
    mutedColor: Color,
    bg: Color,
    border: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AzokleRadius.sm)
            .background(bg)
            .border(0.5.dp, border, AzokleRadius.sm)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = role,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
        Text(
            text = detail,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = mutedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CleanShortcutCard(
    title: String,
    desc: String,
    icon: ImageVector,
    primaryTextColor: Color,
    mutedTextColor: Color,
    cardBg: Color,
    elevatedBg: Color,
    borderCol: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(AzokleRadius.md)
            .background(cardBg)
            .border(1.dp, borderCol, AzokleRadius.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = primaryTextColor.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(12.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title, $desc"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(elevatedBg)
                .border(0.5.dp, borderCol, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryTextColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
            Text(
                text = desc,
                fontSize = 10.5.sp,
                color = mutedTextColor
            )
        }
    }
}
