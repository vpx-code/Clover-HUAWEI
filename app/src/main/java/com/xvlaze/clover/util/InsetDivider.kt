package com.xvlaze.clover.util

import android.content.Context
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

internal class InsetDivider(context: Context, private val insetDividerLeft: Int) :
    ItemDecoration() {/*
    private val dividerDrawable: Drawable?
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        if (parent.layoutManager == null || dividerDrawable == null) {
            return
        }
        val left = parent.paddingLeft + insetDividerLeft
        val right = parent.width - parent.paddingRight
        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + dividerDrawable.intrinsicHeight
            dividerDrawable.setBounds(left, top, right, bottom)
            dividerDrawable.draw(c)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (dividerDrawable == null) {
            outRect[0, 0, 0] = 0
        } else {
            outRect[0, 0, 0] = dividerDrawable.intrinsicHeight
        }
    }

    init {
        val attributesArray = intArrayOf(R.attr.listDivider)
        val typedArray = context.obtainStyledAttributes(attributesArray)
        dividerDrawable = typedArray.getDrawable(0)
        if (dividerDrawable == null) {
            Log.w("InsetDivider", "@android:attr/listDivider was not set in the theme used here")
        }
        typedArray.recycle()
    }*/
}