package com.aureusapps.android.expandablelayout

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.lifecycle.*

abstract class LifecycleAwareViewGroup(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes), DefaultLifecycleObserver {

    private var lifecycleOwner: LifecycleOwner? = null
    val lifecycleScope: LifecycleCoroutineScope? get() = lifecycleOwner?.lifecycleScope

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachLifecycleOwner()
    }

    private fun attachLifecycleOwner() {
        val newLifecycleOwner = findViewTreeLifecycleOwner()
        if (newLifecycleOwner != lifecycleOwner) {
            lifecycleOwner?.lifecycle?.removeObserver(this)
            lifecycleOwner = newLifecycleOwner
            lifecycleOwner?.lifecycle?.addObserver(this)
        }
    }

}