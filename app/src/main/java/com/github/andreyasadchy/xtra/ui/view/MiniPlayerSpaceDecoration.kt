package com.github.andreyasadchy.xtra.ui.view

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/**
 * Adds empty space below the last row of a vertical list while the minimized player is shown,
 * so the list can be scrolled far enough that the minimized player doesn't cover its last items.
 */
class MiniPlayerSpaceDecoration(private val space: () -> Int) : RecyclerView.ItemDecoration() {

    // space each item view was last given, see onDraw
    private val appliedSpace = WeakHashMap<View, Int>()
    private val adapterObservers = WeakHashMap<RecyclerView, Pair<RecyclerView.Adapter<*>, RecyclerView.AdapterDataObserver>>()
    private val pendingInvalidations = Collections.newSetFromMap(WeakHashMap<RecyclerView, Boolean>())
    private val detachListeners = Collections.newSetFromMap(WeakHashMap<RecyclerView, Boolean>())

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        observeAdapter(parent)
        val extraSpace = getSpace(view, parent, state)
        appliedSpace[view] = extraSpace
        outRect.bottom = extraSpace
    }

    /**
     * RecyclerView keeps the offsets of items that aren't rebound, so when items are added, removed or moved
     * the previous last row would keep its space and the new last row could miss it.
     * Mark all offsets dirty before the layout that follows the change, so it's drawn correctly the first time.
     */
    private fun observeAdapter(recyclerView: RecyclerView) {
        val adapter = recyclerView.adapter
        val current = adapterObservers[recyclerView]
        if (current?.first === adapter) {
            return
        }
        current?.let { (oldAdapter, observer) -> oldAdapter.unregisterAdapterDataObserver(observer) }
        if (adapter == null) {
            adapterObservers.remove(recyclerView)
            return
        }
        val recyclerViewRef = WeakReference(recyclerView)
        val observer = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = invalidate()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = invalidate()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = invalidate()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = invalidate()

            private fun invalidate() {
                val view = recyclerViewRef.get()
                if (view == null) {
                    adapter.unregisterAdapterDataObserver(this)
                } else if (space() > 0) {
                    invalidateItemDecorations(view)
                }
            }
        }
        adapter.registerAdapterDataObserver(observer)
        adapterObservers[recyclerView] = adapter to observer
        if (detachListeners.add(recyclerView)) {
            // the map value holds the adapter, and adapters can hold their RecyclerView (like PagedListFragment's observer),
            // so the weak key alone wouldn't let a destroyed list be collected. Forget the adapter when the list leaves
            // the screen, it's observed again on its next layout
            recyclerView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}

                override fun onViewDetachedFromWindow(v: View) {
                    adapterObservers.remove(recyclerView)?.let { (oldAdapter, oldObserver) ->
                        oldAdapter.unregisterAdapterDataObserver(oldObserver)
                    }
                }
            })
        }
    }

    private fun invalidateItemDecorations(recyclerView: RecyclerView) {
        if (!recyclerView.isComputingLayout) {
            recyclerView.invalidateItemDecorations()
        } else if (pendingInvalidations.add(recyclerView)) {
            recyclerView.post {
                pendingInvalidations.remove(recyclerView)
                recyclerView.invalidateItemDecorations()
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // backstop for anything the adapter observer doesn't catch, like an adapter swapped in after the last layout.
        // Views animating out keep stale positions, so wait until animations finish (removing them redraws the list).
        // When the space is 0, MainActivity.setMiniPlayerSpace has already refreshed every list
        if (space() <= 0 || parent.itemAnimator?.isRunning == true || parent in pendingInvalidations) {
            return
        }
        observeAdapter(parent)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as? RecyclerView.LayoutParams ?: continue
            if (layoutParams.isItemRemoved || layoutParams.isViewInvalid) {
                continue
            }
            if ((appliedSpace[child] ?: 0) != getSpace(child, parent, state)) {
                pendingInvalidations.add(parent)
                parent.post {
                    pendingInvalidations.remove(parent)
                    parent.invalidateItemDecorations()
                }
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
            if (spanSizeLookup is GridLayoutManager.DefaultSpanSizeLookup) {
                // the default lookup counts rows from the start of the list each time, every item takes one span here
                position / spanCount == lastPosition / spanCount
            } else {
                spanSizeLookup.getSpanGroupIndex(position, spanCount) == spanSizeLookup.getSpanGroupIndex(lastPosition, spanCount)
            }
        } else {
            position == lastPosition
        }
        return if (isLastRow) extraSpace else 0
    }
}
