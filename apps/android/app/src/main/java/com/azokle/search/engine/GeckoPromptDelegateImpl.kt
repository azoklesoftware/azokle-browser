package com.azokle.search.engine

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.security.KeyChain
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.azokle.search.common.util.UrlHelper
import com.azokle.search.navigation.NavigationSecurity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.*
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap

/**
 * Production-Grade GeckoSession PromptDelegate for Azokle Search.
 *
 * Implements 100% of GeckoView 154 PromptDelegate interface with:
 * - Tab & session lifecycle-safe dialog management (zero memory leaks / ghost dialogs).
 * - Full Tor & Private mode sandboxing (suppresses persistent credential storage & tracking).
 * - Multi-host file picker delegation supporting both MainActivity and PwaActivity via [FilePromptHost].
 * - Modern Material 3 dark-theme aligned dialogs with accessible typography.
 * - Interactive HTML5 color, date, time, and multi-choice controls.
 * - Enterprise & developer Client TLS Certificate authentication via Android KeyChain.
 * - WebAuthn related origin security checks & FedCM (IdentityCredential) origin isolation.
 */
class GeckoPromptDelegateImpl(
    private val context: Context,
    private val isPrivate: Boolean = false,
    private val isTor: Boolean = false,
    private val hasRecentUserInteraction: (() -> Boolean)? = null,
    private val onPopupRequested: ((String) -> Unit)? = null
) : GeckoSession.PromptDelegate {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeDialogs = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<Dialog, Boolean>()))

    companion object {
        private const val TAG = "GeckoPromptDelegate"
    }

    /**
     * Dismisses and clears all pending modal dialogs associated with this session.
     * Must be invoked during tab switching, session detachment, or incinerate/burn operations.
     */
    fun dismissAllPendingDialogs() {
        synchronized(activeDialogs) {
            val iterator = activeDialogs.iterator()
            while (iterator.hasNext()) {
                val dialog = iterator.next()
                try {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                } catch (_: Throwable) {}
                iterator.remove()
            }
        }
    }

    private fun trackDialog(dialog: Dialog) {
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }
    }

    private fun unwrapActivity(ctx: Context?): Activity? {
        var current = ctx
        while (current is android.content.ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext ?: break
        }
        return current as? Activity
    }

    fun isCredentialStoragePermitted(): Boolean = !isTor && !isPrivate

    fun isFedCmPermitted(): Boolean = !isTor && !isPrivate

    fun isWebAuthnRelatedOriginPermitted(): Boolean = !isTor && !isPrivate

    fun shouldAllowPopup(targetUri: String): Boolean {
        val isUserInitiated = hasRecentUserInteraction?.invoke() ?: true
        if (!isUserInitiated) return false
        if (targetUri.isNotBlank()) {
            val scheme = UrlHelper.extractScheme(targetUri)?.lowercase()
            val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data", "jar")
            if (scheme != null && (scheme in dangerousSchemes || NavigationSecurity.isForbiddenWebScheme(scheme))) {
                return false
            }
        }
        return true
    }

    fun shouldAllowRedirect(targetUri: String): Boolean {
        if (targetUri.isNotBlank()) {
            val scheme = UrlHelper.extractScheme(targetUri)?.lowercase()
            val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data", "jar")
            if (scheme != null && (scheme in dangerousSchemes || NavigationSecurity.isForbiddenWebScheme(scheme))) {
                return false
            }
        }
        return true
    }

    private fun isActivityUsable(activity: Activity?): Boolean {
        return activity != null && !activity.isFinishing && !activity.isDestroyed
    }

    // =========================================================================
    // 1. Core JavaScript Dialogs (Alert, Confirm, Prompt, BeforeUnload, Repost)
    // =========================================================================

    override fun onAlertPrompt(
        session: GeckoSession,
        prompt: AlertPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        val message = prompt.message ?: ""
        val title = prompt.title ?: "Alert"

        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { d, _ ->
                        d.dismiss()
                        result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onButtonPrompt(
        session: GeckoSession,
        prompt: ButtonPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle(prompt.title ?: "Confirm")
                    .setMessage(prompt.message ?: "")
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        result.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onTextPrompt(
        session: GeckoSession,
        prompt: TextPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val input = EditText(activity).apply {
                    setText(prompt.defaultValue ?: "")
                    setSingleLine()
                }
                val container = FrameLayout(activity!!).apply {
                    val padHorizontal = (24 * resources.displayMetrics.density).toInt()
                    val padVertical = (8 * resources.displayMetrics.density).toInt()
                    setPadding(padHorizontal, padVertical, padHorizontal, padVertical)
                    addView(input)
                }

                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(prompt.title ?: "Prompt")
                    .setMessage(prompt.message ?: "")
                    .setView(container)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        result.complete(prompt.confirm(input.text.toString()))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onBeforeUnloadPrompt(
        session: GeckoSession,
        prompt: BeforeUnloadPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Leave Site?")
                    .setMessage("Changes you made may not be saved.")
                    .setPositiveButton("Leave") { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.ALLOW))
                    }
                    .setNegativeButton("Stay") { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.DENY))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onRepostConfirmPrompt(
        session: GeckoSession,
        prompt: RepostConfirmPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Resend Information?")
                    .setMessage("The page that you're looking for used information that you entered. Returning to that page might cause any action you took to be repeated.")
                    .setPositiveButton("Resend") { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.ALLOW))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.DENY))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    // =========================================================================
    // 2. Authentication (HTTP Auth, Client TLS Certificates, Passkeys)
    // =========================================================================

    override fun onAuthPrompt(
        session: GeckoSession,
        prompt: AuthPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val layout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    val pad = (24 * resources.displayMetrics.density).toInt()
                    setPadding(pad, 8, pad, 8)
                }

                val userInput = EditText(activity).apply {
                    hint = "Username"
                    setText(prompt.authOptions.username ?: "")
                    setSingleLine()
                }
                val passInput = EditText(activity).apply {
                    hint = "Password"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSingleLine()
                }

                layout.addView(userInput)
                layout.addView(passInput)

                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle(prompt.title ?: "Authentication Required")
                    .setMessage(prompt.message ?: "")
                    .setView(layout)
                    .setPositiveButton("Sign In") { _, _ ->
                        result.complete(prompt.confirm(userInput.text.toString(), passInput.text.toString()))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onRequestCertificate(
        session: GeckoSession,
        prompt: CertificateRequest
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                try {
                    KeyChain.choosePrivateKeyAlias(
                        activity!!,
                        { alias ->
                            if (alias != null) {
                                result.complete(prompt.confirm(alias))
                            } else {
                                result.complete(prompt.dismiss())
                            }
                        },
                        null,
                        prompt.issuers,
                        prompt.host,
                        -1,
                        null
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch KeyChain certificate chooser: ${e.message}")
                    result.complete(prompt.dismiss())
                }
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onWebAuthnRelatedOriginPrompt(
        session: GeckoSession,
        prompt: WebAuthnRelatedOriginPrompt
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) {
            // Strict privacy mode: deny cross-origin passkey sharing
            return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY))
        }

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val action = if (prompt.isCreate) "register a passkey" else "sign in with a passkey"
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Passkey Authorization")
                    .setMessage("Allow ${prompt.origin} to $action associated with ${prompt.rpId}?")
                    .setPositiveButton("Allow") { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.ALLOW))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.DENY))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.confirm(AllowOrDeny.DENY))
            }
        }
        return result
    }

    // =========================================================================
    // 3. HTML5 Controls (Choices, Color, Date/Time, File & Folder)
    // =========================================================================

    override fun onChoicePrompt(
        session: GeckoSession,
        prompt: ChoicePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val choices = prompt.choices ?: emptyArray()
                val labels = choices.map { it.label ?: "" }.toTypedArray()
                val isMultiple = prompt.type == ChoicePrompt.Type.MULTIPLE

                val builder = MaterialAlertDialogBuilder(activity!!)
                    .setTitle(prompt.title ?: "Select Option")

                if (isMultiple) {
                    val selected = BooleanArray(choices.size) { choices[it].selected }
                    builder.setMultiChoiceItems(labels, selected) { _, which, isChecked ->
                        selected[which] = isChecked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val selectedItems = choices.filterIndexed { idx, _ -> selected[idx] }.toTypedArray()
                        result.complete(prompt.confirm(selectedItems))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.dismiss())
                    }
                } else {
                    val initialIdx = choices.indexOfFirst { it.selected }.coerceAtLeast(0)
                    var selectedIdx = initialIdx
                    builder.setSingleChoiceItems(labels, initialIdx) { _, which ->
                        selectedIdx = which
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (selectedIdx in choices.indices) {
                            result.complete(prompt.confirm(choices[selectedIdx]))
                        } else {
                            result.complete(prompt.dismiss())
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.dismiss())
                    }
                }

                val dialog = builder.setOnCancelListener { result.complete(prompt.dismiss()) }.create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onColorPrompt(
        session: GeckoSession,
        prompt: ColorPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (!isActivityUsable(activity)) {
                result.complete(prompt.dismiss())
                return@post
            }

            val palette = prompt.predefinedValues?.takeIf { it.isNotEmpty() } ?: arrayOf(
                "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4", "#3B82F6",
                "#6366F1", "#8B5CF6", "#EC4899", "#111827", "#6B7280", "#FFFFFF"
            )

            val dialog = MaterialAlertDialogBuilder(activity!!)
                .setTitle("Select Color")
                .setItems(palette) { _, which ->
                    result.complete(prompt.confirm(palette[which]))
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.complete(prompt.dismiss())
                }
                .setOnCancelListener { result.complete(prompt.dismiss()) }
                .create()
            trackDialog(dialog)
            dialog.show()
        }
        return result
    }

    override fun onDateTimePrompt(
        session: GeckoSession,
        prompt: DateTimePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (!isActivityUsable(activity)) {
                result.complete(prompt.dismiss())
                return@post
            }

            val cal = Calendar.getInstance()
            when (prompt.type) {
                DateTimePrompt.Type.TIME -> {
                    val timePicker = TimePickerDialog(
                        activity,
                        { _, hourOfDay, minute ->
                            val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                            result.complete(prompt.confirm(formatted))
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(activity)
                    )
                    timePicker.setOnCancelListener { result.complete(prompt.dismiss()) }
                    trackDialog(timePicker)
                    timePicker.show()
                }
                else -> {
                    val datePicker = DatePickerDialog(
                        activity!!,
                        { _, year, month, dayOfMonth ->
                            if (prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
                                val timePicker = TimePickerDialog(
                                    activity,
                                    { _, hourOfDay, minute ->
                                        val formatted = String.format(
                                            Locale.US,
                                            "%04d-%02d-%02dT%02d:%02d",
                                            year, month + 1, dayOfMonth, hourOfDay, minute
                                        )
                                        result.complete(prompt.confirm(formatted))
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    DateFormat.is24HourFormat(activity)
                                )
                                timePicker.setOnCancelListener { result.complete(prompt.dismiss()) }
                                trackDialog(timePicker)
                                timePicker.show()
                            } else {
                                val formatted = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    year, month + 1, dayOfMonth
                                )
                                result.complete(prompt.confirm(formatted))
                            }
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    datePicker.setOnCancelListener { result.complete(prompt.dismiss()) }
                    trackDialog(datePicker)
                    datePicker.show()
                }
            }
        }
        return result
    }

    override fun onFilePrompt(
        session: GeckoSession,
        prompt: FilePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val host = unwrapActivity(context) as? FilePromptHost
            if (host != null && isActivityUsable(unwrapActivity(context))) {
                host.launchFilePicker(prompt, result)
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onFolderUploadPrompt(
        session: GeckoSession,
        prompt: FolderUploadPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dirName = prompt.directoryName ?: "Selected Folder"
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Upload Directory?")
                    .setMessage("Upload all files from \"$dirName\" to this site? Only upload if you trust this website.")
                    .setPositiveButton("Upload") { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.ALLOW))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.confirm(AllowOrDeny.DENY))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    // =========================================================================
    // 4. Popups, Navigation & Redirection
    // =========================================================================

    override fun onPopupPrompt(
        session: GeckoSession,
        prompt: PopupPrompt
    ): GeckoResult<PromptResponse>? {
        val targetUri = prompt.targetUri ?: ""
        val isUserInitiated = hasRecentUserInteraction?.invoke() ?: true
        if (!isUserInitiated) {
            Log.w(TAG, "Blocked automated drive-by popup: $targetUri")
            return GeckoResult.fromValue(prompt.dismiss())
        }

        if (targetUri.isNotBlank()) {
            val scheme = UrlHelper.extractScheme(targetUri)?.lowercase()
            if (scheme != null && NavigationSecurity.isForbiddenWebScheme(scheme)) {
                Log.w(TAG, "Blocked dangerous scheme popup: $targetUri")
                return GeckoResult.fromValue(prompt.dismiss())
            }
        }

        // Notify UI to open the requested URL as a new tab / window
        if (targetUri.isNotBlank()) {
            mainHandler.post {
                onPopupRequested?.invoke(targetUri)
            }
        }

        return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))
    }

    override fun onRedirectPrompt(
        session: GeckoSession,
        prompt: RedirectPrompt
    ): GeckoResult<PromptResponse>? {
        val targetUri = prompt.targetUri ?: ""
        if (targetUri.isNotBlank()) {
            val scheme = UrlHelper.extractScheme(targetUri)?.lowercase()
            if (scheme != null && NavigationSecurity.isForbiddenWebScheme(scheme)) {
                return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY))
            }
        }
        return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))
    }

    override fun onSharePrompt(
        session: GeckoSession,
        prompt: SharePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    prompt.title?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    val content = listOfNotNull(
                        prompt.title?.takeIf { it.isNotBlank() },
                        prompt.text?.takeIf { it.isNotBlank() },
                        prompt.uri?.takeIf { it.isNotBlank() }
                    ).joinToString("\n\n")
                    putExtra(Intent.EXTRA_TEXT, content.ifBlank { prompt.uri ?: "" })
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (isActivityUsable(activity)) {
                    activity!!.startActivity(chooser)
                } else {
                    context.startActivity(chooser)
                }
                result.complete(prompt.confirm(SharePrompt.Result.SUCCESS))
            } catch (e: Exception) {
                Log.e(TAG, "Share failed: ${e.message}")
                result.complete(prompt.confirm(SharePrompt.Result.FAILURE))
            }
        }
        return result
    }

    // =========================================================================
    // 5. Credential & Autocomplete Management (Logins, Cards, Addresses)
    // =========================================================================

    override fun onLoginSave(
        session: GeckoSession,
        request: AutocompleteRequest<Autocomplete.LoginSaveOption>
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) {
            // Strictly zero persistent credential caching in Tor or Private sessions
            return GeckoResult.fromValue(request.dismiss())
        }

        val option = request.options?.firstOrNull() ?: return GeckoResult.fromValue(request.dismiss())
        val entry = option.value
        val username = entry.username ?: ""
        val origin = entry.origin ?: ""

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val message = if (username.isNotBlank()) {
                    "Save password for $username on $origin?"
                } else {
                    "Save password for $origin?"
                }
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Save Password")
                    .setMessage(message)
                    .setPositiveButton("Save") { _, _ ->
                        com.azokle.search.vault.PasswordVaultManager.saveLogin(origin, username, entry.password ?: "")
                        result.complete(request.confirm(option))
                    }
                    .setNegativeButton("Never") { _, _ ->
                        result.complete(request.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(request.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(request.dismiss())
            }
        }
        return result
    }

    override fun onLoginSelect(
        session: GeckoSession,
        request: AutocompleteRequest<Autocomplete.LoginSelectOption>
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) {
            return GeckoResult.fromValue(request.dismiss())
        }
        val options = request.options ?: return GeckoResult.fromValue(request.dismiss())
        if (options.isEmpty()) return GeckoResult.fromValue(request.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val labels = options.map { opt ->
                    val user = opt.value.username ?: "Unknown User"
                    val origin = opt.value.origin ?: ""
                    if (origin.isNotBlank()) "$user ($origin)" else user
                }.toTypedArray()

                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Select Saved Login")
                    .setItems(labels) { _, which ->
                        result.complete(request.confirm(options[which]))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(request.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(request.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(request.dismiss())
            }
        }
        return result
    }

    override fun onCreditCardSave(
        session: GeckoSession,
        request: AutocompleteRequest<Autocomplete.CreditCardSaveOption>
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) return GeckoResult.fromValue(request.dismiss())
        val option = request.options?.firstOrNull() ?: return GeckoResult.fromValue(request.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Save Payment Card")
                    .setMessage("Save this payment card for faster checkout next time?")
                    .setPositiveButton("Save") { _, _ ->
                        result.complete(request.confirm(option))
                    }
                    .setNegativeButton("No Thanks") { _, _ ->
                        result.complete(request.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(request.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(request.dismiss())
            }
        }
        return result
    }

    override fun onCreditCardSelect(
        session: GeckoSession,
        request: AutocompleteRequest<Autocomplete.CreditCardSelectOption>
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) return GeckoResult.fromValue(request.dismiss())
        val options = request.options ?: return GeckoResult.fromValue(request.dismiss())
        if (options.isEmpty()) return GeckoResult.fromValue(request.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val labels = options.map { opt ->
                    opt.value.name ?: "Payment Card"
                }.toTypedArray()

                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Select Saved Card")
                    .setItems(labels) { _, which ->
                        result.complete(request.confirm(options[which]))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(request.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(request.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(request.dismiss())
            }
        }
        return result
    }

    override fun onAddressSave(
        session: GeckoSession,
        request: AutocompleteRequest<Autocomplete.AddressSaveOption>
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) return GeckoResult.fromValue(request.dismiss())
        val option = request.options?.firstOrNull() ?: return GeckoResult.fromValue(request.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Save Address")
                    .setMessage("Save address for future autofill?")
                    .setPositiveButton("Save") { _, _ ->
                        result.complete(request.confirm(option))
                    }
                    .setNegativeButton("No Thanks") { _, _ ->
                        result.complete(request.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(request.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(request.dismiss())
            }
        }
        return result
    }

    override fun onAddressSelect(
        session: GeckoSession,
        request: AutocompleteRequest<Autocomplete.AddressSelectOption>
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) return GeckoResult.fromValue(request.dismiss())
        val options = request.options ?: return GeckoResult.fromValue(request.dismiss())
        if (options.isEmpty()) return GeckoResult.fromValue(request.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val labels = options.map { opt ->
                    opt.value.streetAddress ?: opt.value.name ?: "Address"
                }.toTypedArray()

                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Select Address")
                    .setItems(labels) { _, which ->
                        result.complete(request.confirm(options[which]))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(request.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(request.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(request.dismiss())
            }
        }
        return result
    }

    // =========================================================================
    // 6. Federated Credential Management (FedCM / IdentityCredential)
    // =========================================================================

    override fun onSelectIdentityCredentialProvider(
        session: GeckoSession,
        prompt: IdentityCredential.ProviderSelectorPrompt
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) {
            // Block federated identity cross-tracking in Tor/Private sessions
            return GeckoResult.fromValue(prompt.dismiss())
        }

        val providers = prompt.providers ?: emptyArray()
        if (providers.isEmpty()) return GeckoResult.fromValue(prompt.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val labels = providers.map { it.name ?: it.domain ?: "Identity Provider" }.toTypedArray()
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Sign in with...")
                    .setItems(labels) { _, which ->
                        result.complete(prompt.confirm(which))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onSelectIdentityCredentialAccount(
        session: GeckoSession,
        prompt: IdentityCredential.AccountSelectorPrompt
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) {
            return GeckoResult.fromValue(prompt.dismiss())
        }

        val accounts = prompt.accounts ?: emptyArray()
        if (accounts.isEmpty()) return GeckoResult.fromValue(prompt.dismiss())

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val labels = accounts.map { it.name ?: it.email ?: "Account" }.toTypedArray()
                val providerName = prompt.provider?.name ?: prompt.provider?.domain ?: "Provider"
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Sign in with $providerName")
                    .setItems(labels) { _, which ->
                        result.complete(prompt.confirm(which))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onShowPrivacyPolicyIdentityCredential(
        session: GeckoSession,
        prompt: IdentityCredential.PrivacyPolicyPrompt
    ): GeckoResult<PromptResponse>? {
        if (isTor || isPrivate) {
            return GeckoResult.fromValue(prompt.dismiss())
        }

        val result = GeckoResult<PromptResponse>()
        mainHandler.post {
            val activity = unwrapActivity(context)
            if (isActivityUsable(activity)) {
                val domain = prompt.providerDomain ?: "Identity Provider"
                val dialog = MaterialAlertDialogBuilder(activity!!)
                    .setTitle("Privacy Policy: $domain")
                    .setMessage("Continue signing in with $domain? By continuing, you agree to their Terms of Service and Privacy Policy.")
                    .setPositiveButton("Accept") { _, _ ->
                        result.complete(prompt.confirm(true))
                    }
                    .setNegativeButton("Decline") { _, _ ->
                        result.complete(prompt.confirm(false))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .create()
                trackDialog(dialog)
                dialog.show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }
}
