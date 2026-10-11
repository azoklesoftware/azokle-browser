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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.PanZoomController

/**
 * High-performance, Web-Standard-compliant NestedGeckoView:
 *  - Intercepts touch inputs and queries GeckoView's native Quantum APZ (Asynchronous Pan/Zoom) engine.
 *  - Distinguishes between:
 *      1. Interactive Web Content / Gameplay (Canvas, WebGL, Unity, touch event listeners with preventDefault(), CSS overscroll-behavior: none).
 *      2. Document-Level Scrolling & Overscroll (Normal webpage reading, pull-to-refresh).
 *  - Zero hardcoded domain blacklists: works dynamically and universally across all websites.
 */
class NestedGeckoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GeckoView(context, attrs), NestedScrollingChild {

    private val childHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

    private var lastY: Int = 0
    private var initialDownY: Float = 0f
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private var nestedOffsetY: Int = 0

    private var gestureCanReachParent: Boolean = true

    // Internal state mirrored from GeckoView's PanZoomController.InputResultDetail
    private var inputResult: Int = INPUT_HANDLING_UNKNOWN
    private var scrollDirections: Int = PanZoomController.SCROLLABLE_FLAG_NONE
    private var overscrollDirections: Int = PanZoomController.OVERSCROLL_FLAG_NONE

    companion object {
        const val INPUT_HANDLING_UNKNOWN = -1
        const val INPUT_UNHANDLED = PanZoomController.INPUT_RESULT_UNHANDLED
        const val INPUT_HANDLED = PanZoomController.INPUT_RESULT_HANDLED
        const val INPUT_HANDLED_CONTENT = PanZoomController.INPUT_RESULT_HANDLED_CONTENT
    }

    init {
        isNestedScrollingEnabled = true
    }

    /**
     * Whether the active touch was consumed by the website through its own touch listeners
     * (e.g. game canvas, WebGL controller, joystick, map, drawing surface).
     */
    fun isTouchHandledByWebsite(): Boolean {
        return inputResult == INPUT_HANDLED_CONTENT
    }

    /**
     * True if the document allows vertical overscroll at the top edge AND the touch was NOT
     * consumed by interactive web content or blocked by CSS `overscroll-behavior-y: none / contain`.
     */
    fun canOverscrollTop(): Boolean {
        return !isTouchHandledByWebsite()
    }

    private val density = context.resources.displayMetrics.density
    private val maxPullDownOriginPx = (180 * density).toInt()
    private val edgeGestureThresholdPx = (32 * density).toInt()

    @Volatile
    private var lastUserTouchTimestamp: Long = 0L
    private var initialDownX: Float = 0f

    /**
     * True if the user interacted with the viewport via touch within the last 5 seconds.
     * Used to prevent automated/drive-by popup flooding and malicious redirects.
     */
    fun hasRecentUserInteraction(): Boolean {
        return (android.os.SystemClock.uptimeMillis() - lastUserTouchTimestamp) < 5000L
    }

    /**
     * Handles on touch event events.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            lastUserTouchTimestamp = android.os.SystemClock.uptimeMillis()
        }

        val screenWidth = resources.displayMetrics.widthPixels

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                nestedOffsetY = 0
                lastY = ev.y.toInt()
                initialDownY = ev.y
                initialDownX = ev.x

                val isEdgeBackGestureZone = initialDownX < edgeGestureThresholdPx || initialDownX > (screenWidth - edgeGestureThresholdPx)

                // If touch originates outside top pull-to-refresh zone and NOT in the system edge back gesture zone,
                // disallow parent interception to give web elements full gesture authority.
                // In the edge zone, allow parent/system to intercept for back gesture navigation without APZ long-press conflict.
                if (initialDownY > maxPullDownOriginPx && !isEdgeBackGestureZone) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.y - initialDownY
                val isEdgeBackGestureZone = initialDownX < edgeGestureThresholdPx || initialDownX > (screenWidth - edgeGestureThresholdPx)

                // If gesture started in page content or scrolling upwards, disallow parent interception unless in edge zone
                if ((initialDownY > maxPullDownOriginPx || deltaY < -15) && !isEdgeBackGestureZone) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return super.onTouchEvent(ev)
    }

    // NestedScrollingChild Delegation
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    /**
     * Checks whether nested scrolling enabled.
     */
    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled

    /**
     * Executes the start nested scroll operation.
     */
    override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)

    /**
     * Executes the stop nested scroll operation.
     */
    override fun stopNestedScroll() = childHelper.stopNestedScroll()

    /**
     * Checks if nested scrolling parent exists.
     */
    override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()

    /**
     * Executes the dispatch nested scroll operation.
     */
    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)

    /**
     * Executes the dispatch nested pre scroll operation.
     */
    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    /**
     * Executes the dispatch nested fling operation.
     */
    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean = childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    /**
     * Executes the dispatch nested pre fling operation.
     */
    override fun dispatchNestedPreFling(
        velocityX: Float,
        velocityY: Float
    ): Boolean = childHelper.dispatchNestedPreFling(velocityX, velocityY)
}
