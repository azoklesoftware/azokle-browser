package com.azokle.search.browser.gecko

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.BasicSelectionActionDelegate
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.SelectionActionDelegate

/**
 * Text Selection event payload providing text, screen coordinates, and action execution triggers to Compose.
 */
data class TextSelectionPayload(
    val text: String,
    val screenRect: android.graphics.RectF,
    val canCopy: Boolean = true,
    val canCut: Boolean = false,
    val canPaste: Boolean = false,
    val canSelectAll: Boolean = true,
    val onCopy: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/**
 * Sovereign Text Selection & Floating Contextual Action Delegate for Azokle Browser.
 * Extends Mozilla GeckoView's native Quantum Selection engine (BasicSelectionActionDelegate).
 * Dispatches selection coordinates and actions directly to modern Jetpack Compose floating UI,
 * while maintaining backward-compatible fallback to Android's native action mode.
 */
class AzokleSelectionDelegate(
    private val activity: Activity,
    private val onSelectionAction: ((TextSelectionPayload?) -> Unit)? = null
) : BasicSelectionActionDelegate(activity, true) {

    companion object {
        const val ACTION_SHARE = "com.azokle.search.SHARE"
        const val ACTION_SEARCH = "com.azokle.search.SEARCH"

        val CUSTOM_ACTIONS = arrayOf(
            SelectionActionDelegate.ACTION_CUT,
            SelectionActionDelegate.ACTION_COPY,
            SelectionActionDelegate.ACTION_PASTE,
            SelectionActionDelegate.ACTION_SELECT_ALL,
            ACTION_SHARE,
            ACTION_SEARCH,
            SelectionActionDelegate.ACTION_PASTE_AS_PLAIN_TEXT,
            ACTION_PROCESS_TEXT
        )
    }

    init {
        // Enable system-wide Process Text actions (Translate, Maps, etc.)
        enableExternalActions(true)
    }

    override fun getAllActions(): Array<String> {
        return CUSTOM_ACTIONS
    }

    override fun isActionAvailable(action: String): Boolean {
        val sel = selection ?: return false
        val isPassword = (sel.flags and SelectionActionDelegate.FLAG_IS_PASSWORD) != 0
        val isCollapsed = (sel.flags and SelectionActionDelegate.FLAG_IS_COLLAPSED) != 0
        val text = sel.text?.trim() ?: ""
        val hasSelection = (text.isNotEmpty() || sel.isActionAvailable(SelectionActionDelegate.ACTION_COPY)) && !isCollapsed

        return when (action) {
            ACTION_SHARE -> !isPassword && hasSelection
            ACTION_SEARCH -> !isPassword && hasSelection
            else -> super.isActionAvailable(action)
        }
    }

    override fun prepareAction(action: String, item: MenuItem) {
        when (action) {
            ACTION_SHARE -> {
                item.title = "Share"
                item.setIcon(android.R.drawable.ic_menu_share)
            }
            ACTION_SEARCH -> {
                item.title = "Web Search"
                item.setIcon(android.R.drawable.ic_menu_search)
            }
            else -> super.prepareAction(action, item)
        }
    }

    override fun performAction(action: String, item: MenuItem): Boolean {
        val sel = selection
        val text = sel?.text?.trim() ?: ""

        when (action) {
            ACTION_SHARE -> {
                if (text.isNotBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "Share text"))
                }
                clearSelection()
                return true
            }
            ACTION_SEARCH -> {
                if (text.isNotBlank()) {
                    val searchUrl = com.azokle.search.util.UrlHelper.resolveSearchOrUrl(
                        context = activity,
                        input = text
                    )
                    mSession?.loadUri(searchUrl)
                }
                clearSelection()
                return true
            }
            else -> return super.performAction(action, item)
        }
    }

    override fun onShowClipboardPermissionRequest(
        session: GeckoSession,
        perm: SelectionActionDelegate.ClipboardPermission
    ): GeckoResult<AllowOrDeny>? {
        // Automatically allow user-initiated paste from clipboard without blocking prompt
        return GeckoResult.fromValue(AllowOrDeny.ALLOW)
    }

    override fun onShowActionRequest(
        session: GeckoSession,
        selection: SelectionActionDelegate.Selection
    ) {
        val selectedText = selection.text?.trim() ?: ""
        val rect = selection.screenRect ?: android.graphics.RectF(0f, 0f, 0f, 0f)
        val canCut = selection.isActionAvailable(SelectionActionDelegate.ACTION_CUT)
        val canCopy = selection.isActionAvailable(SelectionActionDelegate.ACTION_COPY) || selectedText.isNotEmpty()
        val canPaste = selection.isActionAvailable(SelectionActionDelegate.ACTION_PASTE)
        val canSelectAll = selection.isActionAvailable(SelectionActionDelegate.ACTION_SELECT_ALL)

        val payload = TextSelectionPayload(
            text = selectedText,
            screenRect = rect,
            canCopy = canCopy,
            canCut = canCut,
            canPaste = canPaste,
            canSelectAll = canSelectAll,
            onCopy = {
                try {
                    selection.copy()
                } catch (e: Exception) {}
            },
            onCut = {
                try {
                    selection.cut()
                } catch (e: Exception) {}
            },
            onPaste = {
                try {
                    selection.paste()
                } catch (e: Exception) {}
            },
            onSelectAll = {
                try {
                    selection.selectAll()
                } catch (e: Exception) {}
            },
            onDismiss = {
                try {
                    selection.unselect()
                } catch (e: Exception) {}
            }
        )

        // Dispatch selection payload to Compose UI layer on main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onSelectionAction?.invoke(payload)
        }

        try {
            super.onShowActionRequest(session, selection)
        } catch (e: Exception) {
            // Gracefully ignore any window ActionMode attachment issues in Compose
        }
    }

    override fun onHideAction(session: GeckoSession, reason: Int) {
        // Clear Compose floating selection menu when user dismisses or scrolls
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onSelectionAction?.invoke(null)
        }
        try {
            super.onHideAction(session, reason)
        } catch (e: Exception) {}
    }
}
