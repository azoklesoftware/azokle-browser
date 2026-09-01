package com.azokle.search.browser.gecko

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.SelectionActionDelegate
import org.mozilla.geckoview.GeckoView

/**
 * Enterprise-grade, floating text selection and clipboard action delegate for Azokle Browser.
 * Integrates Android native ActionMode.Callback2 with Mozilla GeckoView Quantum Selection engine.
 * Full fidelity support for input boxes, textareas, contenteditable, passwords, and selectable webpage text.
 */
class AzokleSelectionDelegate(
    private val activity: Activity,
    private val geckoView: GeckoView
) : SelectionActionDelegate {

    private var currentActionMode: ActionMode? = null
    private var activeSelection: SelectionActionDelegate.Selection? = null
    private var activeSession: GeckoSession? = null

    companion object {
        private const val TAG = "AzokleSelection"
        private const val ID_SEARCH = 10001
    }

    override fun onShowActionRequest(
        session: GeckoSession,
        selection: SelectionActionDelegate.Selection
    ) {
        activeSelection = selection
        activeSession = session

        val flags = selection.flags
        val isCollapsed = (flags and SelectionActionDelegate.FLAG_IS_COLLAPSED) != 0
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0
        val isPassword = (flags and SelectionActionDelegate.FLAG_IS_PASSWORD) != 0
        val selectedText = selection.text?.trim() ?: ""
        val hasSelection = selectedText.isNotEmpty() && !isCollapsed

        Log.d(TAG, "onShowActionRequest: text='$selectedText', isEditable=$isEditable, isPassword=$isPassword, isCollapsed=$isCollapsed")

        activity.runOnUiThread {
            try {
                currentActionMode?.finish()

                val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val hasClipData = clipboardManager?.hasPrimaryClip() == true &&
                        (clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                                clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true)

                val callback = object : ActionMode.Callback2() {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.clear()

                        // 1. CUT Action (Editable + Selected Text + Not Password)
                        if (isEditable && !isPassword && hasSelection && selection.isActionAvailable(SelectionActionDelegate.ACTION_CUT)) {
                            menu.add(Menu.NONE, android.R.id.cut, 1, "Cut")
                                .setIcon(android.R.drawable.ic_menu_edit)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }

                        // 2. COPY Action (Selected Text + Not Password)
                        if (!isPassword && hasSelection && selection.isActionAvailable(SelectionActionDelegate.ACTION_COPY)) {
                            menu.add(Menu.NONE, android.R.id.copy, 2, "Copy")
                                .setIcon(android.R.drawable.ic_menu_save)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }

                        // 3. PASTE Action (Editable + (Action Available or System Clipboard has Data))
                        if (isEditable && (selection.isActionAvailable(SelectionActionDelegate.ACTION_PASTE) || hasClipData)) {
                            menu.add(Menu.NONE, android.R.id.paste, 3, "Paste")
                                .setIcon(android.R.drawable.ic_menu_edit)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        }

                        // 4. SELECT ALL Action
                        if (selection.isActionAvailable(SelectionActionDelegate.ACTION_SELECT_ALL)) {
                            menu.add(Menu.NONE, android.R.id.selectAll, 4, "Select All")
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }

                        // 5. SHARE Action (Selected Text + Not Password)
                        if (!isPassword && hasSelection) {
                            menu.add(Menu.NONE, android.R.id.shareText, 5, "Share")
                                .setIcon(android.R.drawable.ic_menu_share)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }

                        // 6. WEB SEARCH Action (Selected Text + Not Password)
                        if (!isPassword && hasSelection && selectedText.isNotBlank()) {
                            menu.add(Menu.NONE, ID_SEARCH, 6, "Web Search")
                                .setIcon(android.R.drawable.ic_menu_search)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }

                        return menu.size() > 0
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        val currentText = activeSelection?.text ?: selectedText
                        when (item.itemId) {
                            android.R.id.cut -> {
                                try {
                                    activeSelection?.cut()
                                } catch (e: Exception) {
                                    if (currentText.isNotBlank()) {
                                        clipboardManager?.setPrimaryClip(ClipData.newPlainText("Text", currentText))
                                        activeSelection?.delete()
                                    }
                                }
                                mode.finish()
                                return true
                            }
                            android.R.id.copy -> {
                                try {
                                    activeSelection?.copy()
                                } catch (e: Exception) {
                                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("Text", currentText))
                                }
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                mode.finish()
                                return true
                            }
                            android.R.id.paste -> {
                                try {
                                    if (activeSelection?.isActionAvailable(SelectionActionDelegate.ACTION_PASTE) == true) {
                                        activeSelection?.paste()
                                    } else {
                                        activeSelection?.pasteAsPlainText()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error executing paste: ${e.message}")
                                }
                                mode.finish()
                                return true
                            }
                            android.R.id.selectAll -> {
                                try {
                                    activeSelection?.selectAll()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error executing selectAll: ${e.message}")
                                }
                                return true
                            }
                            android.R.id.shareText -> {
                                if (currentText.isNotBlank()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, currentText)
                                    }
                                    activity.startActivity(Intent.createChooser(shareIntent, "Share text"))
                                }
                                mode.finish()
                                return true
                            }
                            ID_SEARCH -> {
                                if (currentText.isNotBlank()) {
                                    val searchUrl = com.azokle.search.util.UrlHelper.resolveSearchOrUrl(
                                        context = activity,
                                        input = currentText.trim()
                                    )
                                    session.loadUri(searchUrl)
                                }
                                mode.finish()
                                return true
                            }
                        }
                        return false
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        currentActionMode = null
                    }

                    override fun onGetContentRect(mode: ActionMode, view: android.view.View, outRect: Rect) {
                        val rectF = activeSelection?.screenRect
                        if (rectF != null && !rectF.isEmpty) {
                            val viewLocation = IntArray(2)
                            view.getLocationOnScreen(viewLocation)
                            outRect.set(
                                (rectF.left - viewLocation[0]).toInt().coerceAtLeast(0),
                                (rectF.top - viewLocation[1]).toInt().coerceAtLeast(0),
                                (rectF.right - viewLocation[0]).toInt().coerceAtLeast(view.width.coerceAtLeast(1)),
                                (rectF.bottom - viewLocation[1]).toInt().coerceAtLeast(40)
                            )
                        } else {
                            super.onGetContentRect(mode, view, outRect)
                        }
                    }
                }

                // Launch floating ActionMode directly on the GeckoView
                val mode = geckoView.startActionMode(callback, ActionMode.TYPE_FLOATING)
                    ?: activity.startActionMode(callback, ActionMode.TYPE_FLOATING)

                currentActionMode = mode
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start action mode: ${e.message}", e)
            }
        }
    }

    override fun onHideAction(session: GeckoSession, reason: Int) {
        Log.d(TAG, "onHideAction reason=$reason")
        activity.runOnUiThread {
            try {
                currentActionMode?.finish()
            } catch (e: Exception) {
                // Safe ignore
            }
            currentActionMode = null
            activeSelection = null
            activeSession = null
        }
    }

    override fun onShowClipboardPermissionRequest(
        session: GeckoSession,
        perm: SelectionActionDelegate.ClipboardPermission
    ): GeckoResult<AllowOrDeny>? {
        // Allow user-initiated clipboard access seamlessly
        return GeckoResult.fromValue(AllowOrDeny.ALLOW)
    }

    override fun onDismissClipboardPermissionRequest(session: GeckoSession) {
        // No-op
    }
}
