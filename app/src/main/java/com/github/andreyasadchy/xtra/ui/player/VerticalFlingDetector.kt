package com.github.andreyasadchy.xtra.ui.player

import android.view.MotionEvent
import android.view.VelocityTracker
import kotlin.math.abs

enum class VerticalFling { NONE, UP, DOWN }

/**
 * Decides whether a touch on the player ended in a vertical flick. Only the finger's speed when it lifts
 * counts, not how far it travelled.
 *
 * Positions are fed in screen pixels; a constant offset doesn't matter, since only differences are used.
 */
class VerticalFlingDetector(private val minVelocity: Float, private val touchSlop: Float) {

    private var tracker: VelocityTracker? = null
    private var downTime = 0L

    // Smaller moves (a resting finger's jitter, the contact point rolling as it lifts) don't count as moving.
    private val jitter = touchSlop / 4
    private var anchorX = 0f
    private var anchorY = 0f
    private var lastMoveTime = 0L

    private var downX = 0f
    private var downY = 0f

    // Where the current upward or downward stroke started: each moves to the last turning point.
    private var upStrokeStartY = 0f
    private var downStrokeStartY = 0f

    var isTracking = false
        private set

    /** Whether the finger has gone past the touch slop from where it landed, at any point. */
    var leftSlop = false
        private set

    fun start(eventTime: Long, x: Float, y: Float) {
        (tracker ?: VelocityTracker.obtain().also { tracker = it }).clear()
        isTracking = true
        downTime = eventTime
        downX = x
        downY = y
        anchorX = x
        anchorY = y
        lastMoveTime = eventTime
        upStrokeStartY = y
        downStrokeStartY = y
        leftSlop = false
        addToTracker(eventTime, x, y, MotionEvent.ACTION_DOWN)
    }

    fun add(eventTime: Long, x: Float, y: Float) {
        if (!isTracking) return
        addToTracker(eventTime, x, y, MotionEvent.ACTION_MOVE)
        // Only real movement counts as still moving; some screens keep reporting a resting finger, and other
        // fingers' moves report this one too.
        if (abs(x - anchorX) > jitter || abs(y - anchorY) > jitter) {
            // Moving down, an upward stroke would start from here, and vice versa.
            if (y > anchorY) {
                upStrokeStartY = y
            } else if (y < anchorY) {
                downStrokeStartY = y
            }
            anchorX = x
            anchorY = y
            lastMoveTime = eventTime
        }
        if (abs(x - downX) > touchSlop || abs(y - downY) > touchSlop) {
            leftSlop = true
        }
    }

    /**
     * Ends the gesture and returns its flick, if any. The lift event's own position must not be added: it
     * repeats the last position a frame later, which would read as a brief stop. A finger that really did stop
     * before lifting is caught by the time since it last moved instead. The last stroke must also be longer
     * than the touch slop in the flick's direction, so a tap or a final twitch is never a flick and lift-off
     * jitter can't flip the result.
     */
    fun finish(liftTime: Long, canceled: Boolean): VerticalFling {
        val tracker = tracker?.takeIf { isTracking } ?: return VerticalFling.NONE
        isTracking = false
        if (canceled || liftTime - lastMoveTime > STOPPED_MS) {
            return VerticalFling.NONE
        }
        tracker.computeCurrentVelocity(1000)
        val xVelocity = tracker.xVelocity
        val yVelocity = tracker.yVelocity
        return when {
            abs(yVelocity) < minVelocity || abs(yVelocity) <= abs(xVelocity) -> VerticalFling.NONE
            // Strokes end at the last real movement (the anchor), so lift-off roll can't lengthen or shorten them.
            yVelocity > 0 && anchorY - downStrokeStartY > touchSlop -> VerticalFling.DOWN
            yVelocity < 0 && upStrokeStartY - anchorY > touchSlop -> VerticalFling.UP
            else -> VerticalFling.NONE
        }
    }

    fun recycle() {
        tracker?.recycle()
        tracker = null
        isTracking = false
    }

    private fun addToTracker(eventTime: Long, x: Float, y: Float, action: Int) {
        val sample = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        tracker?.addMovement(sample)
        sample.recycle()
    }

    companion object {
        // A finger that hasn't moved for this long before lifting counts as stopped, not flicked (the same
        // window Android's own VelocityTracker uses).
        private const val STOPPED_MS = 40L
    }
}
