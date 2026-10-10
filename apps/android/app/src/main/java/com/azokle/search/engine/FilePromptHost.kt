package com.azokle.search.engine

import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Interface implemented by host Activities (MainActivity, PwaActivity)
 * that provide system-level file picker delegation for GeckoView sessions.
 */
interface FilePromptHost {
    fun launchFilePicker(
        prompt: GeckoSession.PromptDelegate.FilePrompt,
        result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
    )
}
