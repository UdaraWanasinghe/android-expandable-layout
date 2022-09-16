package com.aureusapps.android.expandablelayout

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import com.aureusapps.android.extensions.addView
import com.aureusapps.android.extensions.dp
import com.aureusapps.android.extensions.generateLayoutParams
import com.aureusapps.android.extensions.setTextStyle
import com.google.android.material.button.MaterialButton

@Suppress("NestedLambdaShadowedImplicitParameter")
class CodeActivity : AppCompatActivity() {

    private lateinit var expandableLayout: ExpandableLayout
    private lateinit var materialButton: MaterialButton

    @SuppressLint("SetTextI18n")
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
                        materialButton = this
                        layoutParams = it.generateLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                        setText(R.string.collapse)
                        setOnClickListener {
                            expandableLayout.toggleExpanded()
                        }
                    }
            }
            .addView {
                ExpandableLayout(it.context)
                    .apply {
                        expandableLayout = this
                        layoutParams = it.generateLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        expandDirection = ExpandableLayout.ExpandDirection.VERTICAL
                        setExpanded(true)
                        animationInterpolator = DecelerateInterpolator()
                        animationDuration = 2000
                        setContentGravity(ExpandableLayout.Gravity.CENTER)
                        addStateChangeListener(object : ExpandableLayout.OnStateChangeListener {
                            override fun onStateChanged(expandableLayout: ExpandableLayout, isExpanded: Boolean) {
                                materialButton.setText(if (isExpanded) R.string.collapse else R.string.expand)
                            }
                        })
                    }
                    .addView {
                        TextView(it.context)
                            .apply {
                                layoutParams = ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                                    .apply {
                                        setMargins(16.dp)
                                    }
                                text = "HELLO WORLD"
                                setTextStyle(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline1)
                                setTextColor(Color.WHITE)
                                setBackgroundResource(R.drawable.frame)
                                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                                setTypeface(typeface, Typeface.BOLD)
                            }
                    }
            }
        setContentView(rootView)
    }

}