package com.aureusapps.android.expandablelayout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aureusapps.android.extensions.addView
import com.aureusapps.android.extensions.generateLayoutParams
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("NestedLambdaShadowedImplicitParameter")
@RunWith(AndroidJUnit4::class)
class ExpandableLayoutInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var parentLayout: ViewGroup
    private lateinit var expandableLayout: ExpandableLayout
    private lateinit var wrappedLayout1: View
    private lateinit var wrappedLayout2: View

    @Before
    fun setup() {
        createContentView()
    }

    @Test
    fun testOnMeasure() {
        // parent layout is MATCH_PARENT, MATCH_PARENT
        // expandable layout is WRAP_CONTENT, WRAP_CONTENT
        // wrapped layout is 200, 200
        parentLayout.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        expandableLayout.layoutParams = parentLayout.generateLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        expandableLayout.setExpanded(true)
        expandableLayout.expandDirection = ExpandableLayout.ExpandDirection.HORIZONTAL
        wrappedLayout1.layoutParams = expandableLayout.generateLayoutParams(200, 200)
        wrappedLayout2.layoutParams = expandableLayout.generateLayoutParams(300, 300)

        parentLayout.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.AT_MOST)
        )
        Assert.assertEquals(300, expandableLayout.measuredWidth)
    }

    private fun createContentView() {
        parentLayout = LinearLayout(context)
            .addView {
                ExpandableLayout(it.context).apply {
                    expandableLayout = this
                }
                    .addView {
                        TextView(it.context).apply {
                            wrappedLayout1 = this
                        }
                    }
                    .addView {
                        TextView(it.context).apply {
                            wrappedLayout2 = this
                        }
                    }
            }
    }

}