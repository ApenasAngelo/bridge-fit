package com.apenasangelo.bridgefit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Draws the rows of an upstream preference screen as iOS grouped lists: each run of rows between two categories
 * sits on one rounded card, inset from the screen edges, with hairline separators between rows.
 *
 * Added at runtime by [BridgeFitBootstrap], so the upstream preference XML and fragments stay untouched.
 */
class GroupedPreferenceDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val density = context.resources.displayMetrics.density
    private val inset = (16 * density).toInt()
    private val radius = 12 * density
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.themeColor(com.google.android.material.R.attr.colorSurfaceContainerLow)
    }
    private val separatorPaint = Paint().apply {
        color = context.themeColor(com.google.android.material.R.attr.colorOutlineVariant)
        strokeWidth = (0.5f * density).coerceAtLeast(1f)
    }
    private val path = Path()
    private val rect = RectF()

    private fun isCategory(parent: RecyclerView, position: Int): Boolean {
        val adapter = parent.adapter as? PreferenceGroupAdapter ?: return false
        if (position < 0 || position >= adapter.itemCount) return true
        return adapter.getItem(position) is PreferenceCategory
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        outRect.left = inset
        outRect.right = inset
        if (!isCategory(parent, position) && isCategory(parent, position + 1)) {
            outRect.bottom = (8 * density).toInt()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || isCategory(parent, position)) continue
            val first = isCategory(parent, position - 1)
            val last = isCategory(parent, position + 1)
            val top = child.top + child.translationY
            val bottom = child.bottom + child.translationY
            rect.set(child.left.toFloat(), top, child.right.toFloat(), bottom)
            val tr = if (first) radius else 0f
            val br = if (last) radius else 0f
            path.reset()
            path.addRoundRect(rect, floatArrayOf(tr, tr, tr, tr, br, br, br, br), Path.Direction.CW)
            c.drawPath(path, cardPaint)
            if (!last) {
                c.drawLine(child.left + inset.toFloat(), bottom, child.right.toFloat(), bottom, separatorPaint)
            }
        }
    }
}

private fun Context.themeColor(attr: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(attr, value, true)
    return value.data
}
