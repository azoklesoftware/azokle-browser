package com.azokle.search.browser.gecko

import android.app.Activity
import android.content.Context
import android.widget.Toast
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.*

/**
 * Production-Grade GeckoSession PromptDelegate for Azokle Search.
 * Handles JavaScript dialogs (alert, confirm, prompt), HTML5 file inputs,
 * HTTP Authentication, form resubmissions, and popup controls.
 */
class GeckoPromptDelegateImpl(
    private val context: Context,
    private val hasRecentUserInteraction: (() -> Boolean)? = null,
    private val onPopupRequested: ((String) -> Unit)? = null
) : GeckoSession.PromptDelegate {

    private fun unwrapActivity(ctx: Context?): Activity? {
        var current = ctx
        while (current is android.content.ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext ?: break
        }
        return current as? Activity
    }

    override fun onAlertPrompt(
        session: GeckoSession,
        prompt: AlertPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        val message = prompt.message ?: ""
        val title = prompt.title ?: "Alert"

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                android.app.AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .show()
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                android.app.AlertDialog.Builder(activity)
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
                    .show()
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                val input = android.widget.EditText(activity).apply {
                    setText(prompt.defaultValue ?: "")
                    setSingleLine()
                }
                val container = android.widget.FrameLayout(activity).apply {
                    val pad = (16 * resources.displayMetrics.density).toInt()
                    setPadding(pad, 4, pad, 4)
                    addView(input)
                }

                android.app.AlertDialog.Builder(activity)
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
                    .show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onAuthPrompt(
        session: GeckoSession,
        prompt: AuthPrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                val layout = android.widget.LinearLayout(activity).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    val pad = (16 * resources.displayMetrics.density).toInt()
                    setPadding(pad, 4, pad, 4)
                }

                val userInput = android.widget.EditText(activity).apply {
                    hint = "Username"
                    setText(prompt.authOptions.username ?: "")
                    setSingleLine()
                }
                val passInput = android.widget.EditText(activity).apply {
                    hint = "Password"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSingleLine()
                }

                layout.addView(userInput)
                layout.addView(passInput)

                android.app.AlertDialog.Builder(activity)
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
                    .show()
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                android.app.AlertDialog.Builder(activity)
                    .setTitle("Resend Information?")
                    .setMessage("The page that you're looking for used information that you entered. Returning to that page might cause any action you took to be repeated.")
                    .setPositiveButton("Resend") { _, _ ->
                        result.complete(prompt.confirm(org.mozilla.geckoview.AllowOrDeny.ALLOW))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        result.complete(prompt.confirm(org.mozilla.geckoview.AllowOrDeny.DENY))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .show()
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                android.app.AlertDialog.Builder(activity)
                    .setTitle("Leave Site?")
                    .setMessage("Changes you made may not be saved.")
                    .setPositiveButton("Leave") { _, _ ->
                        result.complete(prompt.confirm(org.mozilla.geckoview.AllowOrDeny.ALLOW))
                    }
                    .setNegativeButton("Stay") { _, _ ->
                        result.complete(prompt.confirm(org.mozilla.geckoview.AllowOrDeny.DENY))
                    }
                    .setOnCancelListener {
                        result.complete(prompt.dismiss())
                    }
                    .show()
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onChoicePrompt(
        session: GeckoSession,
        prompt: ChoicePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                val choices = prompt.choices ?: emptyArray()
                val labels = choices.map { it.label ?: "" }.toTypedArray()
                val isMultiple = prompt.type == ChoicePrompt.Type.MULTIPLE

                if (isMultiple) {
                    val selected = BooleanArray(choices.size) { choices[it].selected }
                    android.app.AlertDialog.Builder(activity)
                        .setTitle(prompt.title ?: "Select Options")
                        .setMultiChoiceItems(labels, selected) { _, which, isChecked ->
                            selected[which] = isChecked
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val selectedItems = choices.filterIndexed { idx, _ -> selected[idx] }.toTypedArray()
                            result.complete(prompt.confirm(selectedItems))
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            result.complete(prompt.dismiss())
                        }
                        .setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                } else {
                    val initialIdx = choices.indexOfFirst { it.selected }.coerceAtLeast(0)
                    var selectedIdx = initialIdx
                    android.app.AlertDialog.Builder(activity)
                        .setTitle(prompt.title ?: "Select Option")
                        .setSingleChoiceItems(labels, initialIdx) { _, which ->
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
                        .setOnCancelListener { result.complete(prompt.dismiss()) }
                        .show()
                }
            } else {
                result.complete(prompt.dismiss())
            }
        }
        return result
    }

    override fun onFilePrompt(
        session: GeckoSession,
        prompt: FilePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val mainActivity = unwrapActivity(context) as? com.azokle.search.MainActivity
            if (mainActivity != null && !mainActivity.isFinishing && !mainActivity.isDestroyed) {
                mainActivity.launchFilePicker(prompt, result)
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
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                result.complete(prompt.dismiss())
                return@post
            }

            val defaultHex = prompt.defaultValue?.takeIf { it.isNotBlank() } ?: "#3B82F6"
            val palette = prompt.predefinedValues?.takeIf { it.isNotEmpty() } ?: arrayOf(
                "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4", "#3B82F6",
                "#6366F1", "#8B5CF6", "#EC4899", "#111827", "#6B7280", "#FFFFFF"
            )

            android.app.AlertDialog.Builder(activity)
                .setTitle("Select Color")
                .setItems(palette) { _, which ->
                    result.complete(prompt.confirm(palette[which]))
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.complete(prompt.dismiss())
                }
                .setOnCancelListener { result.complete(prompt.dismiss()) }
                .show()
        }
        return result
    }

    override fun onSharePrompt(
        session: GeckoSession,
        prompt: SharePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            try {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    prompt.title?.takeIf { it.isNotBlank() }?.let { putExtra(android.content.Intent.EXTRA_SUBJECT, it) }
                    val content = listOfNotNull(
                        prompt.title?.takeIf { it.isNotBlank() },
                        prompt.text?.takeIf { it.isNotBlank() },
                        prompt.uri?.takeIf { it.isNotBlank() }
                    ).joinToString("\n\n")
                    putExtra(android.content.Intent.EXTRA_TEXT, content.ifBlank { prompt.uri ?: "" })
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = android.content.Intent.createChooser(shareIntent, "Share with").apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    activity.startActivity(chooser)
                } else {
                    context.startActivity(chooser)
                }
                result.complete(prompt.confirm(SharePrompt.Result.SUCCESS))
            } catch (e: Exception) {
                result.complete(prompt.confirm(SharePrompt.Result.FAILURE))
            }
        }
        return result
    }

    override fun onDateTimePrompt(
        session: GeckoSession,
        prompt: DateTimePrompt
    ): GeckoResult<PromptResponse>? {
        val result = GeckoResult<PromptResponse>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val activity = unwrapActivity(context)
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                result.complete(prompt.dismiss())
                return@post
            }

            val cal = java.util.Calendar.getInstance()
            when (prompt.type) {
                DateTimePrompt.Type.TIME -> {
                    val timePicker = android.app.TimePickerDialog(
                        activity,
                        { _, hourOfDay, minute ->
                            val formatted = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                            result.complete(prompt.confirm(formatted))
                        },
                        cal.get(java.util.Calendar.HOUR_OF_DAY),
                        cal.get(java.util.Calendar.MINUTE),
                        android.text.format.DateFormat.is24HourFormat(activity)
                    )
                    timePicker.setOnCancelListener { result.complete(prompt.dismiss()) }
                    timePicker.show()
                }
                else -> {
                    // Date, Month, Week, DateTimeLocal
                    val datePicker = android.app.DatePickerDialog(
                        activity,
                        { _, year, month, dayOfMonth ->
                            if (prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
                                val timePicker = android.app.TimePickerDialog(
                                    activity,
                                    { _, hourOfDay, minute ->
                                        val formatted = String.format(
                                            java.util.Locale.US,
                                            "%04d-%02d-%02dT%02d:%02d",
                                            year, month + 1, dayOfMonth, hourOfDay, minute
                                        )
                                        result.complete(prompt.confirm(formatted))
                                    },
                                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                                    cal.get(java.util.Calendar.MINUTE),
                                    android.text.format.DateFormat.is24HourFormat(activity)
                                )
                                timePicker.setOnCancelListener { result.complete(prompt.dismiss()) }
                                timePicker.show()
                            } else {
                                val formatted = String.format(
                                    java.util.Locale.US,
                                    "%04d-%02d-%02d",
                                    year, month + 1, dayOfMonth
                                )
                                result.complete(prompt.confirm(formatted))
                            }
                        },
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH),
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                    datePicker.setOnCancelListener { result.complete(prompt.dismiss()) }
                    datePicker.show()
                }
            }
        }
        return result
    }

    override fun onPopupPrompt(
        session: GeckoSession,
        prompt: PopupPrompt
    ): GeckoResult<PromptResponse>? {
        val targetUri = prompt.targetUri ?: ""
        // Check for user interaction before requesting popups to eliminate popup flooding attacks
        val isUserInitiated = hasRecentUserInteraction?.invoke() ?: true
        if (!isUserInitiated) {
            android.util.Log.w("GeckoPromptDelegateImpl", "Blocked automated drive-by popup prompt: $targetUri")
            return GeckoResult.fromValue(prompt.dismiss())
        }

        if (targetUri.isNotBlank()) {
            val scheme = com.azokle.search.util.UrlHelper.extractScheme(targetUri)
            val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript", "data")
            if (scheme == null || scheme !in dangerousSchemes) {
                onPopupRequested?.invoke(targetUri)
            }
        }
        return GeckoResult.fromValue(prompt.dismiss())
    }
}
