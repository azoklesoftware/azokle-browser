package com.azokle.search.vault

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.core.BiometricAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copies sensitive credentials to the system clipboard with Android 13+ EXTRA_IS_SENSITIVE
 * concealment and an automated 45-second RAM/clipboard wiping watchdog.
 */
internal fun copySensitiveToClipboard(
    context: Context,
    label: String,
    content: String,
    coroutineScope: CoroutineScope
) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, content)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        clip.description.extras = extras
    }
    cm.setPrimaryClip(clip)

    coroutineScope.launch {
        delay(45_000L)
        try {
            val currentClip = cm.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0)?.text?.toString()
                if (currentText == content) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        cm.clearPrimaryClip()
                    } else {
                        cm.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun PasswordManagerScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = AzokleTheme.colors
    val allLogins by PasswordVaultManager.loginsFlow.collectAsState()

    var isVaultUnlocked by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var loginToEdit by remember { mutableStateOf<SavedLogin?>(null) }
    var loginToDelete by remember { mutableStateOf<SavedLogin?>(null) }

    val filteredLogins = remember(allLogins, searchQuery) {
        if (searchQuery.isBlank()) allLogins
        else allLogins.filter {
            it.origin.contains(searchQuery.trim(), ignoreCase = true) ||
            it.username.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(640.dp)
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Password Manager",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allLogins.size} saved ${if (allLogins.size == 1) "account" else "accounts"}",
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Vault Lock/Unlock Toggle
                    IconButton(onClick = {
                        if (isVaultUnlocked) {
                            isVaultUnlocked = false
                            ToastManager.showSuccess("Vault locked")
                        } else {
                            BiometricAuthManager.authenticate(
                                context = context,
                                title = "Unlock Password Vault",
                                subtitle = "Authenticate to access credentials",
                                onAuthenticated = {
                                    isVaultUnlocked = true
                                    ToastManager.showSuccess("Vault unlocked")
                                },
                                onDenied = { ToastManager.showError("Authentication required") }
                            )
                        }
                    }) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (isVaultUnlocked) "Lock Vault" else "Unlock Vault",
                            tint = if (isVaultUnlocked) colors.statusSuccess else colors.textMuted
                        )
                    }

                    // Secure Export
                    IconButton(onClick = {
                        BiometricAuthManager.authenticate(
                            context = context,
                            title = "Export Passwords",
                            subtitle = "Authenticate to copy password backup",
                            onAuthenticated = {
                                val json = PasswordVaultManager.exportVaultJson()
                                copySensitiveToClipboard(context, "Azokle Passwords Backup", json, coroutineScope)
                                ToastManager.showSuccess("Vault exported (clipboard clears in 45s)")
                            },
                            onDenied = { ToastManager.showError("Authentication required to export credentials") }
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Backup",
                            tint = colors.textSecondary
                        )
                    }

                    // Add Password
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Password",
                            tint = colors.actionPrimaryBg
                        )
                    }
                }
            }

            // 2. Security Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                (if (isVaultUnlocked) colors.statusSuccess else colors.actionPrimaryBg).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isVaultUnlocked) colors.statusSuccess else colors.actionPrimaryBg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isVaultUnlocked) "Vault Unlocked (Session Active)" else "Hardware AES-256 Vault (Locked)",
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isVaultUnlocked)
                                "Passwords can be revealed and copied. Tap the lock icon to lock anytime."
                            else "Logins are isolated, encrypted with Keystore MasterKey, and protected by biometric authentication.",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // 3. Search Bar
            if (allLogins.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search accounts or domains...",
                                color = colors.textMuted,
                                fontSize = 13.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.actionPrimaryBg,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            }

            // 4. Logins List or Empty State
            if (filteredLogins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No Saved Passwords" else "No Matching Accounts",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank())
                                "Credentials saved during browsing or added manually will appear securely here."
                            else "No saved credentials matched '$searchQuery'.",
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.actionPrimaryBg,
                                    contentColor = colors.actionPrimaryText
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Add Password", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogins, key = { it.id }) { login ->
                        PasswordCard(
                            login = login,
                            isVaultUnlocked = isVaultUnlocked,
                            onRequireUnlock = { onUnlocked ->
                                BiometricAuthManager.authenticate(
                                    context = context,
                                    title = "Unlock Password Vault",
                                    subtitle = "Authenticate to view or copy sensitive credentials",
                                    onAuthenticated = {
                                        isVaultUnlocked = true
                                        onUnlocked()
                                    },
                                    onDenied = { ToastManager.showError("Authentication required") }
                                )
                            },
                            onEdit = { loginToEdit = login },
                            onDelete = { loginToDelete = login },
                            onCopyUsername = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Username", login.username))
                                ToastManager.showSuccess("Username copied")
                            },
                            onCopyPassword = {
                                copySensitiveToClipboard(context, "Password", login.password, coroutineScope)
                                ToastManager.showSuccess("Password copied (auto-clears in 45s)")
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Credential Dialog
    if (showAddDialog) {
        AddOrEditLoginDialog(
            existingLogin = null,
            onDismiss = { showAddDialog = false },
            onSave = { origin, username, password ->
                PasswordVaultManager.saveLogin(origin, username, password)
                showAddDialog = false
                ToastManager.showSuccess("Credential saved to vault")
            }
        )
    }

    // Edit Credential Dialog
    loginToEdit?.let { login ->
        AddOrEditLoginDialog(
            existingLogin = login,
            onDismiss = { loginToEdit = null },
            onSave = { origin, username, password ->
                PasswordVaultManager.updateLogin(login.id, origin, username, password)
                loginToEdit = null
                ToastManager.showSuccess("Credential updated")
            }
        )
    }

    // Delete Confirmation Dialog
    loginToDelete?.let { login ->
        AlertDialog(
            onDismissRequest = { loginToDelete = null },
            title = {
                Text(text = "Delete Saved Login", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the credentials for ${login.origin} (${login.username})? This cannot be undone.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PasswordVaultManager.deleteLogin(login.id)
                        loginToDelete = null
                        ToastManager.showSuccess("Login deleted")
                    }
                ) {
                    Text(text = "Delete", color = colors.statusDestructive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loginToDelete = null }) {
                    Text(text = "Cancel", color = colors.textPrimary)
                }
            },
            containerColor = colors.cardBg
        )
    }
}

@Composable
private fun PasswordCard(
    login: SavedLogin,
    isVaultUnlocked: Boolean,
    onRequireUnlock: (onUnlocked: () -> Unit) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit
) {
    val colors = AzokleTheme.colors
    var showPassword by remember { mutableStateOf(false) }

    val formattedDate = remember(login.dateModified) {
        try {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(login.dateModified))
        } catch (_: Exception) { "" }
    }

    val displayOrigin = remember(login.origin) {
        OriginMatcher.extractHost(login.origin).ifEmpty { login.origin }
    }

    val domainInitial = remember(displayOrigin) {
        displayOrigin.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.actionPrimaryBg.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = domainInitial,
                            color = colors.actionPrimaryBg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = displayOrigin,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = login.username,
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopyUsername,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Username",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Login",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colors.statusDestructive.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Password Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRevealed = isVaultUnlocked && showPassword
                Text(
                    text = if (isRevealed) login.password else "••••••••••••",
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontFamily = if (isRevealed) FontFamily.Monospace else FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (isVaultUnlocked) {
                                showPassword = !showPassword
                            } else {
                                onRequireUnlock {
                                    showPassword = true
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isRevealed) "Hide password" else "Show password",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isVaultUnlocked) {
                                onCopyPassword()
                            } else {
                                onRequireUnlock {
                                    onCopyPassword()
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy password",
                            tint = colors.actionPrimaryBg,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (formattedDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saved $formattedDate",
                    color = colors.textMuted,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun AddOrEditLoginDialog(
    existingLogin: SavedLogin?,
    onDismiss: () -> Unit,
    onSave: (origin: String, username: String, password: String) -> Unit
) {
    val colors = AzokleTheme.colors
    var origin by remember { mutableStateOf(existingLogin?.origin ?: "") }
    var username by remember { mutableStateOf(existingLogin?.username ?: "") }
    var password by remember { mutableStateOf(existingLogin?.password ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    val isEditing = existingLogin != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Saved Login" else "Add Saved Login",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Website / Domain") },
                    placeholder = { Text("e.g. github.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Generator Button
                OutlinedButton(
                    onClick = {
                        password = PasswordVaultManager.generateStrongPassword(16, true)
                        showPassword = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colors.actionPrimaryBg,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Generate Strong Password",
                        fontSize = 12.5.sp,
                        color = colors.actionPrimaryBg
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (origin.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                        onSave(origin.trim(), username.trim(), password)
                    }
                },
                enabled = origin.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.actionPrimaryBg,
                    contentColor = colors.actionPrimaryText
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = if (isEditing) "Update" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = colors.textPrimary)
            }
        },
        containerColor = colors.cardBg
    )
}
