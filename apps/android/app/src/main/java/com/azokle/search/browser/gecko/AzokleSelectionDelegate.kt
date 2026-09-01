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
 * Sovereign Text Selection & Floating Contextual Action Delegate for Azokle Browser.
 * Extends Mozilla GeckoView's native Quantum Selection engine (BasicSelectionActionDelegate).
 * Provides seamless floating action mode (Copy, Cut, Paste, Paste as Plain Text, Select All,
 * Share, Web Search, and Android 6.0+ Process Text actions) with exact window coordinate mapping.
 */
class AzokleSelectionDelegate(
    private val activity: Activity
) : BasicSelectionActionDelegate(activity, true) {

    companion object {
        const val ACTION_SHARE = "com.azokle.search.SHARE"
        const val ACTION_SEARCH = "com.azokle.search.SEARCH"

        private val CUSTOM_ACTIONS = arrayOf(
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
}
