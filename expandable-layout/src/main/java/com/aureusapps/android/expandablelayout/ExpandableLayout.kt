package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.aureusapps.android.extensions.getHorizontalMargin
import com.aureusapps.android.extensions.getVerticalMargin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

class ExpandableLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : LifecycleAwareViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    interface OnExpandStateChangeListener {
        fun onStateChanged(expandableLayout: ExpandableLayout, isExpanded: Boolean)
    }

    private data class ExpandTask(
        val expand: Boolean, val animate: Boolean
    )

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
    private val expandTaskChannel = Channel<ExpandTask>()
    private val layoutHelper = ExpandableLayoutHelper(context, attrs, defStyleAttr, defStyleRes)
    private var expandTaskFlowJob: Job? = null
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

    override fun onCreate(owner: LifecycleOwner) {
        cancelPreviousAndLaunchExpandTaskFlow()
    }

    private fun cancelPreviousAndLaunchExpandTaskFlow() {
        cancelExpandTaskFlowJob()
        expandTaskFlowJob = lifecycleScope?.launch {
            expandTaskChannel.receiveAsFlow().onStart {
                emit(ExpandTask(isExpanded, false))
            }.filter { task ->
                task.expand != isExpanded
            }.collectLatest { task ->
                val animate = task.animate
                val expand = task.expand
                isExpanded = expand
                stateChangeListeners.forEach {
                    it.onStateChanged(this@ExpandableLayout, expand)
                }
                if (animate) {
                    if (expandDirection == DIRECTION_VERTICAL) {
                        val currentHeight = height
                        val maxHeight = getMaxHeight()
                        if (expand) {
                            val expandHeight = maxHeight - currentHeight
                            val duration = animationDuration * expandHeight / maxHeight
                            animate(
                                currentHeight, maxHeight, duration, animationInterpolator
                            ) { requestLayout() }
                        } else {
                            val duration = animationDuration * currentHeight / maxHeight
                            animate(
                                currentHeight, 0, duration, animationInterpolator
                            ) { requestLayout() }
                        }
                    } else {
                        val currentWidth = width
                        val maxWidth = getMaxWidth()
                        if (expand) {
                            val expandWidth = maxWidth - currentWidth
                            val duration = animationDuration * expandWidth / maxWidth
                            animate(
                                currentWidth, maxWidth, duration, animationInterpolator
                            ) { requestLayout() }
                        } else {
                            val duration = animationDuration * currentWidth / maxWidth
                            animate(
                                currentWidth, 0, duration, animationInterpolator
                            ) { requestLayout() }
                        }
                    }
                } else {
                    animator = null
                    requestLayout()
                }
            }
        }
    }

    private fun cancelExpandTaskFlowJob() {
        if (expandTaskFlowJob?.isActive == true) {
            expandTaskFlowJob?.cancel()
        }
    }

    private suspend fun getMaxWidth(): Int {
        return if (isLayoutRequested) {
            suspendCancellableCoroutine { continuation ->
                doOnLayout {
                    continuation.resume(maxContentWidth)
                }
            }
        } else {
            maxContentWidth
        }
    }

    private suspend fun getMaxHeight(): Int {
        return if (isLayoutRequested) {
            suspendCancellableCoroutine { continuation ->
                doOnLayout {
                    continuation.resume(maxContentHeight)
                }
            }
        } else {
            maxContentHeight
        }
    }

    private suspend fun animate(
        from: Int, to: Int, duration: Long, interpolator: TimeInterpolator, callback: (Int) -> Unit
    ) {
        animator = ValueAnimator.ofInt(from, to).apply {
            this.duration = duration
            this.interpolator = interpolator
            this.addUpdateListener {
                callback(animatedValue as Int)
            }
        }
        try {
            animator?.start()
            delay(duration)
        } catch (e: CancellationException) {
            animator?.cancel()
        }
    }

    private inline fun <reified T> ValueAnimator.animatedValue(): T {
        return animatedValue as T
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (animator?.isRunning == true) {
            val animatedValue = animator?.animatedValue<Int>() ?: 0
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
                        child.getHorizontalMargin() + paddingLeft + paddingRight,
                        child.layoutParams.width
                    )
                    val childHeightMeasureSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        child.getVerticalMargin() + paddingTop + paddingBottom,
                        child.layoutParams.height
                    )
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                    maxChildContentWidth =
                        max(maxChildContentWidth, child.measuredWidth + child.getHorizontalMargin())
                    maxChildContentHeight =
                        max(maxChildContentHeight, child.measuredHeight + child.getVerticalMargin())
                }
            }

            // measure layout
            val measuredWidth =
                getLayoutDimension(widthMeasureSpec, layoutParams.width, maxChildContentWidth)
            val measuredHeight =
                getLayoutDimension(heightMeasureSpec, layoutParams.height, maxChildContentHeight)
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

    private fun getLayoutDimension(
        measureSpec: Int, layoutParam: Int, maxChildSize: Int
    ): Int {
        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.AT_MOST -> {
                when (layoutParam) {
                    WRAP_CONTENT -> {
                        min(maxChildSize, MeasureSpec.getSize(measureSpec))
                    }
                    MATCH_PARENT -> {
                        MeasureSpec.getSize(measureSpec)
                    }
                    else -> {
                        min(layoutParam, MeasureSpec.getSize(measureSpec))
                    }
                }
            }
            MeasureSpec.EXACTLY -> {
                MeasureSpec.getSize(measureSpec)
            }
            MeasureSpec.UNSPECIFIED -> {
                when (layoutParam) {
                    WRAP_CONTENT -> {
                        maxChildSize
                    }
                    MATCH_PARENT -> {
                        max(maxChildSize, MeasureSpec.getSize(measureSpec))
                    }
                    else -> {
                        layoutParam
                    }
                }
            }
            else -> {
                throw Exception("Unknown MeasureSpec mode")
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
                contentGravity, displayRect, child.measuredWidth, child.measuredHeight, childRect
            )
            child.layout(
                childRect.left, childRect.top, childRect.right, childRect.bottom
            )
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    fun addExpandStateChangeListener(listener: OnExpandStateChangeListener) {
        stateChangeListeners.add(listener)
        listener.onStateChanged(this, isExpanded)
    }

    fun removeExpandStateChangeListener(listener: OnExpandStateChangeListener) {
        stateChangeListeners.remove(listener)
    }

    fun setExpanded(expand: Boolean, animate: Boolean = true) {
        lifecycleScope?.launch {
            expandTaskChannel.send(ExpandTask(expand, animate))
        } ?: run {
            isExpanded = expand
            requestLayout()
        }
    }

    fun toggleExpanded(animate: Boolean = true) {
        setExpanded(!isExpanded, animate)
    }

}