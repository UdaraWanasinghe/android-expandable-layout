package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.children
import com.aureusapps.android.extensions.animate
import com.aureusapps.android.extensions.getEnum
import com.aureusapps.android.extensions.setHeight
import com.aureusapps.android.extensions.setWidth
import kotlinx.coroutines.Job
import kotlin.math.max

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

    @Suppress("MemberVisibilityCanBePrivate")
    var isExpanded: Boolean
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var animationDuration: Long

    @Suppress("MemberVisibilityCanBePrivate")
    var expandDirection: ExpandDirection = ExpandDirection.VERTICAL
        set(value) {
            field = value
            if (isLaidOut) {
                requestLayout()
            }
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var animationInterpolator: TimeInterpolator = DecelerateInterpolator(2f)

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0
    private val stateChangeListeners: ArrayList<OnStateChangeListener> = ArrayList()
    private var animationJob: Job? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout).apply {
            isExpanded = getBoolean(R.styleable.ExpandableLayout_expanded, false)
            animationDuration = getInteger(R.styleable.ExpandableLayout_duration, DEFAULT_DURATION).toLong()
            expandDirection = getEnum(R.styleable.ExpandableLayout_expandDirection, ExpandDirection.VERTICAL)
            recycle()
        }
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
    fun addStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.add(listener)
        listener.onStateChange(this, isExpanded)
    }

    @Suppress("unused")
    fun removeStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.remove(listener)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setExpanded(expand: Boolean, animate: Boolean = true) {
        if (isExpanded != expand) {
            isExpanded = expand
            stateChangeListeners.forEach {
                it.onStateChange(this, isExpanded)
            }
            if (animationJob?.isActive == true) {
                animationJob?.cancel()
                animationJob = null
            }
            if (animate) {
                if (expandDirection == ExpandDirection.VERTICAL) {
                    val currentHeight = height
                    animationJob = if (isExpanded) {
                        val expandHeight = maxHeight - currentHeight
                        val duration = animationDuration * expandHeight / maxHeight
                        animate(currentHeight, maxHeight, duration, animationInterpolator) { setHeight(it) }
                    } else {
                        val duration = animationDuration * currentHeight / maxHeight
                        animate(currentHeight, 0, duration, animationInterpolator) { setHeight(it) }
                    }
                } else {
                    val currentWidth = width
                    animationJob = if (isExpanded) {
                        val expandWidth = maxWidth - currentWidth
                        val duration = animationDuration * expandWidth / maxWidth
                        animate(currentWidth, maxWidth, duration, animationInterpolator) { setWidth(it) }
                    } else {
                        val duration = animationDuration * currentWidth / maxWidth
                        animate(currentWidth, 0, duration, animationInterpolator) { setWidth(it) }
                    }
                }
            } else {
                if (expandDirection == ExpandDirection.VERTICAL) {
                    setHeight(if (isExpanded) maxHeight else 0)
                } else {
                    setWidth(if (isExpanded) maxWidth else 0)
                }
            }
        }
    }

    @Suppress("unused")
    fun toggleExpanded(animate: Boolean = true) {
        setExpanded(!isExpanded, animate)
    }

}