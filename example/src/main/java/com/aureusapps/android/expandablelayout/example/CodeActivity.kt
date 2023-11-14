package com.aureusapps.android.expandablelayout.example

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.aureusapps.android.expandablelayout.ExpandableLayout
import com.aureusapps.android.extensions.addView
import com.aureusapps.android.extensions.dp
import com.aureusapps.android.extensions.generateLayoutParams
import com.aureusapps.android.extensions.setTextStyle
import com.aureusapps.android.styles.extensions.withButtonStyle_Elevated
import com.google.android.material.button.MaterialButton

@Suppress("NestedLambdaShadowedImplicitParameter")
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
                MaterialButton(
                    it.context.withButtonStyle_Elevated,
                    null,
                    com.aureusapps.android.styles.R.attr.buttonStyle_elevated
                ).apply {
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
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                            .apply {
                                setMargins(8.dp)
                            }
                        expandDirection = ExpandableLayout.DIRECTION_VERTICAL
                        setExpanded(false)
                        animationInterpolator = DecelerateInterpolator()
                        animationDuration = 2000
                        contentGravity = ExpandableLayout.GRAVITY_CENTER
                        setBackgroundResource(R.drawable.frame)
                        addExpandStateChangeListener { isExpanded ->
                            materialButton.setText(if (isExpanded) R.string.collapse else R.string.expand)
                        }
                    }
                    .addView {
                        TextView(it.context)
                            .apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    WRAP_CONTENT,
                                    WRAP_CONTENT
                                )
                                setText(R.string.hello_world)
                                setTextStyle(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline2)
                                setPadding(16.dp)
                                setTextColor(Color.YELLOW)
                                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                                setTypeface(typeface, Typeface.BOLD)
                            }
                    }
            }
        setContentView(rootView)
    }

}