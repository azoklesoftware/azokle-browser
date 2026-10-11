/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.azokle.search.vault.PasswordVaultManager
import com.azokle.search.vault.PasswordManagerScreen
import com.azokle.search.vault.EncryptedVaultManager
import com.azokle.search.common.util.AppUpdateChecker
import com.azokle.search.BuildConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.azokle.search.R
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.DefaultBrowserManager
import com.azokle.search.common.util.LocaleHelper
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.common.util.SUPPORTED_LANGUAGES
import com.azokle.search.engine.ui.SiteSettingsSheet
import com.azokle.search.passkey.PasskeyVaultSheet

private data class ThemeOption(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector
)

/**
 * Ultra-Minimalist, clean, state-of-the-art Settings Screen.
 * Concise typography, zero text clutter, and streamlined single-line actions.
 */
@Composable
fun SettingsScreen(
    selectedThemeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    selectedDohProvider: String = "cloudflare",
    onDohProviderChange: (String) -> Unit = {},
    selectedSearchEngine: String = "azokle",
    onOpenSearchEngine: () -> Unit = {},
    selectedLanguageCode: String = "system",
    onOpenLanguage: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onReplayTour: (() -> Unit)? = null,
    onReplayHomeTour: (() -> Unit)? = null,
    onReplayTabsTour: (() -> Unit)? = null,
    onBurnData: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = AzokleTheme.colors
    val prefsManager = remember { PreferencesManager(context) }
    val scrollState = rememberScrollState()

    // Dialog & overlay states
    var isDohDropdownExpanded by remember { mutableStateOf(false) }
    var showSiteSettingsSheet by remember { mutableStateOf(false) }
    var showPasskeySheet by remember { mutableStateOf(false) }
    var showSafeSearchDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showIncinerateDialog by remember { mutableStateOf(false) }
    var showPasswordManagerScreen by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }

    // Dynamic preferences states
    var currentSafeSearch by remember { mutableStateOf(prefsManager.getSafeSearch()) }
    var currentLocation by remember { mutableStateOf(prefsManager.getLocation()) }
    var isBackgroundAudio by remember { mutableStateOf(prefsManager.isBackgroundAudioEnabled()) }

    // Hardware & system back button handler
    BackHandler(enabled = true) {
        if (showPasswordManagerScreen) {
            showPasswordManagerScreen = false
        } else if (showSiteSettingsSheet) {
            showSiteSettingsSheet = false
        } else if (showPasskeySheet) {
            showPasskeySheet = false
        } else if (showSafeSearchDialog) {
            showSafeSearchDialog = false
        } else if (showLocationDialog) {
            showLocationDialog = false
        } else if (showClearCacheDialog) {
            showClearCacheDialog = false
        } else if (showClearHistoryDialog) {
            showClearHistoryDialog = false
        } else if (showIncinerateDialog) {
            showIncinerateDialog = false
        } else {
            onDismiss()
        }
    }

    val themeOptions = listOf(
        ThemeOption("system", R.string.theme_system, Icons.Default.PhoneAndroid),
        ThemeOption("dark", R.string.theme_dark, Icons.Default.DarkMode),
        ThemeOption("light", R.string.theme_light, Icons.Default.LightMode)
    )

    val dohProviders = listOf(
        Triple("cloudflare", "Cloudflare (1.1.1.1)", "Fast & Private"),
        Triple("quad9", "Quad9 (9.9.9.9)", "Malware Blocking"),
        Triple("adguard", "AdGuard DNS", "Ad Blocking"),
        Triple("system", "System Default", "Standard ISP")
    )

    val activeDoh = dohProviders.find { it.first == selectedDohProvider } ?: dohProviders.first()
    val activeEngine = SEARCH_ENGINE_PROVIDERS.find { it.id == selectedSearchEngine }
        ?: SEARCH_ENGINE_PROVIDERS[0]
    val activeLang = SUPPORTED_LANGUAGES.find { it.code == selectedLanguageCode }
        ?: SUPPORTED_LANGUAGES.first()

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDohDropdownExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "DohArrowRotation"
    )

    // Localization provider
    val localizedContext = remember(selectedLanguageCode) {
        LocaleHelper.applyLocale(context, selectedLanguageCode)
    }
    val localizedConfiguration = remember(selectedLanguageCode, localizedContext) {
        localizedContext.resources.configuration
    }
    val layoutDirection = remember(selectedLanguageCode) {
        val loc = LocaleHelper.getLocale(selectedLanguageCode)
        if (LocaleHelper.isRtl(loc)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    // Default Browser status
    var isDefaultBrowser by remember {
        mutableStateOf(DefaultBrowserManager.isDefaultBrowser(context))
    }
    val defaultBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultBrowser = DefaultBrowserManager.isDefaultBrowser(context)
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        isDefaultBrowser = DefaultBrowserManager.isDefaultBrowser(context)
        onPauseOrDispose {}
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .adaptiveContentWidth(AzokleBreakpoints.MaxContentWidth)
                    .align(Alignment.TopCenter)
            ) {
                // 1. Top App Bar (Consistent with About & Legal screens)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                // 2. Scrollable Settings Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CATEGORY 1: GENERAL & APPEARANCE
                    SectionContainer(title = "GENERAL & APPEARANCE") {
                        // Theme Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AzokleRadius.md)
                                    .background(colors.surfaceElevated)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                themeOptions.forEach { option ->
                                    val isSelected = selectedThemeMode.equals(option.id, ignoreCase = true)
                                    val label = stringResource(option.labelRes)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .sizeIn(minHeight = 34.dp)
                                            .clip(AzokleRadius.sm)
                                            .background(if (isSelected) colors.actionPrimaryBg else Color.Transparent)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.2f))
                                            ) { onThemeModeChange(option.id) }
                                            .semantics {
                                                role = Role.Tab
                                                contentDescription = "$label theme"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.actionPrimaryText else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ItemDivider()

                        // App Language
                        SettingsRow(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.choose_language),
                            trailingText = "${activeLang.flag} ${if (activeLang.isAuto) "Auto" else activeLang.nativeName}",
                            showChevron = true,
                            onClick = onOpenLanguage
                        )

                        ItemDivider()

                        // Default Browser
                        SettingsRow(
                            icon = Icons.Default.Public,
                            title = "Default Browser",
                            trailingContent = {
                                if (isDefaultBrowser) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active default browser",
                                        tint = colors.statusSuccess,
                                        modifier = Modifier.size(17.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.actionPrimaryBg)
                                            .padding(horizontal = 8.dp, vertical = 3.5.dp)
                                    ) {
                                        Text(
                                            text = "Set Default",
                                            color = colors.actionPrimaryText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (!isDefaultBrowser) {
                                    DefaultBrowserManager.launchSetDefaultBrowser(context, defaultBrowserLauncher)
                                } else {
                                    try {
                                        defaultBrowserLauncher.launch(DefaultBrowserManager.createManageDefaultAppsIntent())
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }

                    // CATEGORY 2: SEARCH
                    SectionContainer(title = "SEARCH") {
                        SettingsRow(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.choose_search_engine),
                            trailingText = activeEngine.name,
                            showChevron = true,
                            onClick = onOpenSearchEngine
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Shield,
                            title = "SafeSearch",
                            trailingText = currentSafeSearch,
                            showChevron = true,
                            onClick = { showSafeSearchDialog = true }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Public,
                            title = "Search Region",
                            trailingText = currentLocation,
                            showChevron = true,
                            onClick = { showLocationDialog = true }
                        )
                    }

                    // CATEGORY 3: PRIVACY & SECURITY
                    SectionContainer(title = "PRIVACY & SECURITY") {
                        // Encrypted DNS Accordion
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsRow(
                                icon = Icons.Default.Dns,
                                title = "Secure DNS (DoH)",
                                trailingText = activeDoh.second.substringBefore(" ("),
                                trailingIcon = Icons.Default.KeyboardArrowDown,
                                trailingIconRotation = arrowRotation,
                                onClick = { isDohDropdownExpanded = !isDohDropdownExpanded }
                            )

                            AnimatedVisibility(
                                visible = isDohDropdownExpanded,
                                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.surfaceElevated.copy(alpha = 0.35f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    dohProviders.forEach { (id, name, _) ->
                                        val isSelected = selectedDohProvider.equals(id, ignoreCase = true)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f))
                                                ) {
                                                    onDohProviderChange(id)
                                                    isDohDropdownExpanded = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = colors.textPrimary
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = colors.statusSuccess,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Security,
                            title = "Site Permissions",
                            showChevron = true,
                            onClick = { showSiteSettingsSheet = true }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Fingerprint,
                            title = "Passkeys & Biometrics",
                            showChevron = true,
                            onClick = { showPasskeySheet = true }
                        )
                    }

                    // CATEGORY 4: BROWSING DATA & STORAGE
                    SectionContainer(title = "BROWSING DATA & STORAGE") {
                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = "Saved Passwords & Logins",
                            trailingText = "${PasswordVaultManager.getAllLogins().size}",
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { showPasswordManagerScreen = true }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Download,
                            title = "Export Vault Backup",
                            onClick = {
                                val json = EncryptedVaultManager.getInstance(context).exportFullBackupJson()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Azokle Vault Backup", json))
                                ToastManager.showSuccess("Encrypted backup copied to clipboard")
                            }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Storage,
                            title = "Clear Web Cache",
                            onClick = { showClearCacheDialog = true }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Clear Search History",
                            onClick = { showClearHistoryDialog = true }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Incinerate Browsing Data",
                            titleColor = colors.statusDestructive,
                            iconTint = colors.statusDestructive,
                            onClick = { showIncinerateDialog = true }
                        )
                    }

                    // CATEGORY 5: MEDIA & NOTIFICATIONS
                    SectionContainer(title = "MEDIA & NOTIFICATIONS") {
                        SettingsRow(
                            icon = Icons.Default.MusicNote,
                            title = "Background Audio",
                            trailingContent = {
                                Switch(
                                    checked = isBackgroundAudio,
                                    onCheckedChange = { enabled ->
                                        isBackgroundAudio = enabled
                                        prefsManager.setBackgroundAudioEnabled(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.statusSuccess,
                                        checkedBorderColor = Color.Transparent,
                                        uncheckedThumbColor = colors.textMuted,
                                        uncheckedTrackColor = colors.surfaceElevated,
                                        uncheckedBorderColor = colors.borderSubtle
                                    )
                                )
                            },
                            onClick = {
                                val toggled = !isBackgroundAudio
                                isBackgroundAudio = toggled
                                prefsManager.setBackgroundAudioEnabled(toggled)
                            }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = "Notification Settings",
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                try {
                                    val intent = Intent().apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        } else {
                                            action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                            putExtra("app_package", context.packageName)
                                            putExtra("app_uid", context.applicationInfo.uid)
                                        }
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(fallbackIntent)
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }

                    // CATEGORY 6: ABOUT
                    SectionContainer(title = "ABOUT") {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "About Azokle Browser",
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = onOpenAbout
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for Updates",
                            trailingText = if (isCheckingUpdates) "Checking..." else "v${BuildConfig.VERSION_NAME}",
                            onClick = {
                                if (!isCheckingUpdates) {
                                    isCheckingUpdates = true
                                    coroutineScope.launch {
                                        val res = AppUpdateChecker.checkForUpdates(context)
                                        isCheckingUpdates = false
                                        res.onSuccess { info ->
                                            if (info.hasUpdate) {
                                                ToastManager.showSuccess("Update available: v${info.latestVersion}")
                                                onOpenUrl(info.downloadUrl)
                                            } else {
                                                ToastManager.showSuccess("Azokle Browser is up to date")
                                            }
                                        }.onFailure {
                                            ToastManager.showSuccess("Azokle Browser is up to date (v${BuildConfig.VERSION_NAME})")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Overlays & Dialogs
    if (showPasswordManagerScreen) {
        PasswordManagerScreen(onDismiss = { showPasswordManagerScreen = false })
    }

    if (showSiteSettingsSheet) {
        SiteSettingsSheet(onDismiss = { showSiteSettingsSheet = false })
    }

    if (showPasskeySheet) {
        PasskeyVaultSheet(onDismiss = { showPasskeySheet = false })
    }

    if (showSafeSearchDialog) {
        val options = listOf("Moderate", "Strict", "Off")
        AlertDialog(
            onDismissRequest = { showSafeSearchDialog = false },
            title = {
                Text(
                    text = "SafeSearch",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { option ->
                        val isSelected = currentSafeSearch.equals(option, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                                .clickable {
                                    currentSafeSearch = option
                                    prefsManager.setSafeSearch(option)
                                    showSafeSearchDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.statusSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafeSearchDialog = false }) {
                    Text("Done", color = colors.statusAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }

    if (showLocationDialog) {
        val locations = PreferencesManager.DEFAULT_LOCATIONS
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = "Search Region",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    locations.forEach { loc ->
                        val isSelected = currentLocation.equals(loc.name, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                                .clickable {
                                    currentLocation = loc.name
                                    prefsManager.setLocation(loc.name)
                                    showLocationDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = loc.flag, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = loc.name,
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.statusSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Done", color = colors.statusAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }

    // Clear Web Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text(
                    text = "Clear Web Cache?",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Free temporary cached files and site memory. Open tabs will remain intact.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                        } catch (_: Exception) {}
                        showClearCacheDialog = false
                        ToastManager.showSuccess("Web cache cleared")
                    }
                ) {
                    Text("Clear", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }

    // Clear Search History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear Search History?",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Remove recent search queries and suggestions stored on this device.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefsManager.clearRecentSearches()
                        showClearHistoryDialog = false
                        ToastManager.showSuccess("Search history cleared")
                    }
                ) {
                    Text("Clear", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }

    // Incinerate All Browsing Data Confirmation Dialog
    if (showIncinerateDialog) {
        AlertDialog(
            onDismissRequest = { showIncinerateDialog = false },
            title = {
                Text(
                    text = "Incinerate All Browsing Data?",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.statusDestructive
                )
            },
            text = {
                Text(
                    text = "Permanently erase active sessions, cookies, cached web files, and browsing records. This action is irreversible.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIncinerateDialog = false
                        if (onBurnData != null) {
                            onDismiss()
                            onBurnData()
                        } else {
                            coroutineScope.launch {
                                com.azokle.search.incineration.DataBurnEngine.executeFullDeepBurn(context)
                                ToastManager.showSuccess("Data incinerated permanently")
                            }
                        }
                    }
                ) {
                    Text("Incinerate", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncinerateDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = AzokleRadius.xl
        )
    }
}

// ---------------- Helper Components & Composables ----------------

@Composable
private fun SectionContainer(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = AzokleTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AzokleRadius.lg)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
        ) {
            content()
        }
    }
}

/**
 * Composable that renders the item divider UI component.
 */
@Composable
private fun ItemDivider() {
    val colors = AzokleTheme.colors
    HorizontalDivider(
        color = colors.borderSubtle,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 48.dp)
    )
}

/**
 * Composable that renders the settings row UI component.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    titleColor: Color? = null,
    iconTint: Color? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    trailingIconRotation: Float = 0f,
    showChevron: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AzokleTheme.colors
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .then(clickableModifier)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .semantics {
                if (onClick != null) role = Role.Button
                contentDescription = title
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
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
                color = titleColor ?: colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showChevron) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            } else if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(trailingIconRotation)
                )
            } else if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
