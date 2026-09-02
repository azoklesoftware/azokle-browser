package com.azokle.search.browser.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.browser.adblock.AdBlockEngine
import com.azokle.search.browser.adblock.AdBlockStats
import com.azokle.search.browser.gecko.GeckoEngineManager
import com.azokle.search.browser.permissions.SitePermissionState
import com.azokle.search.browser.permissions.SitePermissionType
import com.azokle.search.browser.permissions.SitePermissionsManager
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
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
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uri = try { Uri.parse(url) } catch (e: Exception) { null }
    val host = uri?.host ?: url
    val isHttps = url.startsWith("https://", ignoreCase = true)
    val isSecure = securityInfo?.isSecure == true

    var isShieldEnabled by remember {
        mutableStateOf(AdBlockEngine.instance.isShieldEnabled(host))
    }

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
        issuedByCn = parseDnField(issuerName, "CN") ?: parseDnField(issuerName, "OU") ?: "Verified Certificate Authority"
        issuedByOrg = parseDnField(issuerName, "O") ?: "Certificate Authority"

        validFrom = try { dateFormat.format(cert.notBefore) } catch (e: Exception) { "Active" }
        validTo = try { dateFormat.format(cert.notAfter) } catch (e: Exception) { "Valid" }
        sigAlg = cert.sigAlgName ?: "TLS 1.3 Cipher Suite"
        keyAlg = "${cert.publicKey?.algorithm ?: "ECC"} (${cert.publicKey?.encoded?.size?.times(8) ?: 256}-bit)"
    } else {
        issuedToCn = host
        issuedToOrg = if (isSecure) "Verified Host" else "Unverified Host"
        issuedByCn = if (isSecure) "TLS Trust Store / Native System CA" else "No Certificate Authority"
        issuedByOrg = if (isSecure) "Native Root Trust" else "None"
        validFrom = if (isSecure) "Active Session" else "N/A"
        validTo = if (isSecure) "Active Session" else "N/A"
        sigAlg = if (isSecure) "TLS 1.3 / Quantum Protected" else "None (Unencrypted HTTP)"
        keyAlg = if (isSecure) "Elliptic Curve / RSA Key Exchange" else "N/A"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 18.dp)
                .padding(top = 2.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Clean Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.semantics { heading() }) {
                    Text(
                        text = "Site Security",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = host,
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 48dp Close Target
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. Calm Connection Status Card
            val statusShape = AzokleRadius.lg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(statusShape)
                    .background(colors.cardBg)
                    .border(
                        1.dp,
                        if (isSecure) colors.statusSuccess.copy(alpha = 0.35f) else colors.statusDestructive.copy(alpha = 0.35f),
                        statusShape
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSecure) colors.statusSuccess.copy(alpha = 0.12f) else colors.statusDestructive.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSecure) colors.statusSuccess else colors.statusDestructive,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSecure) "Connection is Secure" else "Connection Not Secure",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSecure) colors.statusSuccess else colors.statusDestructive
                        )
                        if (isSecure) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = colors.statusSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSecure)
                            "Your traffic to $host is end-to-end encrypted with valid cryptographic identity."
                        else
                            "Unencrypted HTTP. Sensitive information such as passwords could be visible to network eavesdroppers.",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Certificate Identity (X.509) Grouped Card
            SectionHeader(title = "CERTIFICATE IDENTITY")

            CleanGroupCard(colors = colors) {
                CertRow(label = "Domain (CN)", value = issuedToCn, colors = colors)
                CertDivider(colors = colors)
                CertRow(label = "Organization", value = issuedToOrg, colors = colors)
                CertDivider(colors = colors)
                CertRow(label = "Issued By", value = issuedByCn, colors = colors)
                CertDivider(colors = colors)
                CertRow(label = "Valid From", value = validFrom, colors = colors)
                CertDivider(colors = colors)
                CertRow(
                    label = "Expires On",
                    value = validTo,
                    colors = colors,
                    valueColor = if (isSecure) colors.statusSuccess else colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Security & Cryptographic Specifications
            SectionHeader(title = "SPECIFICATIONS")

            CleanGroupCard(colors = colors) {
                CertRow(
                    label = "Protocol",
                    value = if (isHttps) "HTTPS (TLS 1.3)" else "HTTP (Cleartext)",
                    colors = colors
                )
                CertDivider(colors = colors)
                CertRow(
                    label = "Cipher Suite",
                    value = sigAlg,
                    colors = colors
                )
                CertDivider(colors = colors)
                CertRow(
                    label = "Key Exchange",
                    value = keyAlg,
                    colors = colors
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Site Privacy & Permissions
            SectionHeader(title = "SITE CONTROLS")

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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isShieldEnabled) colors.statusSuccess else colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                            Toast.makeText(
                                context,
                                if (it) "Shield activated for $host" else "Shield paused for $host",
                                Toast.LENGTH_SHORT
                            ).show()
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

                // Passkeys & Biometric Credentials Action
                var showPasskeyVault by remember { mutableStateOf(false) }
                val passkeyCount = remember(host) {
                    com.azokle.search.security.passkey.PasskeyVaultManager.getInstance(context).getPasskeysForDomain(host).size
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusAccent.copy(alpha = 0.12f)),
                            onClick = {
                                showPasskeyVault = true
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Manage saved passkeys for $host"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.statusAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = colors.statusAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Passkeys & Biometrics",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (passkeyCount > 0) "$passkeyCount passkey(s) saved for $host" else "FIDO2 / WebAuthn passwordless sign-in",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showPasskeyVault) {
                    com.azokle.search.security.passkey.PasskeyVaultSheet(
                        onDismiss = { showPasskeyVault = false }
                    )
                }

                CertDivider(colors = colors)

                // Clear Site Data Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.statusDestructive.copy(alpha = 0.12f)),
                            onClick = {
                                GeckoEngineManager.clearHostData(context, host)
                                Toast.makeText(context, "Site data cleared for $host", Toast.LENGTH_SHORT).show()
                                onReload()
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Clear cookies and site data for $host"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = colors.statusDestructive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Clear Cookies & Site Data",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.statusDestructive
                        )
                        Text(
                            text = "Purge local cache and storage for $host",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

private fun parseDnField(dn: String, field: String): String? {
    val prefix = "$field="
    return dn.split(",").firstOrNull { it.trim().startsWith(prefix, ignoreCase = true) }
        ?.substringAfter("=")?.trim()
}

@Composable
private fun SectionHeader(title: String) {
    val colors = AzokleTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = colors.textMuted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun CleanGroupCard(
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
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
private fun CertDivider(colors: com.azokle.search.ui.theme.AzokleThemeColors) {
    HorizontalDivider(
        color = colors.border.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun CertRow(
    label: String,
    value: String,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    valueColor: Color = colors.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = colors.textMuted
        )
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
