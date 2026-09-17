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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.AzokleThemeColors
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.DefaultBrowserManager
import com.azokle.search.common.util.LocaleHelper
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.common.util.SUPPORTED_LANGUAGES
import com.azokle.search.engine.GeckoEngineManager
import com.azokle.search.engine.ui.SiteSettingsSheet
import com.azokle.search.passkey.PasskeyVaultSheet
import com.azokle.search.tabs.TabManager

private data class ThemeOption(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector
)

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    // Dynamic preferences states
    var currentSafeSearch by remember { mutableStateOf(prefsManager.getSafeSearch()) }
    var currentLocation by remember { mutableStateOf(prefsManager.getLocation()) }
    var isBackgroundAudio by remember { mutableStateOf(prefsManager.isBackgroundAudioEnabled()) }

    // Hardware & system back button handler
    BackHandler(enabled = true) {
        if (showSiteSettingsSheet) {
            showSiteSettingsSheet = false
        } else if (showPasskeySheet) {
            showPasskeySheet = false
        } else if (showSafeSearchDialog) {
            showSafeSearchDialog = false
        } else if (showLocationDialog) {
            showLocationDialog = false
        } else if (showClearCacheDialog) {
            showClearCacheDialog = false
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
        Triple("cloudflare", "Cloudflare 1.1.1.1", "Zero-log Anycast encrypted DNS"),
        Triple("quad9", "Quad9 (9.9.9.9)", "Swiss non-profit malware blocking"),
        Triple("adguard", "AdGuard DNS", "Network-level ad & tracker filter"),
        Triple("system", "System Default DNS", "Standard ISP-provided name servers")
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
                // ==========================================
                // 1. TOP APP BAR (Minimalist & Accessible)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 20.dp, color = colors.textPrimary),
                                onClick = onDismiss
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Navigate back"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.semantics { heading() }) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Preferences & sovereign engine configuration",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(
                    color = colors.borderSubtle,
                    thickness = 0.5.dp
                )

                // ==========================================
                // 2. SCROLLABLE SETTINGS CONTENT
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ------------------------------------------
                    // CATEGORY: APPEARANCE
                    // ------------------------------------------
                    SectionContainer(title = "APPEARANCE") {
                        // Theme Mode Segmented Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.section_theme),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(bottom = 7.dp)
                            )

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
                                            .sizeIn(minHeight = 36.dp)
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
                                                modifier = Modifier.size(13.5.dp)
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
                            subtitle = if (activeLang.isAuto) "Auto-detect" else activeLang.nativeName,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeLang.flag,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = onOpenLanguage
                        )

                        ItemDivider()

                        // Default Browser
                        SettingsRow(
                            icon = Icons.Default.Public,
                            title = "Default Browser",
                            subtitle = if (isDefaultBrowser) "Azokle is your default browser" else "Open web links securely in Azokle",
                            trailingContent = {
                                if (isDefaultBrowser) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active default browser",
                                        tint = colors.statusSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.actionPrimaryBg)
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
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
                                    DefaultBrowserManager.launchSetDefaultBrowser(
                                        context = context,
                                        launcher = defaultBrowserLauncher
                                    )
                                } else {
                                    try {
                                        val intent = DefaultBrowserManager.createManageDefaultAppsIntent()
                                        defaultBrowserLauncher.launch(intent)
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }

                    // ------------------------------------------
                    // CATEGORY: SEARCH
                    // ------------------------------------------
                    SectionContainer(title = "SEARCH") {
                        // Search Engine Selection
                        SettingsRow(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.choose_search_engine),
                            subtitle = activeEngine.name,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeEngine.domain,
                                        fontSize = 11.5.sp,
                                        color = colors.textMuted
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = onOpenSearchEngine
                        )

                        ItemDivider()

                        // SafeSearch Filter
                        SettingsRow(
                            icon = Icons.Default.Shield,
                            title = "SafeSearch Filter",
                            subtitle = currentSafeSearch,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showSafeSearchDialog = true }
                        )

                        ItemDivider()

                        // Search Region / Location
                        SettingsRow(
                            icon = Icons.Default.Public,
                            title = "Search Region",
                            subtitle = currentLocation,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showLocationDialog = true }
                        )
                    }

                    // ------------------------------------------
                    // CATEGORY: PRIVACY & SECURITY
                    // ------------------------------------------
                    SectionContainer(title = "PRIVACY & SECURITY") {
                        // Encrypted DNS (DoH) Accordion
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsRow(
                                icon = Icons.Default.Dns,
                                title = stringResource(R.string.section_doh),
                                subtitle = activeDoh.second,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isDohDropdownExpanded) "Collapse" else "Expand",
                                        tint = colors.textMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .rotate(arrowRotation)
                                    )
                                },
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
                                        .background(colors.surfaceElevated.copy(alpha = 0.4f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    dohProviders.forEach { (id, name, desc) ->
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = name,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = desc,
                                                    fontSize = 10.5.sp,
                                                    color = colors.textMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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
                            }
                        }

                        ItemDivider()

                        // Site Permissions
                        SettingsRow(
                            icon = Icons.Default.Security,
                            title = "Site Permissions",
                            subtitle = "Hardware, camera, mic, location & storage rules",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showSiteSettingsSheet = true }
                        )

                        ItemDivider()

                        // Passkeys & Biometrics
                        SettingsRow(
                            icon = Icons.Default.Fingerprint,
                            title = "Passkeys & Biometrics",
                            subtitle = "FIDO2 WebAuthn credentials & hardware lock",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showPasskeySheet = true }
                        )

                        ItemDivider()

                        // Clear Search History
                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = stringResource(R.string.clear_history),
                            subtitle = "Erase local search suggestions and query history",
                            onClick = {
                                prefsManager.clearRecentSearches()
                                ToastManager.showSuccess("Search history cleared")
                            }
                        )

                        ItemDivider()

                        // Incinerate Session
                        SettingsRow(
                            icon = Icons.Default.LocalFireDepartment,
                            title = stringResource(R.string.incinerate_session),
                            subtitle = "Close all tabs, purge cookies, storage and cache",
                            iconTint = colors.statusDestructive,
                            onClick = {
                                GeckoEngineManager.clearAllData(context)
                                TabManager.closeAllTabs()
                                ToastManager.showSuccess("Browsing session incinerated")
                            }
                        )
                    }

                    // ------------------------------------------
                    // CATEGORY: NOTIFICATIONS
                    // ------------------------------------------
                    SectionContainer(title = "NOTIFICATIONS") {
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = "System Notification Settings",
                            subtitle = "Manage download alerts and system channels",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
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
                                    } catch (_: Exception) {
                                        // Ignore if system settings activity is unavailable
                                    }
                                }
                            }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.DownloadDone,
                            title = "Download Alerts",
                            subtitle = "Live progress and background completion updates"
                        )
                    }

                    // ------------------------------------------
                    // CATEGORY: DATA & STORAGE
                    // ------------------------------------------
                    SectionContainer(title = "DATA & STORAGE") {
                        // Background Audio
                        SettingsRow(
                            icon = Icons.Default.MusicNote,
                            title = "Background Audio",
                            subtitle = "Keep media playing when the app is minimized",
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

                        // Clear Web Cache
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            title = "Clear Web Cache",
                            subtitle = "Free temporary cached files and site memory",
                            onClick = { showClearCacheDialog = true }
                        )
                    }

                    // ------------------------------------------
                    // CATEGORY: ABOUT & LEGAL
                    // ------------------------------------------
                    SectionContainer(title = "ABOUT") {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.about_us),
                            subtitle = "azokle.com/company-info",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            onClick = {
                                onDismiss()
                                onOpenUrl("https://azokle.com/company-info")
                                onOpenAbout()
                            }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.privacy_policy),
                            subtitle = "azokle.com/privacy",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            onClick = {
                                onDismiss()
                                onOpenUrl("https://azokle.com/privacy")
                                onOpenPrivacyPolicy()
                            }
                        )

                        ItemDivider()

                        SettingsRow(
                            icon = Icons.Default.Shield,
                            title = stringResource(R.string.terms_service),
                            subtitle = "azokle.com/terms",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            onClick = {
                                onDismiss()
                                onOpenUrl("https://azokle.com/terms")
                                onOpenTerms()
                            }
                        )
                    }

                    // ------------------------------------------
                    // MINIMAL FOOTER
                    // ------------------------------------------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AZOKLE SEARCH • 2026.08",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zero Telemetry • Sovereign Privacy Engine",
                            fontSize = 10.sp,
                            color = colors.textMuted.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Site Settings Full Sheet
    if (showSiteSettingsSheet) {
        SiteSettingsSheet(
            onDismiss = { showSiteSettingsSheet = false }
        )
    }

    // Passkeys Vault Sheet
    if (showPasskeySheet) {
        PasskeyVaultSheet(
            onDismiss = { showPasskeySheet = false }
        )
    }

    // SafeSearch Filter Dialog
    if (showSafeSearchDialog) {
        val options = listOf("Moderate", "Strict", "Off")
        AlertDialog(
            onDismissRequest = { showSafeSearchDialog = false },
            title = {
                Text(
                    text = "SafeSearch Filter",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    modifier = Modifier.size(16.dp)
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

    // Search Location Dialog
    if (showLocationDialog) {
        val locations = PreferencesManager.DEFAULT_LOCATIONS
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = "Search Region",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                    modifier = Modifier.size(16.dp)
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

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text(
                    text = "Clear Web Cache?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will free up cached temporary files and images. Open tabs and saved cookies will remain intact.",
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
                        } catch (e: Exception) {}
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
}

@Composable
private fun SectionContainer(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = AzokleTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = colors.textMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AzokleRadius.lg)
                .background(colors.cardBg)
                .border(1.dp, colors.borderSubtle, AzokleRadius.lg)
        ) {
            content()
        }
    }
}

@Composable
private fun ItemDivider() {
    val colors = AzokleTheme.colors
    HorizontalDivider(
        color = colors.borderSubtle,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    iconTint: Color? = null,
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
            .sizeIn(minHeight = 50.dp)
            .then(clickableModifier)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                if (onClick != null) role = Role.Button
                contentDescription = if (subtitle != null) "$title, $subtitle" else title
            },
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: colors.textPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
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

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

