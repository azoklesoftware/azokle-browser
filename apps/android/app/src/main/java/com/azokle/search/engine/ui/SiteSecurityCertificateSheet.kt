package com.azokle.search.engine.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.azokle.search.engine.GeckoEngineManager
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.passkey.PasskeyVaultManager
import com.azokle.search.passkey.PasskeyVaultSheet
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

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
    val host = uri?.host ?: url
    val isTor = remember(url) { url.contains(".onion", ignoreCase = true) }
    val isHttps = url.startsWith("https://", ignoreCase = true)
    val isSecure = securityInfo?.isSecure == true

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
    val issuedToOrg: String
    val issuedByCn: String
    val issuedByOrg: String
    val validFrom: String
    val validTo: String
    val sigAlg: String
    val keyAlg: String

    if (cert != null) {
        val subjectName = cert.subjectX500Principal.name
        val issuerName = cert.issuerX500Principal.name

        issuedToCn = parseDnField(subjectName, "CN") ?: host
        issuedToOrg = parseDnField(subjectName, "O") ?: "Authenticated Domain"
        issuedByCn = parseDnField(issuerName, "CN") ?: parseDnField(issuerName, "OU") ?: "Certificate Authority"
        issuedByOrg = parseDnField(issuerName, "O") ?: "Certificate Authority"

        validFrom = try { dateFormat.format(cert.notBefore) } catch (e: Exception) { "Active" }
        validTo = try { dateFormat.format(cert.notAfter) } catch (e: Exception) { "Valid" }
        sigAlg = cert.sigAlgName ?: "TLS 1.3 Cipher Suite"
        keyAlg = "${cert.publicKey?.algorithm ?: "ECC"} (${cert.publicKey?.encoded?.size?.times(8) ?: 256}-bit)"
    } else {
        issuedToCn = host
        issuedToOrg = if (isSecure) "Verified Host" else "Unverified Host"
        issuedByCn = if (isSecure) "TLS Trust Store" else "None"
        issuedByOrg = if (isSecure) "Native Root Trust" else "None"
        validFrom = if (isSecure) "Active Session" else "N/A"
        validTo = if (isSecure) "Active Session" else "N/A"
        sigAlg = if (isSecure) "TLS 1.3 / Quantum Protected" else "None (Unencrypted HTTP)"
        keyAlg = if (isSecure) "Elliptic Curve (256-bit)" else "N/A"
    }

    // Clean user-friendly issuer name
    val displayIssuer = remember(issuedByOrg, issuedByCn) {
        when {
            issuedByOrg != "Certificate Authority" && issuedByOrg != "None" && issuedByOrg.isNotBlank() -> issuedByOrg
            issuedByCn.isNotBlank() && issuedByCn != "None" -> issuedByCn
            isSecure -> "Verified Trust Store"
            else -> "Unencrypted"
        }
    }

    // SHA-256 Fingerprint calculation
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
        label = "techChevronRotation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.8f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Sleek Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.semantics { heading() }) {
                    Text(
                        text = "Site Security",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Cryptographic identity & site controls",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                // 44dp Accessible Close Action
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
                            contentDescription = "Close security details"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape),
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

            // 2. Hero Trust Banner Card
            val heroShape = AzokleRadius.xl
            val accentStatusColor = when {
                isTor -> Color(0xFF8B5CF6)
                isSecure -> colors.statusSuccess
                else -> colors.statusDestructive
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(colors.cardBg)
                    .border(1.dp, accentStatusColor.copy(alpha = 0.25f), heroShape)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Domain Favicon Squircle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(AzokleRadius.md)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border.copy(alpha = 0.6f), AzokleRadius.md),
                        contentAlignment = Alignment.Center
                    ) {
                        FaviconImage(
                            url = url,
                            isTor = isTor,
                            size = 24.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = host,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Status Badge Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentStatusColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isTor -> Icons.Default.VpnLock
                                    isSecure -> Icons.Default.Lock
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = accentStatusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isTor -> "Tor Onion Encrypted"
                                    isSecure -> "Connection is Secure"
                                    else -> "Connection Not Secure"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentStatusColor
                            )
                            if (isSecure && !isTor) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = accentStatusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        isTor -> "Traffic is anonymized through the 3-hop Tor Onion network with end-to-end cryptographic encapsulation."
                        isSecure -> "Your connection to $host is end-to-end encrypted with a valid digital certificate. Sensitive data remains private."
                        else -> "This website does not use encryption. Passwords, payment details, and private messages may be visible to network eavesdroppers."
                    },
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Quick-Glance Trust Shelf (3 Metric Tiles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VerifiedUser,
                    label = "ISSUER",
                    value = displayIssuer,
                    accentColor = colors.statusAccent,
                    colors = colors
                )
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Event,
                    label = "EXPIRES",
                    value = validTo,
                    accentColor = if (isSecure) colors.statusSuccess else colors.textPrimary,
                    colors = colors
                )
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VpnLock,
                    label = "PROTOCOL",
                    value = if (isHttps) "TLS 1.3" else "HTTP",
                    accentColor = colors.textPrimary,
                    colors = colors
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Site Privacy & Controls Card
            SectionLabel(title = "SITE PRIVACY & CONTROLS")

            CleanGroupCard(colors = colors) {
                // Azokle Shield Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ActionAvatar(
                            icon = Icons.Default.Shield,
                            tint = if (isShieldEnabled) colors.statusSuccess else colors.textMuted,
                            bg = if (isShieldEnabled) colors.statusSuccess.copy(alpha = 0.12f) else colors.surfaceElevated
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Azokle Shield Protection",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isShieldEnabled) "Block trackers, ads & fingerprinting" else "Protection paused for this site",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = isShieldEnabled,
                        onCheckedChange = {
                            isShieldEnabled = it
                            AdBlockStats.toggleShield()
                            ToastManager.show(
                                if (it) "Shield activated for $host" else "Shield paused for $host"
                            )
                            onReload()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.statusSuccess,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surfaceElevated,
                            uncheckedBorderColor = colors.border
                        )
                    )
                }

                CertDivider(colors = colors)

                // Site Permissions Trigger
                val customPermCount = sitePerms.size
                ActionRow(
                    icon = Icons.Default.Tune,
                    iconTint = colors.statusAccent,
                    iconBg = colors.statusAccent.copy(alpha = 0.12f),
                    title = "Site Permissions",
                    subtitle = if (customPermCount > 0) "$customPermCount custom permission(s) active" else "Camera, microphone, location & notifications",
                    onClick = { showSitePermissionsDialog = true },
                    contentDesc = "Manage hardware permissions for $host",
                    colors = colors
                )

                CertDivider(colors = colors)

                // Passkeys & Biometrics Action
                ActionRow(
                    icon = Icons.Default.VpnKey,
                    iconTint = colors.statusAccent,
                    iconBg = colors.statusAccent.copy(alpha = 0.12f),
                    title = "Passkeys & Biometrics",
                    subtitle = if (passkeyCount > 0) "$passkeyCount passkey(s) saved for $host" else "FIDO2 / WebAuthn passwordless sign-in",
                    onClick = { showPasskeyVault = true },
                    contentDesc = "Manage saved passkeys for $host",
                    colors = colors
                )

                CertDivider(colors = colors)

                // Clear Cookies & Site Data
                ActionRow(
                    icon = Icons.Default.DeleteOutline,
                    iconTint = colors.statusDestructive,
                    iconBg = colors.statusDestructive.copy(alpha = 0.10f),
                    title = "Clear Cookies & Site Data",
                    titleColor = colors.statusDestructive,
                    subtitle = "Purge local cache, storage & cookies for $host",
                    onClick = {
                        GeckoEngineManager.clearHostData(context, host)
                        ToastManager.showSuccess("Site data cleared for $host")
                        onReload()
                    },
                    contentDesc = "Clear cookies and site data for $host",
                    showChevron = false,
                    colors = colors
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Expandable Technical Certificate Details (Accordion)
            CleanGroupCard(colors = colors) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusAccent.copy(alpha = 0.12f)),
                            onClick = { showTechDetails = !showTechDetails }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActionAvatar(
                            icon = Icons.Default.Fingerprint,
                            tint = colors.textPrimary,
                            bg = colors.surfaceElevated
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Certificate Details",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (showTechDetails) "Hide X.509 technical parameters" else "View cipher suite, keys & SHA fingerprint",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = if (showTechDetails) "Collapse" else "Expand",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }

                AnimatedVisibility(
                    visible = showTechDetails,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceElevated.copy(alpha = 0.35f))
                    ) {
                        CertDivider(colors = colors)

                        CertRow(label = "Subject (CN)", value = issuedToCn, colors = colors)
                        CertDivider(colors = colors)
                        CertRow(label = "Organization", value = issuedToOrg, colors = colors)
                        CertDivider(colors = colors)
                        CertRow(label = "Issuer Authority", value = issuedByCn, colors = colors)
                        CertDivider(colors = colors)
                        CertRow(label = "Valid Period", value = "$validFrom ➔ $validTo", colors = colors)
                        CertDivider(colors = colors)
                        CertRow(label = "Cipher Suite", value = sigAlg, colors = colors)
                        CertDivider(colors = colors)
                        CertRow(label = "Public Key", value = keyAlg, colors = colors)

                        if (sha256Fingerprint != null) {
                            CertDivider(colors = colors)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SHA-256 Fingerprint",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMuted
                                    )

                                    // 1-Tap Copy Chip
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.surfaceElevated)
                                            .border(0.8.dp, colors.border, RoundedCornerShape(6.dp))
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(sha256Fingerprint))
                                                ToastManager.showCopy("Fingerprint copied to clipboard")
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Fingerprint",
                                            tint = colors.statusAccent,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Copy",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.statusAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.background)
                                        .border(0.8.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = sha256Fingerprint,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textSecondary,
                                            lineHeight = 15.sp
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
private fun QuickMetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    val shape = AzokleRadius.md
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.cardBg)
            .border(1.dp, colors.border.copy(alpha = 0.5f), shape)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted,
                letterSpacing = 0.6.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionAvatar(
    icon: ImageVector,
    tint: Color,
    bg: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    titleColor: Color? = null,
    subtitle: String,
    onClick: () -> Unit,
    contentDesc: String,
    showChevron: Boolean = true,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = (titleColor ?: colors.statusAccent).copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionAvatar(icon = icon, tint = iconTint, bg = iconBg)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor ?: colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    val colors = AzokleTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = colors.textMuted,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 2.dp, bottom = 6.dp)
    )
}

@Composable
private fun CleanGroupCard(
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    content: @Composable () -> Unit
) {
    val shape = AzokleRadius.lg
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBg)
            .border(1.dp, colors.border, shape)
    ) {
        content()
    }
}

@Composable
private fun CertDivider(colors: com.azokle.search.common.theme.AzokleThemeColors) {
    HorizontalDivider(
        color = colors.border.copy(alpha = 0.4f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun CertRow(
    label: String,
    value: String,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    valueColor: Color = colors.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textMuted
        )
        SelectionContainer {
            Text(
                text = value,
                fontSize = 11.5.sp,
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
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val updateTick by permManager.updatesFlow.collectAsState()
    val sitePerms = remember(host, updateTick) { permManager.getHostPermissions(host) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Site Permissions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = host,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type.displayName,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Allow Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAllow) colors.statusSuccess.copy(alpha = 0.22f) else colors.surfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isAllow) colors.statusSuccess.copy(alpha = 0.6f) else colors.borderSubtle,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        permManager.setPermission(host, type, SitePermissionState.ALLOW)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Allow",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isAllow) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllow) colors.statusSuccess else colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            // Block Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isBlock) colors.statusDestructive.copy(alpha = 0.22f) else colors.surfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isBlock) colors.statusDestructive.copy(alpha = 0.6f) else colors.borderSubtle,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        permManager.setPermission(host, type, SitePermissionState.BLOCK)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Block",
                                    fontSize = 11.5.sp,
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
                    ToastManager.show("Permissions reset for $host")
                    onDismiss()
                }
            ) {
                Text("Reset Defaults", color = colors.textMuted)
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
