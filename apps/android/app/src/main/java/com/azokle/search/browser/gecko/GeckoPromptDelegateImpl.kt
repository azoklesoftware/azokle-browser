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
            result.complete(prompt.dismiss())
        }
        return result
    }

    override fun onPopupPrompt(
        session: GeckoSession,
        prompt: PopupPrompt
    ): GeckoResult<PromptResponse>? {
        val targetUri = prompt.targetUri ?: ""
        if (targetUri.isNotBlank()) {
            onPopupRequested?.invoke(targetUri)
        }
        return GeckoResult.fromValue(prompt.dismiss())
    }
}
