package com.azokle.search.browser.gecko

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
    var onSelectionAction: ((TextSelectionPayload?) -> Unit)? = null
) : SelectionActionDelegate {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onShowClipboardPermissionRequest(
        session: GeckoSession,
        perm: SelectionActionDelegate.ClipboardPermission
    ): GeckoResult<AllowOrDeny>? {
        // VULN-01: Disallow silent web script clipboard reading (e.g. navigator.clipboard.readText())
        // User-initiated paste operations through the floating selection menu execute safely via selection.paste()
        return GeckoResult.fromValue(AllowOrDeny.DENY)
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

        Log.i("AZOKLE_SELECTION", "onShowActionRequest: text='$selectedText', rect=$rect, canCopy=$canCopy, canPaste=$canPaste")

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
            Log.i("AZOKLE_SELECTION", "Dispatching payload to Compose: text='${payload.text}', onSelectionAction != null: ${onSelectionAction != null}")
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
