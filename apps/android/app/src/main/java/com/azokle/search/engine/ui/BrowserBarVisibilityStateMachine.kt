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

package com.azokle.search.engine.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Deterministic State Machine for Omnibar and Bottom Navigation bar visibility.
 *
 * Prevents recursive layout-scroll feedback loops, suppresses digitizer micro-jitter,
 * applies asymmetric density-aware hysteresis, and locks out conflicting transitions while
 * animations are running.
 *
 * States:
 *   VISIBLE  -> HIDING  (sustained downward scroll past top safe margin)
 *   HIDING   -> HIDDEN  (animation completion)
 *   HIDDEN   -> SHOWING (sustained upward scroll OR user reaches top of page)
 *   SHOWING  -> VISIBLE (animation completion)
 */
enum class BarVisibilityState {
    VISIBLE,
    HIDING,
    HIDDEN,
    SHOWING
}

class BrowserBarVisibilityStateMachine(
    private val density: Float
) {
    companion object {
        // Minimum displacement in dp to count as intentional scroll rather than digitizer jitter
        const val MIN_JITTER_DP = 2.5f

        // Minimum scrollY in dp below the page top before bars are permitted to hide
        // Prevents hiding while user is viewing the page header / hero area
        const val MIN_SCROLL_TO_HIDE_DP = 72f

        // Sustained directional accumulation required to initiate hiding
        const val HIDE_THRESHOLD_DP = 48f

        // Sustained directional accumulation required to initiate showing (asymmetric for responsiveness)
        const val SHOW_THRESHOLD_DP = 32f

        // Top margin threshold where bars must be fully revealed unconditionally
        const val TOP_REVEAL_THRESHOLD_DP = 16f

        // Time window in ms during which layout-induced synthetic scroll events are filtered out
        const val FEEDBACK_COOLDOWN_MS = 250L
    }

    var currentState by mutableStateOf(BarVisibilityState.VISIBLE)
        private set

    var isVisible by mutableStateOf(true)
        private set

    private var lastScrollY = 0
    private var accumulatedDeltaDp = 0f
    private var lastDirection = 0 // +1 for downward, -1 for upward, 0 for neutral
    private var transitionStartTime = 0L

    /**
     * Process scroll event dispatched from the GeckoSession ScrollDelegate.
     */
    fun onScroll(scrollX: Int, scrollY: Int) {
        val now = System.currentTimeMillis()

        // 1. Layout-induced Feedback Protection
        // When bars hide or show, Android layout resizes GeckoView, causing Gecko to emit
        // synthetic scroll callbacks. We absorb these during the transition cooldown window.
        if (currentState == BarVisibilityState.HIDING || currentState == BarVisibilityState.SHOWING) {
            if (now - transitionStartTime < FEEDBACK_COOLDOWN_MS) {
                lastScrollY = scrollY
                return
            }
        }

        val deltaPx = scrollY - lastScrollY
        lastScrollY = scrollY

        val deltaDp = if (density > 0f) deltaPx / density else deltaPx.toFloat()
        val scrollYDp = if (density > 0f) scrollY / density else scrollY.toFloat()

        // 2. Page Top Reveal Check
        // If the user has scrolled to the very top, unconditionally reveal bars
        if (scrollYDp <= TOP_REVEAL_THRESHOLD_DP) {
            accumulatedDeltaDp = 0f
            lastDirection = 0
            if (currentState != BarVisibilityState.VISIBLE && currentState != BarVisibilityState.SHOWING) {
                transitionTo(BarVisibilityState.SHOWING)
            }
            return
        }

        // 3. Jitter Suppression
        // Micro-movements from touch digitizer noise are filtered out without resetting accumulation
        if (kotlin.math.abs(deltaDp) < MIN_JITTER_DP) {
            return
        }

        // 4. Directional Accumulation with Reversal Reset
        val currentDirection = if (deltaDp > 0) 1 else -1
        if (currentDirection != lastDirection) {
            // Direction reversed: reset accumulated distance to the new impulse
            accumulatedDeltaDp = deltaDp
            lastDirection = currentDirection
        } else {
            // Sustained scroll in the same direction: accumulate
            accumulatedDeltaDp += deltaDp
        }

        // 5. State-Machine Evaluation
        when (currentState) {
            BarVisibilityState.VISIBLE -> {
                // Must exceed hide threshold AND must have scrolled past the top safe margin
                if (accumulatedDeltaDp >= HIDE_THRESHOLD_DP && scrollYDp > MIN_SCROLL_TO_HIDE_DP) {
                    transitionTo(BarVisibilityState.HIDING)
                    accumulatedDeltaDp = 0f
                }
            }
            BarVisibilityState.HIDDEN -> {
                // Must exceed show threshold in the upward direction
                if (accumulatedDeltaDp <= -SHOW_THRESHOLD_DP) {
                    transitionTo(BarVisibilityState.SHOWING)
                    accumulatedDeltaDp = 0f
                }
            }
            BarVisibilityState.HIDING -> {
                // Ignore hide/show inputs while hiding animation is active; wait for onAnimationFinished
            }
            BarVisibilityState.SHOWING -> {
                // Ignore hide/show inputs while showing animation is active; wait for onAnimationFinished
            }
        }
    }

    /**
     * Executes the transition to operation.
     */
    private fun transitionTo(target: BarVisibilityState) {
        currentState = target
        transitionStartTime = System.currentTimeMillis()
        when (target) {
            BarVisibilityState.HIDING -> isVisible = false
            BarVisibilityState.SHOWING -> isVisible = true
            BarVisibilityState.VISIBLE -> isVisible = true
            BarVisibilityState.HIDDEN -> isVisible = false
        }
    }

    /**
     * Notified by Compose animation lifecycle when the visibility transition finishes.
     */
    fun onAnimationFinished(completedToVisible: Boolean) {
        transitionStartTime = 0L
        accumulatedDeltaDp = 0f
        lastDirection = 0
        if (completedToVisible) {
            currentState = BarVisibilityState.VISIBLE
            isVisible = true
        } else {
            currentState = BarVisibilityState.HIDDEN
            isVisible = false
        }
    }

    /**
     * Unconditionally force bars to be visible (e.g., on tab switch, navigation, new tab).
     */
    fun forceShow() {
        transitionStartTime = 0L
        currentState = BarVisibilityState.VISIBLE
        isVisible = true
        accumulatedDeltaDp = 0f
        lastDirection = 0
    }

    /**
     * Reset tracker state on page reload or fresh tab activation.
     */
    fun reset(initialScrollY: Int = 0) {
        transitionStartTime = 0L
        currentState = BarVisibilityState.VISIBLE
        isVisible = true
        lastScrollY = initialScrollY
        accumulatedDeltaDp = 0f
        lastDirection = 0
    }
}
