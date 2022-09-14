package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.aureusapps.android.extensions.setHeight
import com.aureusapps.android.extensions.setWidth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes), DefaultLifecycleObserver {

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

    private var maxWidth: Int = -1
    private var maxHeight: Int = -1
    private val stateChangeListeners: ArrayList<OnStateChangeListener> = ArrayList()
    private val expandTaskChannel = Channel<ExpandTask>()

    private val layoutHelper = ExpandableLayoutHelper(context, attrs, defStyleAttr, defStyleRes)

    // attributes
    var expanded: Boolean = layoutHelper.expanded
        private set
    var expandDirection: ExpandDirection = layoutHelper.expandDirection
        set(value) {
            field = value
            requestLayout()
        }
    var animationDuration: Long = layoutHelper.animationDuration
    var animationInterpolator: TimeInterpolator = layoutHelper.animationInterpolator
    var contentGravity: Int = layoutHelper.contentGravity

    private var lifecycleOwner: LifecycleOwner? = null
    private var expandTaskFlowJob: Job? = null
    private val displayRect = Rect()
    private val childRect = Rect()


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateLifecycleOwner()
    }

    private fun updateLifecycleOwner() {
        val newLifecycleOwner = findViewTreeLifecycleOwner()
        if (lifecycleOwner != newLifecycleOwner) {
            // cancel last job
            if (expandTaskFlowJob?.isActive == true) {
                expandTaskFlowJob?.cancel()
            }
            // remove last observer
            lifecycleOwner?.lifecycle?.removeObserver(this)
            // subscribe to the new lifecycle owner
            newLifecycleOwner?.lifecycle?.addObserver(this)
            lifecycleOwner = newLifecycleOwner
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        expandTaskFlowJob = owner.lifecycleScope.launch {
            launchExpandTaskFlow()
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
                            animate(currentHeight, maxHeight, duration, animationInterpolator) { setHeight(it) }
                        } else {
                            val duration = animationDuration * currentHeight / maxHeight
                            animate(currentHeight, 0, duration, animationInterpolator) { setHeight(it) }
                        }
                    } else {
                        val currentWidth = width
                        val maxWidth = getMaxWidth()
                        if (expand) {
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
                        val maxHeight = getMaxHeight()
                        setHeight(if (expand) maxHeight else 0)
                    } else {
                        val maxWidth = getMaxWidth()
                        setWidth(if (expand) maxWidth else 0)
                    }
                }
            }
    }

    private suspend fun getMaxWidth(): Int {
        if (maxWidth > -1) return maxWidth
        return suspendCoroutine { continuation ->
            doOnLayout {
                continuation.resume(maxWidth)
            }
        }
    }

    private suspend fun getMaxHeight(): Int {
        if (maxHeight > -1) return maxHeight
        return suspendCoroutine { continuation ->
            doOnLayout {
                continuation.resume(maxHeight)
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
        val animator = ValueAnimator.ofInt(from, to)
            .apply {
                this.duration = duration
                this.interpolator = interpolator
                this.addUpdateListener {
                    callback(animatedValue as Int)
                }
            }
        try {
            animator.start()
            delay(duration)
        } catch (e: CancellationException) {
            animator.cancel()
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
                    if (expanded) maxHeight else 0
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
                    if (expanded) maxWidth else 0
                }
                setMeasuredDimension(exactWidth, exactHeight)
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
        lifecycleOwner?.let { owner ->
            owner.lifecycleScope.launch {
                expandTaskChannel.send(ExpandTask(expand, animate))
            }
        } ?: run {
            expanded = expand
        }
    }

    fun toggleExpanded(animate: Boolean = true) {
        setExpanded(!expanded, animate)
    }

}