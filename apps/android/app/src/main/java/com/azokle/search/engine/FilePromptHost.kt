/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.engine

import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Interface implemented by host Activities (MainActivity, PwaActivity)
 * that provide system-level file picker delegation for GeckoView sessions.
 */
interface FilePromptHost {
    /**
     * Executes the launch file picker operation.
     */
    fun launchFilePicker(
        prompt: GeckoSession.PromptDelegate.FilePrompt,
        result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
    )
}
