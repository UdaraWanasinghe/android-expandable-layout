package com.aureusapps.android.expandablelayout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
    private lateinit var expandableLayout: ExpandableLayout
    private lateinit var wrappedLayout1: View
    private lateinit var wrappedLayout2: View

    @Before
    fun setup() {
        createContentView()
    }

    @Test
    fun testOnMeasure() {
        testLayout(
            expandableWidth = WRAP_CONTENT, expandableHeight = WRAP_CONTENT,
            wrapped1Width = 200, wrapped1Height = 200,
            wrapped2Width = 500, wrapped2Height = 500,
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            expectedExpandableWidth = 500, expectedExpandableHeight = 500,
            expectedWrapped1Width = 200, expectedWrapped1Height = 200,
            expectedWrapped2Width = 500, expectedWrapped2Height = 500
        )
        testLayout(
            expandableWidth = MATCH_PARENT, expandableHeight = WRAP_CONTENT,
            wrapped1Width = 200, wrapped1Height = 200,
            wrapped2Width = 500, wrapped2Height = 500,
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            expectedExpandableWidth = 1000, expectedExpandableHeight = 500,
            expectedWrapped1Width = 200, expectedWrapped1Height = 200,
            expectedWrapped2Width = 500, expectedWrapped2Height = 500
        )
        testLayout(
            expandableWidth = 100, expandableHeight = WRAP_CONTENT,
            wrapped1Width = 200, wrapped1Height = 200,
            wrapped2Width = 500, wrapped2Height = 500,
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            expectedExpandableWidth = 100, expectedExpandableHeight = 500,
            expectedWrapped1Width = 200, expectedWrapped1Height = 200,
            expectedWrapped2Width = 500, expectedWrapped2Height = 500
        )
        testLayout(
            expandableWidth = 200, expandableHeight = WRAP_CONTENT,
            wrapped1Width = 200, wrapped1Height = 200,
            wrapped2Width = 500, wrapped2Height = 500,
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
            expectedExpandableWidth = 100, expectedExpandableHeight = 500,
            expectedWrapped1Width = 200, expectedWrapped1Height = 200,
            expectedWrapped2Width = 500, expectedWrapped2Height = 500
        )
    }

    private fun testLayout(
        expandableWidth: Int,
        expandableHeight: Int,
        wrapped1Width: Int,
        wrapped1Height: Int,
        wrapped2Width: Int,
        wrapped2Height: Int,
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        expectedExpandableWidth: Int,
        expectedExpandableHeight: Int,
        expectedWrapped1Width: Int,
        expectedWrapped1Height: Int,
        expectedWrapped2Width: Int,
        expectedWrapped2Height: Int,
        expandDirection: ExpandableLayout.ExpandDirection = ExpandableLayout.ExpandDirection.HORIZONTAL
    ) {
        expandableLayout.layoutParams = ViewGroup.LayoutParams(expandableWidth, expandableHeight)
        expandableLayout.setExpanded(true)
        expandableLayout.expandDirection = expandDirection
        wrappedLayout1.layoutParams = expandableLayout.generateLayoutParams(wrapped1Width, wrapped1Height)
        wrappedLayout2.layoutParams = expandableLayout.generateLayoutParams(wrapped2Width, wrapped2Height)
        expandableLayout.measure(widthMeasureSpec, heightMeasureSpec)

        // expandable layout
        Assert.assertEquals(expectedExpandableWidth, expandableLayout.measuredWidth)
        Assert.assertEquals(expectedExpandableHeight, expandableLayout.measuredHeight)

        // wrapped layout 1
        Assert.assertEquals(expectedWrapped1Width, wrappedLayout1.measuredWidth)
        Assert.assertEquals(expectedWrapped1Height, wrappedLayout1.measuredHeight)

        // wrapped layout 2
        Assert.assertEquals(expectedWrapped2Width, wrappedLayout2.measuredWidth)
        Assert.assertEquals(expectedWrapped2Height, wrappedLayout2.measuredHeight)
    }

    private fun createContentView() {
        ExpandableLayout(context).apply {
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