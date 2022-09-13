package com.aureusapps.android.expandablelayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnAttach
import com.aureusapps.android.extensions.lifecycleScope
import com.aureusapps.android.extensions.setHeight
import com.aureusapps.android.extensions.setWidth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max

class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    enum class ExpandDirection {
        HORIZONTAL,
        VERTICAL
    }

    interface OnStateChangeListener {
        fun onStateChange(expandableLayout: ExpandableLayout, isExpanded: Boolean)
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

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0
    private val stateChangeListeners: ArrayList<OnStateChangeListener> = ArrayList()
    private val expandTaskChannel = Channel<ExpandTask>()

    private val layoutHelper = ExpandableLayoutHelper(context, attrs, defStyleAttr, defStyleRes)

    // attributes
    private var expanded: Boolean = layoutHelper.expanded
    private var expandDirection: ExpandDirection = layoutHelper.expandDirection
    private var duration: Long = layoutHelper.duration
    private var interpolator: TimeInterpolator = layoutHelper.interpolator

    init {
        launchExpandTaskFlow()
    }

    private fun launchExpandTaskFlow() {
        expandTaskChannel.receiveAsFlow()
            .onStart {
                emit(ExpandTask(expanded, false))
            }
            .scan(ExpandState.INITIAL) { previousState, task ->
                ExpandState(task, previousState)
            }
            .filter { state ->
                state.previousState?.expandTask?.expand != state.expandTask?.expand
            }
            .mapNotNull { it.expandTask }
            .launch()
    }

    private fun Flow<ExpandTask>.launch() {
        doOnAttach {
            lifecycleScope.launch {
                collectLatest { task ->
                    val animate = task.animate
                    val expand = task.expand
                    expanded = expand
                    stateChangeListeners.forEach {
                        it.onStateChange(this@ExpandableLayout, expand)
                    }
                    if (animate) {
                        if (expandDirection == ExpandDirection.VERTICAL) {
                            val currentHeight = height
                            if (expand) {
                                val expandHeight = maxHeight - currentHeight
                                val duration = duration * expandHeight / maxHeight
                                animate(currentHeight, maxHeight, duration, interpolator) { setHeight(it) }
                            } else {
                                val duration = duration * currentHeight / maxHeight
                                animate(currentHeight, 0, duration, interpolator) { setHeight(it) }
                            }
                        } else {
                            val currentWidth = width
                            if (expand) {
                                val expandWidth = maxWidth - currentWidth
                                val duration = duration * expandWidth / maxWidth
                                animate(currentWidth, maxWidth, duration, interpolator) { setWidth(it) }
                            } else {
                                val duration = duration * currentWidth / maxWidth
                                animate(currentWidth, 0, duration, interpolator) { setWidth(it) }
                            }
                        }
                    } else {
                        if (expandDirection == ExpandDirection.VERTICAL) {
                            setHeight(if (expand) maxHeight else 0)
                        } else {
                            setWidth(if (expand) maxWidth else 0)
                        }
                    }
                }
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
        this.interpolator = interpolator
    }

    @Suppress("unused")
    fun addStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.add(listener)
    }

    @Suppress("unused")
    fun removeStateChangeListener(listener: OnStateChangeListener) {
        stateChangeListeners.remove(listener)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setExpanded(expand: Boolean, animate: Boolean = true) {
        lifecycleScope.launch {
            expandTaskChannel.send(ExpandTask(expand, animate))
        }
    }

    @Suppress("unused")
    fun toggleExpanded(animate: Boolean = true) {
        setExpanded(!expanded, animate)
    }

}