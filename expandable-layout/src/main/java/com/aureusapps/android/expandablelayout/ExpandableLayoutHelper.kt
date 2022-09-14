package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.util.AttributeSet
import android.util.LayoutDirection
import android.view.animation.*
import androidx.core.view.GravityCompat
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.BOTTOM
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.LEFT
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.RIGHT
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.TOP
import com.aureusapps.android.extensions.getEnum

internal class ExpandableLayoutHelper(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) {

    val expanded: Boolean
    val duration: Long
    val expandDirection: ExpandableLayout.ExpandDirection
    val interpolator: TimeInterpolator
    val gravity: Int

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout, defStyleAttr, defStyleRes).apply {
            expanded = getBoolean(R.styleable.ExpandableLayout_expanded, false)
            expandDirection = getEnum(R.styleable.ExpandableLayout_expandDirection, ExpandableLayout.ExpandDirection.VERTICAL)
            duration = getInteger(R.styleable.ExpandableLayout_duration, 300).toLong()
            interpolator = getInterpolator(R.styleable.ExpandableLayout_interpolator)
            gravity = getInt(R.styleable.ExpandableLayout_gravity, TOP or LEFT)
                .let { GravityCompat.getAbsoluteGravity(it, LayoutDirection.LOCALE) }
            recycle()
        }
    }

    private fun TypedArray.getInterpolator(index: Int): TimeInterpolator {
        return when (getInt(index, 6)) {
            0 -> AccelerateDecelerateInterpolator()
            1 -> AccelerateInterpolator()
            2 -> AnticipateInterpolator()
            3 -> AnticipateOvershootInterpolator()
            4 -> BounceInterpolator()
            5 -> CycleInterpolator(1f)
            6 -> DecelerateInterpolator()
            7 -> OvershootInterpolator()
            else -> LinearInterpolator()
        }
    }

    fun applyGravity(gravity: Int, displayRect: Rect, outRect: Rect, width: Int, height: Int) {
        val left = if (gravity and LEFT != 0) {
            displayRect.left
        } else if (gravity and RIGHT != 0) {
            displayRect.right - width
        } else {
            displayRect.left + (displayRect.width() - width) / 2
        }
        val top = if (gravity and TOP != 0) {
            displayRect.top
        } else if (gravity and BOTTOM != 0) {
            displayRect.bottom - height
        } else {
            displayRect.top + (displayRect.height() - height) / 2
        }
        outRect.set(left, top, left + width, top + height)
    }

}