package com.azokle.search.engine

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.mozilla.geckoview.AllowOrDeny
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
    val isPassword: Boolean = false,
    val isEditable: Boolean = false,
    val isCollapsed: Boolean = false,
    val canSearch: Boolean = !isPassword && text.isNotBlank(),
    val canShare: Boolean = !isPassword && text.isNotBlank(),
    val onCopy: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/**
 * Sovereign Text Selection & Floating Contextual Action Delegate for Azokle Browser.
 * Implements Mozilla GeckoView's [GeckoSession.SelectionActionDelegate] directly for
 * seamless integration with Jetpack Compose floating context menu.
 */
class AzokleSelectionDelegate(
    private val context: android.content.Context? = null,
    private val isPrivate: Boolean = false,
    private val isTor: Boolean = false,
    private val onPermissionPromptRequested: ((com.azokle.search.permissions.SitePermissionRequest) -> Unit)? = null,
    var onSelectionAction: ((TextSelectionPayload?) -> Unit)? = null
) : SelectionActionDelegate {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onShowClipboardPermissionRequest(
        session: GeckoSession,
        perm: SelectionActionDelegate.ClipboardPermission
    ): GeckoResult<AllowOrDeny>? {
        val uriStr = perm.uri ?: ""
        val scheme = try { android.net.Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
        val host = try { android.net.Uri.parse(uriStr).host ?: uriStr } catch (e: Exception) { uriStr }

        // Insecure origins are denied clipboard read access
        if (scheme != "https" && !host.equals("localhost", true) && !host.equals("127.0.0.1", true)) {
            return GeckoResult.fromValue(AllowOrDeny.DENY)
        }

        val ctx = context ?: return GeckoResult.fromValue(AllowOrDeny.DENY)
        val permManager = com.azokle.search.permissions.SitePermissionsManager.getInstance(ctx)
        val origin = permManager.canonicalizeOrigin(uriStr)
        val isTabPrivate = isPrivate || isTor

        val siteState = permManager.getPermission(origin, com.azokle.search.permissions.SitePermissionType.CLIPBOARD, isPrivate = isTabPrivate)
        if (siteState == com.azokle.search.permissions.SitePermissionState.BLOCK) {
            return GeckoResult.fromValue(AllowOrDeny.DENY)
        }
        if (siteState == com.azokle.search.permissions.SitePermissionState.ALLOW) {
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        val promptDispatcher = onPermissionPromptRequested ?: return GeckoResult.fromValue(AllowOrDeny.DENY)
        val geckoResult = GeckoResult<AllowOrDeny>()
        mainHandler.post {
            val requestPayload = com.azokle.search.permissions.SitePermissionRequest(
                host = permManager.getDisplayHost(origin),
                origin = origin,
                uri = uriStr,
                isPrivate = isTabPrivate,
                requestedTypes = listOf(com.azokle.search.permissions.SitePermissionType.CLIPBOARD),
                clipboardCallback = { allowOrDeny ->
                    geckoResult.complete(allowOrDeny)
                }
            )
            promptDispatcher(requestPayload)
        }
        return geckoResult
    }

    override fun onShowActionRequest(
        session: GeckoSession,
        selection: SelectionActionDelegate.Selection
    ) {
        val flags = selection.flags
        val isPassword = (flags and SelectionActionDelegate.FLAG_IS_PASSWORD) != 0
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0
        val isCollapsed = (flags and SelectionActionDelegate.FLAG_IS_COLLAPSED) != 0

        val rawText = selection.text ?: ""
        val trimmedText = rawText.trim()
        val rect = selection.screenRect ?: android.graphics.RectF(0f, 0f, 0f, 0f)

        // Strict Password Security Policy:
        // Passwords must never be copied, cut, searched, or shared to prevent credential harvesting
        val canCut = !isPassword && isEditable && !isCollapsed && selection.isActionAvailable(SelectionActionDelegate.ACTION_CUT)
        val canCopy = !isPassword && !isCollapsed && (selection.isActionAvailable(SelectionActionDelegate.ACTION_COPY) || trimmedText.isNotEmpty())
        val canPaste = isEditable && selection.isActionAvailable(SelectionActionDelegate.ACTION_PASTE)
        val canSelectAll = selection.isActionAvailable(SelectionActionDelegate.ACTION_SELECT_ALL)

        val canSearch = !isPassword && trimmedText.isNotEmpty()
        val canShare = !isPassword && trimmedText.isNotEmpty()

        Log.i("AZOKLE_SELECTION", "onShowActionRequest: isPassword=$isPassword, isEditable=$isEditable, isCollapsed=$isCollapsed, canCopy=$canCopy, canPaste=$canPaste")

        val payload = TextSelectionPayload(
            text = rawText,
            screenRect = rect,
            canCopy = canCopy,
            canCut = canCut,
            canPaste = canPaste,
            canSelectAll = canSelectAll,
            isPassword = isPassword,
            isEditable = isEditable,
            isCollapsed = isCollapsed,
            canSearch = canSearch,
            canShare = canShare,
            onCopy = {
                try {
                    selection.copy()
                } catch (e: Exception) {
                    Log.e("AZOKLE_SELECTION", "Error during copy", e)
                }
            },
            onCut = {
                try {
                    selection.cut()
                } catch (e: Exception) {
                    Log.e("AZOKLE_SELECTION", "Error during cut", e)
                }
            },
            onPaste = {
                try {
                    selection.paste()
                } catch (e: Exception) {
                    Log.e("AZOKLE_SELECTION", "Error during paste", e)
                }
            },
            onSelectAll = {
                try {
                    selection.selectAll()
                } catch (e: Exception) {
                    Log.e("AZOKLE_SELECTION", "Error during selectAll", e)
                }
            },
            onDismiss = {
                try {
                    selection.unselect()
                } catch (e: Exception) {
                    Log.e("AZOKLE_SELECTION", "Error during unselect", e)
                }
            }
        )

        // Dispatch selection payload to Compose UI layer on main thread
        mainHandler.post {
            onSelectionAction?.invoke(payload)
        }
    }

    override fun onHideAction(session: GeckoSession, reason: Int) {
        Log.i("AZOKLE_SELECTION", "onHideAction called with reason=$reason")
        // Dismiss Compose floating selection menu when user dismisses or selection becomes invisible
        if (reason == SelectionActionDelegate.HIDE_REASON_NO_SELECTION ||
            reason == SelectionActionDelegate.HIDE_REASON_INVISIBLE_SELECTION) {
            mainHandler.post {
                Log.i("AZOKLE_SELECTION", "Posting onSelectionAction(null) on main thread")
                onSelectionAction?.invoke(null)
            }
        }
    }
}
