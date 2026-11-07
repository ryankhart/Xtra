package com.github.andreyasadchy.xtra.ui.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Adds empty space below the last row of a vertical list while the minimized player is shown,
 * so the list can be scrolled far enough that the minimized player doesn't cover its last items.
 */
class MiniPlayerSpaceDecoration(private val space: () -> Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val extraSpace = space()
        if (extraSpace <= 0) {
            return
        }
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        if (layoutManager.orientation != RecyclerView.VERTICAL || layoutManager.reverseLayout) {
            return
        }
        val position = parent.getChildLayoutPosition(view)
        val lastPosition = state.itemCount - 1
        if (position < 0 || lastPosition < 0) {
            return
        }
        val isLastRow = if (layoutManager is GridLayoutManager) {
            // every item in the last row gets the same offset, otherwise the grid stretches the other items in that row
            val spanSizeLookup = layoutManager.spanSizeLookup
            val spanCount = layoutManager.spanCount
            spanSizeLookup.getSpanGroupIndex(position, spanCount) == spanSizeLookup.getSpanGroupIndex(lastPosition, spanCount)
        } else {
            position == lastPosition
        }
        if (isLastRow) {
            outRect.bottom = extraSpace
        }
    }
}
