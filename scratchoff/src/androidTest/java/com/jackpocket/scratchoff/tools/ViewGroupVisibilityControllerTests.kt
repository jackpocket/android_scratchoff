package com.jackpocket.scratchoff.tools

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewGroupVisibilityControllerTests {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testHidesView() {
        val parent = LinearLayout(context)

        val controller = ViewGroupVisibilityController()
        controller.hide(parent)

        assertEquals(View.GONE, parent.visibility)
    }

    @Test
    fun testHideChildrenHidesChildrenButNotParent() {
        val parent = LinearLayout(context)
        parent.addView(View(context))
        parent.addView(View(context))

        val controller = ViewGroupVisibilityController()
        controller.hideChildren(parent)

        assertEquals(View.VISIBLE, parent.visibility)
        assertEquals(View.GONE, parent.getChildAt(0).visibility)
        assertEquals(View.GONE, parent.getChildAt(1).visibility)
    }

    @Test
    fun testShowChildrenShowsChildrenButNotParent() {
        val parent = LinearLayout(context)
        parent.visibility = View.GONE
        parent.addView(View(context).apply({ this.visibility = View.GONE }))
        parent.addView(View(context).apply({ this.visibility = View.GONE }))

        val controller = ViewGroupVisibilityController()
        controller.showChildren(parent)

        assertEquals(View.GONE, parent.visibility)
        assertEquals(View.VISIBLE, parent.getChildAt(0).visibility)
        assertEquals(View.VISIBLE, parent.getChildAt(1).visibility)
    }

    @Test
    fun testNullViewsDoesNotCauseDeath() {
        val controller = ViewGroupVisibilityController()
        controller.hide(null)
        controller.hideChildren(null)
        controller.showChildren(null)
    }
}