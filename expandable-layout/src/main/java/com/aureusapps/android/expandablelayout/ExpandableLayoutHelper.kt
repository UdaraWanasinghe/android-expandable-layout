package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.util.AttributeSet
import android.view.animation.*
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.DIRECTION_VERTICAL
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.GRAVITY_BOTTOM
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.GRAVITY_LEFT
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.GRAVITY_RIGHT
import com.aureusapps.android.expandablelayout.ExpandableLayout.Companion.GRAVITY_TOP

internal class ExpandableLayoutHelper(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) {

    val expanded: Boolean
    val animationDuration: Long
    val expandDirection: Int
    val animationInterpolator: TimeInterpolator
    val contentGravity: Int

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ExpandableLayout,
            defStyleAttr,
            defStyleRes
        ).apply {
            expanded = getBoolean(R.styleable.ExpandableLayout_expanded, false)
            expandDirection =
                getInt(R.styleable.ExpandableLayout_expandDirection, DIRECTION_VERTICAL)
            animationDuration = getInt(R.styleable.ExpandableLayout_animationDuration, 300).toLong()
            animationInterpolator =
                getInterpolator(R.styleable.ExpandableLayout_animationInterpolator)
            contentGravity =
                getInt(R.styleable.ExpandableLayout_contentGravity, GRAVITY_TOP or GRAVITY_LEFT)
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

    fun applyGravity(
        gravity: Int,
        displayRect: Rect,
        width: Int,
        height: Int,
        outRect: Rect
    ) {
        val left = if (gravity and GRAVITY_LEFT != 0) {
            displayRect.left
        } else if (gravity and GRAVITY_RIGHT != 0) {
            displayRect.right - width
        } else {
            displayRect.left + (displayRect.width() - width) / 2
        }
        val top = if (gravity and GRAVITY_TOP != 0) {
            displayRect.top
        } else if (gravity and GRAVITY_BOTTOM != 0) {
            displayRect.bottom - height
        } else {
            displayRect.top + (displayRect.height() - height) / 2
        }
        outRect.set(left, top, left + width, top + height)
    }

}