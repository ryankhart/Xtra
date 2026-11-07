package com.github.andreyasadchy.xtra.ui.view

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.WeakHashMap

/**
 * Adds empty space below the last row of a vertical list while the minimized player is shown,
 * so the list can be scrolled far enough that the minimized player doesn't cover its last items.
 */
class MiniPlayerSpaceDecoration(private val space: () -> Int) : RecyclerView.ItemDecoration() {

    // space each item view was last given, see onDraw
    private val appliedSpace = WeakHashMap<View, Int>()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val extraSpace = getSpace(view, parent, state)
        appliedSpace[view] = extraSpace
        outRect.bottom = extraSpace
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // RecyclerView keeps the offsets of items that weren't rebound, so after items are added, removed or moved
        // the previous last row could keep its space, or the new last row could miss it, until offsets are refreshed.
        // Views animating out keep stale positions, so wait until animations finish (removing them redraws the list)
        if (parent.itemAnimator?.isRunning == true) {
            return
        }
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as? RecyclerView.LayoutParams ?: continue
            if (layoutParams.isItemRemoved || layoutParams.isViewInvalid) {
                continue
            }
            if ((appliedSpace[child] ?: 0) != getSpace(child, parent, state)) {
                parent.post { parent.invalidateItemDecorations() }
                return
            }
        }
    }

    private fun getSpace(view: View, parent: RecyclerView, state: RecyclerView.State): Int {
        val extraSpace = space()
        if (extraSpace <= 0) {
            return 0
        }
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return 0
        if (layoutManager.orientation != RecyclerView.VERTICAL || layoutManager.reverseLayout) {
            return 0
        }
        val position = parent.getChildLayoutPosition(view)
        val lastPosition = state.itemCount - 1
        if (position < 0 || lastPosition < 0) {
            return 0
        }
        val isLastRow = if (layoutManager is GridLayoutManager) {
            // every item in the last row gets the same offset, otherwise the grid stretches the other items in that row
            val spanSizeLookup = layoutManager.spanSizeLookup
            val spanCount = layoutManager.spanCount
            spanSizeLookup.getSpanGroupIndex(position, spanCount) == spanSizeLookup.getSpanGroupIndex(lastPosition, spanCount)
        } else {
            position == lastPosition
        }
        return if (isLastRow) extraSpace else 0
    }
}
