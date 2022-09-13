package com.aureusapps.android.expandablelayout

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.aureusapps.android.extensions.addView
import com.aureusapps.android.extensions.generateLayoutParams
import com.google.android.material.button.MaterialButton

class CodeActivity : AppCompatActivity() {

    private lateinit var expandableLayout: ExpandableLayout
    private lateinit var materialButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = LinearLayout(this)
            .apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
            }
            .addView {
                MaterialButton(it.context)
                    .apply {
                        setText(R.string.collapse)
                        setOnClickListener {
                            expandableLayout.toggleExpanded()
                        }
                        materialButton = this
                    }
            }
            .addView {
                ExpandableLayout(it.context)
                    .apply {
                        layoutParams = it.generateLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        setExpandDirection(ExpandableLayout.ExpandDirection.VERTICAL)
                        setExpanded(true)
                        addStateChangeListener(object : ExpandableLayout.OnStateChangeListener {
                            override fun onStateChanged(expandableLayout: ExpandableLayout, isExpanded: Boolean) {
                                materialButton.setText(if (isExpanded) R.string.collapse else R.string.expand)
                            }
                        })
                        expandableLayout = this
                    }
            }
        setContentView(rootView)
    }

}