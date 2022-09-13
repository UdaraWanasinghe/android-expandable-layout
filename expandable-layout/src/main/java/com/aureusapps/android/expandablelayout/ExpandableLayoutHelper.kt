package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.animation.*
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

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout, defStyleAttr, defStyleRes).apply {
            expanded = getBoolean(R.styleable.ExpandableLayout_expanded, false)
            expandDirection = getEnum(R.styleable.ExpandableLayout_expandDirection, ExpandableLayout.ExpandDirection.VERTICAL)
            duration = getInteger(R.styleable.ExpandableLayout_duration, 300).toLong()
            interpolator = getInterpolator(R.styleable.ExpandableLayout_interpolator)
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

}