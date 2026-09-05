package com.azokle.search.browser.gecko

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
        val notHandledByContent = inputResult != INPUT_HANDLED_CONTENT
        val cannotScrollUp = (scrollDirections and PanZoomController.SCROLLABLE_FLAG_TOP) == 0
        val allowsVerticalOverscroll = (overscrollDirections and PanZoomController.OVERSCROLL_FLAG_VERTICAL) != 0

        // If unknown (initial touch down before APZ responds), check if content cannot scroll up
        if (inputResult == INPUT_HANDLING_UNKNOWN) {
            return cannotScrollUp
        }

        return notHandledByContent && cannotScrollUp && allowsVerticalOverscroll
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val event = MotionEvent.obtain(ev)
        val action = ev.actionMasked
        val eventY = event.y.toInt()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // A new gesture started. Ask GeckoView APZ if it can handle this.
                parent?.requestDisallowInterceptTouchEvent(true)
                updateInputResult(event)

                nestedOffsetY = 0
                lastY = eventY
                initialDownY = event.y

                event.recycle()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val allowScroll = !shouldPinOnScreen() && (inputResult == INPUT_HANDLED)

                var deltaY = lastY - eventY

                if (allowScroll && dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
                    deltaY -= scrollConsumed[1]
                    event.offsetLocation(0f, (-scrollOffset[1]).toFloat())
                    nestedOffsetY += scrollOffset[1]
                }

                lastY = eventY - scrollOffset[1]

                if (allowScroll && dispatchNestedScroll(0, scrollOffset[1], 0, deltaY, scrollOffset)) {
                    lastY -= scrollOffset[1]
                    event.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedOffsetY += scrollOffset[1]
                }

                val hasDragGestureStarted = event.y != initialDownY
                if (gestureCanReachParent && hasDragGestureStarted) {
                    updateInputResult(event)
                    event.recycle()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                inputResult = INPUT_HANDLING_UNKNOWN
                scrollDirections = PanZoomController.SCROLLABLE_FLAG_NONE
                overscrollDirections = PanZoomController.OVERSCROLL_FLAG_NONE
                stopNestedScroll()

                parent?.requestDisallowInterceptTouchEvent(false)
                gestureCanReachParent = true
            }
        }

        val eventHandled = super.onTouchEvent(event)
        event.recycle()
        return eventHandled
    }

    private fun updateInputResult(event: MotionEvent) {
        val eventAction = event.actionMasked
        val eventY = event.y

        onTouchEventForDetailResult(event).accept { detail ->
            if (!gestureCanReachParent) {
                return@accept
            }

            detail?.let {
                inputResult = it.handledResult()
                scrollDirections = it.scrollableDirections()
                overscrollDirections = it.overscrollDirections()
            }

            when (eventAction) {
                MotionEvent.ACTION_DOWN -> {
                    gestureCanReachParent = canOverscrollTop()

                    if (gestureCanReachParent && inputResult == INPUT_UNHANDLED) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (initialDownY < eventY) {
                        // Dragging downward: allow parent SwipeRefreshLayout to intercept
                        // ONLY if the event was NOT consumed by the website (game/canvas/touch listener)
                        if (!isTouchHandledByWebsite() && canOverscrollTop()) {
                            parent?.requestDisallowInterceptTouchEvent(false)
                        } else {
                            parent?.requestDisallowInterceptTouchEvent(true)
                            gestureCanReachParent = false
                        }
                    } else if (initialDownY > eventY) {
                        // Scrolling downward into content: disallow parent intercept completely
                        parent?.requestDisallowInterceptTouchEvent(true)
                        gestureCanReachParent = false
                    } else {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }

            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
        }
    }

    // NestedScrollingChild Delegation
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)

    override fun stopNestedScroll() = childHelper.stopNestedScroll()

    override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean = childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(
        velocityX: Float,
        velocityY: Float
    ): Boolean = childHelper.dispatchNestedPreFling(velocityX, velocityY)
}
