package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import com.aureusapps.android.extensions.horizontalMargin
import com.aureusapps.android.extensions.horizontalPadding
import com.aureusapps.android.extensions.verticalMargin
import com.aureusapps.android.extensions.verticalPadding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    fun interface OnExpandStateChangeListener {
        fun onStateChanged(isExpanded: Boolean)
    }

    companion object {
        const val DIRECTION_VERTICAL = 0
        const val DIRECTION_HORIZONTAL = 1
        const val GRAVITY_LEFT = 0x01
        const val GRAVITY_CENTER_HORIZONTAL = 0x02
        const val GRAVITY_RIGHT = 0x04
        const val GRAVITY_TOP = 0x10
        const val GRAVITY_CENTER_VERTICAL = 0x20
        const val GRAVITY_BOTTOM = 0x40
        const val GRAVITY_CENTER = 0x22
    }

    private val stateChangeListeners = ArrayList<OnExpandStateChangeListener>()
    private val layoutHelper = ExpandableLayoutHelper(
        context, attrs, defStyleAttr, defStyleRes
    )
    private val displayRect = Rect()
    private val childRect = Rect()
    private var animator: ValueAnimator? = null
    private var maxContentWidth = 0
    private var maxContentHeight = 0

    // attributes
    var isExpanded = layoutHelper.expanded
        private set
    var expandDirection = layoutHelper.expandDirection
        set(value) {
            field = value
            requestLayout()
        }
    var animationDuration = layoutHelper.animationDuration
    var animationInterpolator = layoutHelper.animationInterpolator
    var contentGravity = layoutHelper.contentGravity
        set(value) {
            field = value
            requestLayout()
        }

    fun addExpandStateChangeListener(
        notifyStateChanged: Boolean = true, listener: OnExpandStateChangeListener
    ) {
        stateChangeListeners.add(listener)
        if (notifyStateChanged) {
            listener.onStateChanged(isExpanded)
        }
    }

    fun removeExpandStateChangeListener(listener: OnExpandStateChangeListener) {
        stateChangeListeners.remove(listener)
    }

    fun setExpanded(animate: Boolean) {
        setExpandedInternal(true, animate)
    }

    fun setCollapsed(animate: Boolean) {
        setExpandedInternal(false, animate)
    }

    private fun setExpandedInternal(expand: Boolean, animate: Boolean) {
        if (expand == isExpanded) return
        if (animate) {
            animateExpansion(expand)
        } else {
            isExpanded = expand
            notifyStateListeners(expand)
            animator = null
            requestLayout()
        }
    }

    private fun animateExpansion(expand: Boolean) {
        val fromExtent = if (expandDirection == DIRECTION_VERTICAL) height else width
        if (isLaidOut) {
            isExpanded = expand
            notifyStateListeners(expand)
            animateExpansion(expand, fromExtent)
        } else {
            doOnNextLayout {
                isExpanded = expand
                notifyStateListeners(expand)
                animateExpansion(expand, fromExtent)
            }
            requestLayout()
        }
    }

    private fun animateExpansion(expand: Boolean, fromExtent: Int) {
        val maxExtent = if (expandDirection == DIRECTION_VERTICAL) {
            maxContentHeight
        } else {
            maxContentWidth
        }
        val toExtent = if (expand) maxExtent else 0
        val expandExtent = abs(toExtent - fromExtent)
        val duration = if (maxExtent == 0) {
            animationDuration
        } else {
            animationDuration * expandExtent / maxExtent
        }
        startAnimation(fromExtent, toExtent, duration, animationInterpolator) { requestLayout() }
    }

    private fun notifyStateListeners(expand: Boolean) {
        stateChangeListeners.forEach { listener ->
            listener.onStateChanged(expand)
        }
    }

    fun toggleExpanded(animate: Boolean = true) {
        setExpandedInternal(!isExpanded, animate)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (animator?.isRunning == true) {
            val animatedValue = animator?.animatedValue as Int? ?: 0
            when (expandDirection) {
                DIRECTION_VERTICAL -> {
                    setMeasuredDimension(measuredWidth, animatedValue)
                }

                DIRECTION_HORIZONTAL -> {
                    setMeasuredDimension(animatedValue, measuredHeight)
                }
            }
        } else {
            // measure children
            var maxChildContentWidth = 0
            var maxChildContentHeight = 0
            for (child in children) {
                if (child.visibility != GONE) {
                    val childWidthMeasureSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        child.horizontalMargin + horizontalPadding,
                        child.layoutParams.width
                    )
                    val childHeightMeasureSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        child.verticalMargin + verticalPadding,
                        child.layoutParams.height
                    )
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                    maxChildContentWidth = max(
                        maxChildContentWidth,
                        child.measuredWidth + child.horizontalMargin + horizontalPadding
                    )
                    maxChildContentHeight = max(
                        maxChildContentHeight,
                        child.measuredHeight + child.verticalMargin + verticalPadding
                    )
                }
            }

            // measure layout
            val measuredWidth = getLayoutDimension(
                widthMeasureSpec, layoutParams.width, maxChildContentWidth
            )
            val measuredHeight = getLayoutDimension(
                heightMeasureSpec, layoutParams.height, maxChildContentHeight
            )
            maxContentWidth = measuredWidth
            maxContentHeight = measuredHeight
            when (expandDirection) {
                DIRECTION_VERTICAL -> {
                    setMeasuredDimension(measuredWidth, if (isExpanded) measuredHeight else 0)
                }

                DIRECTION_HORIZONTAL -> {
                    setMeasuredDimension(if (isExpanded) measuredWidth else 0, measuredHeight)
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
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
            displayRect.set(
                marginLeft + paddingLeft,
                marginTop + paddingTop,
                r - l - marginRight - paddingRight,
                b - t - marginBottom - paddingBottom
            )
            layoutHelper.applyGravity(
                contentGravity,
                displayRect,
                child.measuredWidth,
                child.measuredHeight,
                childRect
            )
            child.layout(
                childRect.left,
                childRect.top,
                childRect.right,
                childRect.bottom
            )
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        if (p is MarginLayoutParams) return p
        return MarginLayoutParams(p)
    }

    override fun checkLayoutParams(p: LayoutParams): Boolean {
        return p is MarginLayoutParams
    }

    private fun getLayoutDimension(
        measureSpec: Int, layoutParam: Int, maxChildSize: Int
    ): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> when (layoutParam) {
                WRAP_CONTENT -> min(maxChildSize, specSize)
                MATCH_PARENT -> specSize
                else -> min(layoutParam, specSize)
            }

            MeasureSpec.UNSPECIFIED -> when (layoutParam) {
                WRAP_CONTENT -> maxChildSize
                MATCH_PARENT -> max(maxChildSize, specSize)
                else -> layoutParam
            }

            else -> throw Exception("Unknown MeasureSpec mode")
        }
    }

    @Synchronized
    private fun startAnimation(
        from: Int, to: Int, duration: Long, interpolator: TimeInterpolator, callback: (Int) -> Unit
    ) {
        cancelAnimation()
        animator = ValueAnimator.ofInt(from, to).apply {
            setDuration(duration)
            setInterpolator(interpolator)
            addUpdateListener { animator ->
                callback(animator.animatedValue as Int)
            }
            start()
        }
    }

    @Synchronized
    private fun cancelAnimation() {
        val anim = animator
        if (anim != null && anim.isRunning) {
            anim.cancel()
        }
        animator = null
    }

}