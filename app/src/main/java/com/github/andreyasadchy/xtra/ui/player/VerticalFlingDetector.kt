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

    // The current stroke: its direction (1 down, -1 up, 0 not yet moved), where it started and the furthest point
    // it reached. It only turns once the finger comes back more than the touch slop from that furthest point, so
    // lift-off roll or a wobble can't restart it.
    private var strokeDirection = 0
    private var strokeStartY = 0f
    private var strokeExtremeY = 0f
    private var strokeTurned = false

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
        strokeDirection = 0
        strokeStartY = y
        strokeExtremeY = y
        strokeTurned = false
        leftSlop = false
        addToTracker(eventTime, x, y, MotionEvent.ACTION_DOWN)
    }

    fun add(eventTime: Long, x: Float, y: Float) {
        if (!isTracking) return
        addToTracker(eventTime, x, y, MotionEvent.ACTION_MOVE)
        updateStroke(y)
        // Only real movement counts as still moving; some screens keep reporting a resting finger, and other
        // fingers' moves report this one too.
        if (abs(x - anchorX) > jitter || abs(y - anchorY) > jitter) {
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
     * before lifting is caught by the time since it last moved instead. The last stroke must also run in the
     * flick's direction and be longer than the touch slop, so a tap or a final twitch is never a flick.
     * `firstStrokeMin` raises that minimum for a gesture that never turned, where a tap that slides is likelier
     * than a flick; a stroke after a turn is always judged against the touch slop.
     */
    fun finish(liftTime: Long, canceled: Boolean, firstStrokeMin: Float = touchSlop): VerticalFling {
        val tracker = tracker?.takeIf { isTracking } ?: return VerticalFling.NONE
        isTracking = false
        if (canceled || liftTime - lastMoveTime >= STOPPED_MS) {
            return VerticalFling.NONE
        }
        tracker.computeCurrentVelocity(1000)
        val xVelocity = tracker.xVelocity
        val yVelocity = tracker.yVelocity
        return when {
            abs(yVelocity) < minVelocity || abs(yVelocity) <= abs(xVelocity) -> VerticalFling.NONE
            abs(strokeExtremeY - strokeStartY) <= if (strokeTurned) touchSlop else firstStrokeMin -> VerticalFling.NONE
            yVelocity > 0 && strokeDirection == 1 -> VerticalFling.DOWN
            yVelocity < 0 && strokeDirection == -1 -> VerticalFling.UP
            else -> VerticalFling.NONE
        }
    }

    private fun updateStroke(y: Float) {
        when {
            // The first direction needs more than jitter, so a finger rolling as it lands doesn't pick it.
            strokeDirection == 0 -> if (abs(y - strokeStartY) > jitter) {
                strokeDirection = if (y > strokeStartY) 1 else -1
                strokeExtremeY = y
            }
            // Further in the same direction.
            (y - strokeExtremeY) * strokeDirection > 0 -> strokeExtremeY = y
            // Came back more than the slop: a new stroke starts at the turning point. It only counts as a turn if
            // the stroke it ends was itself longer than the slop; otherwise the gesture is still on its first stroke.
            (strokeExtremeY - y) * strokeDirection > touchSlop -> {
                if (abs(strokeExtremeY - strokeStartY) > touchSlop) strokeTurned = true
                strokeDirection = -strokeDirection
                strokeStartY = strokeExtremeY
                strokeExtremeY = y
            }
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
