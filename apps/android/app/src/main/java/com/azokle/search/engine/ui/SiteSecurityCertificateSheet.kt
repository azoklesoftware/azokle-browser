package com.azokle.search.engine.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.adblock.AdBlockEngine
import com.azokle.search.adblock.AdBlockStats
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleThemeColors
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.engine.GeckoEngineManager
import com.azokle.search.passkey.PasskeyVaultManager
import com.azokle.search.passkey.PasskeyVaultSheet
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Ultra-Minimalist Site Security & SSL Certificate Inspector Sheet.
 * Designed with clean typography, compact flat cards, zero text clutter, and fast tactile controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSecurityCertificateSheet(
    url: String,
    securityInfo: SecurityInformation?,
    onDismiss: () -> Unit,
    onReload: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uri = try { Uri.parse(url) } catch (e: Exception) { null }
    val host = uri?.host?.takeIf { it.isNotBlank() } ?: url
    val cleanHost = remember(host) { host.removePrefix("www.") }
    val isTor = remember(url) { url.contains(".onion", ignoreCase = true) }
    val isHttps = url.startsWith("https://", ignoreCase = true)
    val isSecure = securityInfo?.isSecure == true || isTor

    var isShieldEnabled by remember {
        mutableStateOf(AdBlockEngine.instance.isShieldEnabled(host))
    }

    var showTechDetails by remember { mutableStateOf(false) }
    var showPasskeyVault by remember { mutableStateOf(false) }
    var showSitePermissionsDialog by remember { mutableStateOf(false) }

    val passkeyCount = remember(host) {
        PasskeyVaultManager.getInstance(context).getPasskeysForDomain(host).size
    }

    val permManager = remember { SitePermissionsManager.getInstance(context) }
    val permUpdateTick by permManager.updatesFlow.collectAsState()
    val sitePerms = remember(host, permUpdateTick) { permManager.getHostPermissions(host) }

    val cert = securityInfo?.certificate as? X509Certificate
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Parse X.509 Certificate Metadata
    val issuedToCn: String
    val issuedByCn: String
    val issuedByOrg: String
    val validTo: String
    val sigAlg: String
    val keyAlg: String

    if (cert != null) {
        val subjectName = cert.subjectX500Principal.name
        val issuerName = cert.issuerX500Principal.name

        issuedToCn = parseDnField(subjectName, "CN") ?: cleanHost
        issuedByCn = parseDnField(issuerName, "CN") ?: parseDnField(issuerName, "OU") ?: "Certificate Authority"
        issuedByOrg = parseDnField(issuerName, "O") ?: "Certificate Authority"

        validTo = try { dateFormat.format(cert.notAfter) } catch (e: Exception) { "Valid" }
        sigAlg = cert.sigAlgName ?: "TLS 1.3 / ECC"
        keyAlg = "${cert.publicKey?.algorithm ?: "ECC"} (${cert.publicKey?.encoded?.size?.times(8) ?: 256}-bit)"
    } else {
        issuedToCn = cleanHost
        issuedByCn = if (isTor) "Tor Onion Circuit" else if (isSecure) "TLS Trust Store" else "None"
        issuedByOrg = if (isTor) "Tor Network" else if (isSecure) "Root Trust" else "None"
        validTo = if (isSecure) "Active Session" else "N/A"
        sigAlg = if (isTor) "ed25519 (Tor v3)" else if (isSecure) "TLS 1.3" else "None (HTTP)"
        keyAlg = if (isTor) "ed25519 (256-bit)" else if (isSecure) "ECC (256-bit)" else "N/A"
    }

    val displayIssuer = remember(issuedByOrg, issuedByCn, isTor, isSecure) {
        when {
            isTor -> "Tor v3 Network"
            issuedByOrg != "Certificate Authority" && issuedByOrg != "None" && issuedByOrg.isNotBlank() -> issuedByOrg
            issuedByCn.isNotBlank() && issuedByCn != "None" -> issuedByCn
            isSecure -> "Verified Issuer"
            else -> "Unencrypted"
        }
    }

    val sha256Fingerprint = remember(cert) {
        if (cert != null) {
            try {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(cert.encoded)
                digest.joinToString(":") { "%02X".format(it) }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (showTechDetails) 90f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "techChevronRotation"
    )

    val statusColor = when {
        isTor -> colors.statusTor
        isSecure -> colors.statusSuccess
        else -> colors.statusDestructive
    }

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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Site Security",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.semantics { heading() }
                    )

                    // Close Action
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                                onClick = onDismiss
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Close site security"
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

                // 2. Minimalist Hero Card (Domain, Status Pill & Cipher Spec)
                val heroShape = AzokleRadius.lg
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(heroShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.borderSubtle, heroShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(AzokleRadius.md)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, AzokleRadius.md),
                        contentAlignment = Alignment.Center
                    ) {
                        FaviconImage(
                            url = url,
                            isTor = isTor,
                            size = 20.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cleanHost,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isTor -> "Tor Encrypted • v3 Onion"
                                    isSecure -> "Secure • $displayIssuer"
                                    else -> "Not Secure • HTTP"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Site Controls Card (Shield, Permissions, Passkeys, Clear Data)
                val controlsShape = AzokleRadius.lg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(controlsShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.borderSubtle, controlsShape)
                ) {
                    // Shield Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isShieldEnabled) colors.statusSuccess else colors.textMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Azokle Shield",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }

                        Switch(
                            checked = isShieldEnabled,
                            onCheckedChange = {
                                isShieldEnabled = it
                                AdBlockStats.toggleShield()
                                ToastManager.show(if (it) "Shield on" else "Shield off")
                                onReload()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.statusSuccess,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated,
                                uncheckedBorderColor = colors.borderSubtle
                            )
                        )
                    }

                    CertDivider(colors = colors)

                    // Site Permissions Row
                    val customPermCount = sitePerms.size
                    MinimalControlRow(
                        icon = Icons.Default.Tune,
                        title = "Permissions",
                        badge = if (customPermCount > 0) "$customPermCount" else null,
                        colors = colors,
                        onClick = { showSitePermissionsDialog = true }
                    )

                    CertDivider(colors = colors)

                    // Passkeys Row
                    MinimalControlRow(
                        icon = Icons.Default.VpnKey,
                        title = "Passkeys",
                        badge = if (passkeyCount > 0) "$passkeyCount" else null,
                        colors = colors,
                        onClick = { showPasskeyVault = true }
                    )

                    CertDivider(colors = colors)

                    // Clear Site Data Row
                    MinimalControlRow(
                        icon = Icons.Default.DeleteOutline,
                        title = "Clear Cookies & Site Data",
                        titleColor = colors.statusDestructive,
                        iconTint = colors.statusDestructive,
                        showChevron = false,
                        colors = colors,
                        onClick = {
                            GeckoEngineManager.clearHostData(context, host)
                            GeckoEngineManager.clearStorageForHost(host)
                            ToastManager.showSuccess("Site data cleared")
                            onReload()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Certificate & Encryption Details (Accordion)
                val certShape = AzokleRadius.lg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(certShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.borderSubtle, certShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                                onClick = { showTechDetails = !showTechDetails }
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Certificate Details",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (showTechDetails) "Collapse" else "Expand",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(chevronRotation)
                        )
                    }

                    AnimatedVisibility(
                        visible = showTechDetails,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceElevated.copy(alpha = 0.35f))
                        ) {
                            CertDivider(colors = colors)
                            CertRow(label = "Issued To", value = issuedToCn, colors = colors)
                            CertDivider(colors = colors)
                            CertRow(label = "Issued By", value = displayIssuer, colors = colors)
                            CertDivider(colors = colors)
                            CertRow(label = "Valid Until", value = validTo, colors = colors)
                            CertDivider(colors = colors)
                            CertRow(label = "Cipher", value = sigAlg, colors = colors)
                            CertDivider(colors = colors)
                            CertRow(label = "Public Key", value = keyAlg, colors = colors)

                            if (sha256Fingerprint != null) {
                                CertDivider(colors = colors)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SHA-256 Fingerprint",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        SelectionContainer {
                                            Text(
                                                text = sha256Fingerprint,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = colors.textSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceElevated)
                                            .border(0.8.dp, colors.borderSubtle, CircleShape)
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(sha256Fingerprint))
                                                ToastManager.showCopy("Fingerprint copied")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Fingerprint",
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Passkey Vault
    if (showPasskeyVault) {
        PasskeyVaultSheet(onDismiss = { showPasskeyVault = false })
    }

    // Dialog: Site Permissions Quick Editor
    if (showSitePermissionsDialog) {
        SitePermissionsEditorDialog(
            host = host,
            permManager = permManager,
            colors = colors,
            onDismiss = { showSitePermissionsDialog = false }
        )
    }
}

// ---------------- Helper Components & Composables ----------------

@Composable
private fun MinimalControlRow(
    icon: ImageVector,
    title: String,
    titleColor: Color? = null,
    iconTint: Color? = null,
    badge: String? = null,
    showChevron: Boolean = true,
    colors: AzokleThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = (titleColor ?: colors.textPrimary).copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics {
                role = Role.Button
                contentDescription = title
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: colors.textSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor ?: colors.textPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CertDivider(colors: AzokleThemeColors) {
    HorizontalDivider(
        color = colors.borderSubtle,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun CertRow(
    label: String,
    value: String,
    colors: AzokleThemeColors,
    valueColor: Color = colors.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textMuted
        )
        Spacer(modifier = Modifier.width(12.dp))
        SelectionContainer {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SitePermissionsEditorDialog(
    host: String,
    permManager: SitePermissionsManager,
    colors: AzokleThemeColors,
    onDismiss: () -> Unit
) {
    val updateTick by permManager.updatesFlow.collectAsState()
    val sitePerms = remember(host, updateTick) { permManager.getHostPermissions(host) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Site Permissions",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = host,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    SitePermissionType.CAMERA,
                    SitePermissionType.MICROPHONE,
                    SitePermissionType.GEOLOCATION,
                    SitePermissionType.NOTIFICATIONS,
                    SitePermissionType.AUTOPLAY
                ).forEach { type ->
                    val state = sitePerms[type] ?: permManager.getGlobalDefault(type)
                    val isAllow = state == SitePermissionState.ALLOW
                    val isBlock = state == SitePermissionState.BLOCK

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = type.icon,
                                contentDescription = null,
                                tint = when {
                                    isAllow -> colors.statusSuccess
                                    isBlock -> colors.statusDestructive
                                    else -> colors.textPrimary
                                },
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type.displayName,
                                fontSize = 12.5.sp,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Allow Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAllow) colors.statusSuccess.copy(alpha = 0.20f) else colors.surfaceElevated)
                                    .border(
                                        0.8.dp,
                                        if (isAllow) colors.statusSuccess.copy(alpha = 0.5f) else colors.borderSubtle,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        permManager.setPermission(host, type, SitePermissionState.ALLOW)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Allow",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllow) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllow) colors.statusSuccess else colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            // Block Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isBlock) colors.statusDestructive.copy(alpha = 0.20f) else colors.surfaceElevated)
                                    .border(
                                        0.8.dp,
                                        if (isBlock) colors.statusDestructive.copy(alpha = 0.5f) else colors.borderSubtle,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        permManager.setPermission(host, type, SitePermissionState.BLOCK)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Block",
                                    fontSize = 11.sp,
                                    fontWeight = if (isBlock) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isBlock) colors.statusDestructive else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = colors.statusAccent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    permManager.clearHostPermissions(host)
                    ToastManager.show("Permissions reset")
                    onDismiss()
                }
            ) {
                Text("Reset", color = colors.textMuted)
            }
        },
        containerColor = colors.surface,
        shape = AzokleRadius.xl
    )
}

private fun parseDnField(dn: String, field: String): String? {
    val prefix = "$field="
    return dn.split(",").firstOrNull { it.trim().startsWith(prefix, ignoreCase = true) }
        ?.substringAfter("=")?.trim()?.removeSurrounding("\"")
}
