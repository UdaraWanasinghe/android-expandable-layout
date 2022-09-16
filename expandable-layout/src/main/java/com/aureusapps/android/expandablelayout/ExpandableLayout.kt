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
import androidx.lifecycle.lifecycleScope
import com.aureusapps.android.extensions.getHorizontalMargin
import com.aureusapps.android.extensions.getVerticalMargin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min

class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LifecycleAwareViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        internal const val LEFT = 0x01
        internal const val CENTER_HORIZONTAL = 0x02
        internal const val RIGHT = 0x04
        internal const val TOP = 0x10
        internal const val CENTER_VERTICAL = 0x20
        internal const val BOTTOM = 0x40
        internal const val CENTER = 0x22
    }

    enum class Gravity(val value: Int) {
        LEFT(ExpandableLayout.LEFT),
        CENTER_HORIZONTAL(ExpandableLayout.CENTER_HORIZONTAL),
        RIGHT(ExpandableLayout.RIGHT),
        TOP(ExpandableLayout.TOP),
        CENTER_VERTICAL(ExpandableLayout.CENTER_VERTICAL),
        BOTTOM(ExpandableLayout.BOTTOM),
        CENTER(ExpandableLayout.CENTER)
    }

    enum class ExpandDirection {
        HORIZONTAL,
        VERTICAL
    }

    interface OnStateChangeListener {
        fun onStateChanged(expandableLayout: ExpandableLayout, isExpanded: Boolean)
    }

    private data class ExpandTask(val expand: Boolean, val animate: Boolean)

    private data class ExpandState(
        val expandTask: ExpandTask? = null,
        val previousState: ExpandState? = null
    ) {
        companion object {
            val INITIAL = ExpandState()
        }
    }

    private val stateChangeListeners = ArrayList<OnStateChangeListener>()
    private val expandTaskChannel = Channel<ExpandTask>()

    private val layoutHelper = ExpandableLayoutHelper(context, attrs, defStyleAttr, defStyleRes)

    // attributes
    var expanded = layoutHelper.expanded
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

    private var expandTaskFlowJob: Job? = null
    private val displayRect = Rect()
    private val childRect = Rect()
    private var animator: ValueAnimator? = null
    private var maxContentWidth = 0
    private var maxContentHeight = 0

    override fun onCreate(owner: LifecycleOwner) {
        cancelExpandTaskFlowJob()
        expandTaskFlowJob = owner.lifecycleScope.launch {
            launchExpandTaskFlow()
        }
    }

    private fun cancelExpandTaskFlowJob() {
        if (expandTaskFlowJob?.isActive == true) {
            expandTaskFlowJob?.cancel()
        }
    }

    private suspend fun launchExpandTaskFlow() {
        expandTaskChannel.receiveAsFlow()
            .onStart {
                emit(ExpandTask(expanded, false))
            }
            .scan(ExpandState.INITIAL) { previousState, task ->
                ExpandState(task, previousState)
            }
            .filter { state ->
                val lastExpandedState = state.previousState?.expandTask?.expand
                val nextExpandState = state.expandTask?.expand
                lastExpandedState != nextExpandState
            }
            .mapNotNull { it.expandTask }
            .collectLatest { task ->
                val animate = task.animate
                val expand = task.expand
                expanded = expand
                stateChangeListeners.forEach {
                    it.onStateChanged(this@ExpandableLayout, expand)
                }
                if (animate) {
                    if (expandDirection == ExpandDirection.VERTICAL) {
                        val currentHeight = height
                        val maxHeight = getMaxHeight()
                        if (expand) {
                            val expandHeight = maxHeight - currentHeight
                            val duration = animationDuration * expandHeight / maxHeight
                            animate(currentHeight, maxHeight, duration, animationInterpolator) { requestLayout() }
                        } else {
                            val duration = animationDuration * currentHeight / maxHeight
                            animate(currentHeight, 0, duration, animationInterpolator) { requestLayout() }
                        }
                    } else {
                        val currentWidth = width
                        val maxWidth = getMaxWidth()
                        if (expand) {
                            val expandWidth = maxWidth - currentWidth
                            val duration = animationDuration * expandWidth / maxWidth
                            animate(currentWidth, maxWidth, duration, animationInterpolator) { requestLayout() }
                        } else {
                            val duration = animationDuration * currentWidth / maxWidth
                            animate(currentWidth, 0, duration, animationInterpolator) { requestLayout() }
                        }
                    }
                } else {
                    animator = null
                    requestLayout()
                }
            }
    }

    private suspend fun getMaxWidth(): Int {
        if (maxContentWidth > 0) return maxContentWidth
        return suspendCoroutine { continuation ->
            doOnLayout {
                continuation.resume(maxContentWidth)
            }
        }
    }

    private suspend fun getMaxHeight(): Int {
        if (maxContentHeight > 0) return maxContentHeight
        return suspendCoroutine { continuation ->
            doOnLayout {
                continuation.resume(maxContentHeight)
            }
        }
    }

    private suspend fun animate(
        from: Int,
        to: Int,
        duration: Long,
        interpolator: TimeInterpolator,
        callback: (Int) -> Unit
    ) {
        animator = ValueAnimator.ofInt(from, to)
            .apply {
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
        if (animator?.isRunning == true || !expanded) {
            val animatedValue = animator?.animatedValue<Int>() ?: 0
            when (expandDirection) {
                ExpandDirection.VERTICAL -> {
                    setMeasuredDimension(measuredWidth, animatedValue)
                }
                ExpandDirection.HORIZONTAL -> {
                    setMeasuredDimension(animatedValue, measuredHeight)
                }
            }

        } else {
            var maxChildWidth = 0
            var maxChildHeight = 0
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
                    maxChildWidth = max(maxChildWidth, child.measuredWidth)
                    maxChildHeight = max(maxChildHeight, child.measuredHeight)
                }
            }
            measureChildren(widthMeasureSpec, heightMeasureSpec)
            val measuredWidth = getLayoutDimension(widthMeasureSpec, layoutParams.width, maxChildWidth)
            val measuredHeight = getLayoutDimension(heightMeasureSpec, layoutParams.height, maxChildHeight)
            maxContentWidth = measuredWidth
            maxContentHeight = measuredHeight
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }

    private fun getLayoutDimension(
        measureSpec: Int,
        layoutParam: Int,
        maxChildSize: Int
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

    fun setContentGravity(gravity: Gravity) {
        this.contentGravity = gravity.value
        requestLayout()
    }

    fun getContentGravity(): Gravity {
        return Gravity.values().first { it.value == contentGravity }
    }

    fun addStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.add(listener)
        listener.onStateChanged(this, expanded)
    }

    fun removeStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.remove(listener)
    }

    fun setExpanded(expand: Boolean, animate: Boolean = true) {
        lifecycleScope?.launch {
            expandTaskChannel.send(ExpandTask(expand, animate))
        } ?: run {
            expanded = expand
        }
    }

    fun toggleExpanded(animate: Boolean = true) {
        setExpanded(!expanded, animate)
    }

}