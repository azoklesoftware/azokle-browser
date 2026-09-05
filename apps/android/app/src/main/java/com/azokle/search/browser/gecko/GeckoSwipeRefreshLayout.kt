package com.azokle.search.browser.gecko

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

/**
 * Sovereign GeckoSwipeRefreshLayout:
 *  - Production-grade, Web-Standard-compliant pull-to-refresh container specialized for Mozilla GeckoView.
 *  - Engine-Level Overscroll & Input Arbitration:
 *      1. Connects directly to [NestedGeckoView], which queries Gecko's Quantum APZ engine.
 *      2. Interactive Content Lockout: If a web game (canvas, WebGL), interactive map, drawing surface,
 *         or JS touch listener consumes the touch, [NestedGeckoView.canOverscrollTop] evaluates to false,
 *         completely protecting gameplay from pull-to-refresh.
 *      3. Zero Domain Blacklists: Eliminates brittle URL matching; works universally on all websites.
 *      4. Fullscreen Lockout: Disables pull-to-refresh during HTML5 fullscreen video or games.
 *      5. Multi-touch Guard: 2+ pointers immediately disallow refresh for the gesture (pinch-zoom, dual controls).
 *      6. Horizontal Gesture Filter: Horizontal swipes (carousels, tabs) do not trigger refresh.
 */
class GeckoSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // Gesture Tracking
    private var initialDownX = 0f
    private var initialDownY = 0f
    private var isMultiTouchDetected = false
    private var isHorizontalSwipe = false

    /**
     * Updated via GeckoSession.ScrollDelegate
     */
    var currentScrollY: Int = 0
        set(value) {
            field = value
            updateEnabledState()
        }

    /**
     * Reference to embedded NestedGeckoView for APZ overscroll arbitration
     */
    var targetNestedGeckoView: NestedGeckoView? = null

    /**
     * Whether content can be considered at the top edge of scroll
     */
    var isContentAtTop: Boolean = true
        set(value) {
            field = value
            updateEnabledState()
        }

    /**
     * Current active URL
     */
    var currentUrl: String = ""

    /**
     * Whether browser is in Fullscreen mode (HTML5 video/game)
     */
    var isFullScreen: Boolean = false
        set(value) {
            field = value
            updateEnabledState()
        }

    /**
     * Whether the active page is currently loading
     */
    var isPageLoading: Boolean = false

    /**
     * Debounce reload execution
     */
    private var lastRefreshTimestamp: Long = 0L

    init {
        // High-precision child scroll check:
        // Returns true if child CAN scroll up (meaning NOT at top or handled by game -> do NOT pull-to-refresh)
        // Returns false if child CANNOT scroll up (meaning at top and eligible -> allow pull-to-refresh)
        setOnChildScrollUpCallback { _, _ ->
            if (isFullScreen) return@setOnChildScrollUpCallback true
            val target = targetNestedGeckoView
            if (target != null) {
                !target.canOverscrollTop()
            } else {
                !isAtTop()
            }
        }
    }

    fun isAtTop(): Boolean {
        return isContentAtTop && currentScrollY <= 15
    }

    /**
     * Dynamically determines whether the document can currently overscroll at the top.
     * Delegates to [NestedGeckoView.canOverscrollTop] when available.
     */
    fun canOverscrollTop(): Boolean {
        if (isFullScreen) return false
        val target = targetNestedGeckoView
        return if (target != null) {
            target.canOverscrollTop()
        } else {
            isAtTop()
        }
    }

    private fun updateEnabledState() {
        isEnabled = !isFullScreen
    }

    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        // If the webpage allows overscroll at the top and is not in fullscreen,
        // ignore the disallow request so parent can intercept downward pull-to-refresh.
        // If the touch is handled by web content (game, canvas, etc.), canOverscrollTop() is false,
        // so the disallow request is honored, giving 100% control to the game/canvas!
        if (b && canOverscrollTop() && !isFullScreen) {
            return
        }
        super.requestDisallowInterceptTouchEvent(b)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 1. Guard against inactive or fullscreen conditions
        if (!isEnabled || isFullScreen) {
            return false
        }

        // 2. Multi-touch guard: 2+ pointers (pinch-zoom, dual-finger game controls, joystick)
        if (ev.pointerCount > 1) {
            isMultiTouchDetected = true
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialDownX = ev.x
                initialDownY = ev.y
                isMultiTouchDetected = false
                isHorizontalSwipe = false

                // Initialize SwipeRefreshLayout internal state (mInitialDownY, mActivePointerId),
                // but NEVER intercept ACTION_DOWN itself so child receives it!
                super.onInterceptTouchEvent(ev)
                return false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                isMultiTouchDetected = true
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isMultiTouchDetected || isHorizontalSwipe) {
                    return false
                }

                val dx = abs(ev.x - initialDownX)
                val dy = ev.y - initialDownY

                // Moving upward (scrolling down into webpage) -> ignore gesture
                if (dy < 0) {
                    return false
                }

                // Moving horizontally more than vertically -> ignore gesture (carousel / horizontal swipe)
                if (dx > touchSlop && dx > dy) {
                    isHorizontalSwipe = true
                    return false
                }

                // Moving predominantly downwards and exceeded touch slop
                if (dy > touchSlop && dy > dx * 1.15f) {
                    if (canOverscrollTop()) {
                        return super.onInterceptTouchEvent(ev)
                    }
                }

                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isMultiTouchDetected = false
                isHorizontalSwipe = false
                super.onInterceptTouchEvent(ev)
                return false
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1) {
            isMultiTouchDetected = true
            return false
        }
        if (isMultiTouchDetected || isFullScreen) {
            return false
        }
        return super.onTouchEvent(ev)
    }

    /**
     * Debounced refresh trigger to prevent duplicate consecutive reloads
     */
    fun canTriggerRefresh(): Boolean {
        if (isFullScreen) {
            return false
        }
        val target = targetNestedGeckoView
        if (target != null && !target.canOverscrollTop()) {
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastRefreshTimestamp < 1500L) {
            return false
        }
        lastRefreshTimestamp = now
        return true
    }

    /**
     * Reset gesture tracking on tab switch or navigation
     */
    fun resetGestureState() {
        isMultiTouchDetected = false
        isHorizontalSwipe = false
    }
}
