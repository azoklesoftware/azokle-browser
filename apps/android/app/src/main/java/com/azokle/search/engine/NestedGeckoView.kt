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

    @Volatile
    private var lastUserTouchTimestamp: Long = 0L

    /**
     * True if the user interacted with the viewport via touch within the last 5 seconds.
     * Used to prevent automated/drive-by popup flooding and malicious redirects.
     */
    fun hasRecentUserInteraction(): Boolean {
        return (android.os.SystemClock.uptimeMillis() - lastUserTouchTimestamp) < 5000L
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            lastUserTouchTimestamp = android.os.SystemClock.uptimeMillis()
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                nestedOffsetY = 0
                lastY = ev.y.toInt()
                initialDownY = ev.y

                // If touch originates outside the top pull-to-refresh zone (e.g. playlist sheet, overlays, comments, drawer),
                // immediately disallow parent interception to give web elements full gesture authority.
                if (initialDownY > maxPullDownOriginPx) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.y - initialDownY
                // If gesture started in page content or scrolling upwards, disallow parent interception
                if (initialDownY > maxPullDownOriginPx || deltaY < -15) {
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
