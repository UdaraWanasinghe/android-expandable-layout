package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import com.aureusapps.android.extensions.getEnum
import kotlin.math.max

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    enum class ExpandDirection {
        HORIZONTAL,
        VERTICAL
    }

    interface OnStateChangeListener {
        fun onStateChange(expandableLayout: ExpandableLayout, isExpanded: Boolean)
    }

    companion object {
        private const val DEFAULT_DURATION = 300
    }

    // attributes
    var isExpanded: Boolean
        private set
    var animationDuration: Long
    var expandDirection: ExpandDirection = ExpandDirection.VERTICAL
        set(value) {
            field = value
            if (isLaidOut) {
                requestLayout()
            }
        }
    var animationInterpolator: TimeInterpolator = DecelerateInterpolator(2f)

    // local
    private var maxWidth: Int = 0
    private var maxHeight: Int = 0
    private val stateChangeListeners: ArrayList<OnStateChangeListener> = ArrayList()
    private var expandAnimator: ValueAnimator? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout).apply {
            isExpanded = getBoolean(R.styleable.ExpandableLayout_expanded, false)
            animationDuration = getInteger(R.styleable.ExpandableLayout_duration, DEFAULT_DURATION).toLong()
            expandDirection = getEnum(R.styleable.ExpandableLayout_expandDirection, ExpandDirection.VERTICAL)
            recycle()
        }
    }

    fun setExpanded(expanded: Boolean) {
        if (this.isExpanded != expanded) {
            this.isExpanded = expanded
            val layoutParams = layoutParams
            if (expandDirection == ExpandDirection.VERTICAL) {
                layoutParams.height = if (expanded) maxHeight else 0
            } else {
                layoutParams.width = if (expanded) maxWidth else 0
            }
            setLayoutParams(layoutParams)
        }
    }

    fun addStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.add(listener)
        listener.onStateChange(this, isExpanded)
    }

    fun removeStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.remove(listener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // measure children
        for (child in children) {
            var marginLeft = 0
            var marginTop = 0
            var marginRight = 0
            var marginBottom = 0
            val childLayoutParams = child.layoutParams
            if (childLayoutParams is MarginLayoutParams) {
                marginLeft = childLayoutParams.leftMargin
                marginTop = childLayoutParams.topMargin
                marginRight = childLayoutParams.rightMargin
                marginBottom = childLayoutParams.bottomMargin
            }
            if (child.visibility != GONE) {
                val childWidthSpec = when (expandDirection) {
                    ExpandDirection.HORIZONTAL -> {
                        MeasureSpec.makeMeasureSpec(
                            0,
                            MeasureSpec.UNSPECIFIED
                        )
                    }
                    ExpandDirection.VERTICAL -> {
                        getChildMeasureSpec(
                            widthMeasureSpec,
                            paddingLeft + paddingRight + marginLeft + marginRight,
                            child.layoutParams.width
                        )
                    }
                }
                val childHeightSpec = when (expandDirection) {
                    ExpandDirection.HORIZONTAL -> {
                        getChildMeasureSpec(
                            heightMeasureSpec,
                            paddingTop + paddingBottom + marginTop + marginBottom,
                            child.layoutParams.height
                        )
                    }
                    ExpandDirection.VERTICAL -> {
                        MeasureSpec.makeMeasureSpec(
                            0,
                            MeasureSpec.UNSPECIFIED
                        )
                    }
                }
                child.measure(childWidthSpec, childHeightSpec)
                maxWidth = max(maxWidth, child.measuredWidth + paddingLeft + paddingRight + marginLeft + marginRight)
                maxHeight = max(maxHeight, child.measuredHeight + paddingTop + paddingBottom + marginTop + marginBottom)
            }
        }
        // measure parent
        when (expandDirection) {
            ExpandDirection.VERTICAL -> {
                val widthMode = MeasureSpec.getMode(widthMeasureSpec)
                val exactWidth = if (widthMode == MeasureSpec.AT_MOST) {
                    maxWidth
                } else {
                    MeasureSpec.getSize(widthMeasureSpec)
                }
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                val exactHeight = if (heightMode == MeasureSpec.EXACTLY) {
                    MeasureSpec.getSize(heightMeasureSpec)
                } else {
                    if (isExpanded) maxHeight else 0
                }
                setMeasuredDimension(exactWidth, exactHeight)
            }
            ExpandDirection.HORIZONTAL -> {
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                val exactHeight = if (heightMode == MeasureSpec.AT_MOST) {
                    maxHeight
                } else {
                    MeasureSpec.getSize(heightMeasureSpec)
                }
                val widthMode = MeasureSpec.getMode(widthMeasureSpec)
                val exactWidth = if (widthMode == MeasureSpec.EXACTLY) {
                    MeasureSpec.getSize(widthMeasureSpec)
                } else {
                    if (isExpanded) maxWidth else 0
                }
                setMeasuredDimension(exactWidth, exactHeight)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (child in children) {
            var marginLeft = 0
            var marginTop = 0
            val childLayoutParams = child.layoutParams
            if (childLayoutParams is MarginLayoutParams) {
                marginLeft = childLayoutParams.leftMargin
                marginTop = childLayoutParams.topMargin
            }
            child.layout(
                paddingLeft + marginLeft,
                paddingTop + marginTop,
                child.measuredWidth + paddingLeft + marginLeft,
                child.measuredHeight + paddingTop + marginTop
            )
        }
    }

    @Suppress("unused")
    fun setInterpolator(interpolator: TimeInterpolator) {
        this.animationInterpolator = interpolator
    }

    @Suppress("unused")
    fun toggleExpanded() {
        isExpanded = !isExpanded
        stateChangeListeners.forEach {
            it.onStateChange(this, isExpanded)
        }
        if (isExpanded) {
            if (expandDirection == ExpandDirection.VERTICAL) {
                animateValue(0, maxHeight) { params, value -> params.height = value }
            } else {
                animateValue(0, maxWidth) { params, value -> params.width = value }
            }
        } else {
            if (expandDirection == ExpandDirection.VERTICAL) {
                animateValue(maxHeight, 0) { params, value -> params.height = value }
            } else {
                animateValue(maxWidth, 0) { params, value -> params.width = value }
            }
        }
    }

    private fun animateValue(fromValue: Int, toValue: Int, update: (LayoutParams, Int) -> Unit) {
        expandAnimator?.cancel()
        val animator = ValueAnimator.ofInt(fromValue, toValue)
        animator.duration = this.animationDuration
        animator.interpolator = animationInterpolator
        animator.addUpdateListener {
            val params = layoutParams
            update(params, it.animatedValue as Int)
            layoutParams = params
        }
        animator.doOnEnd {
            this.expandAnimator = null
        }
        animator.start()
        this.expandAnimator = animator
    }

}