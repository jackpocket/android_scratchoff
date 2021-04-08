package com.jackpocket.scratchoff.views

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jackpocket.scratchoff.ScratchoffController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchableLayoutTests {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testViewsDelegateOnDrawEvents() {
        val relativeLayout = object: ScratchableRelativeLayout(context) {
            override fun createScratchoffController(): ScratchoffController {
                return LoggingScratchoffController(this)
            }
        }

        val linearLayout = object: ScratchableLinearLayout(context) {
            override fun createScratchoffController(): ScratchoffController {
                return LoggingScratchoffController(this)
            }
        }

        assertEquals(0, relativeLayout.loggingController.drawCount)
        assertEquals(0, linearLayout.loggingController.drawCount)

        relativeLayout.draw(Canvas())
        linearLayout.draw(Canvas())

        assertEquals(1, relativeLayout.loggingController.drawCount)
        assertEquals(1, linearLayout.loggingController.drawCount)
    }

    private val ScratchableLayout.loggingController: LoggingScratchoffController
        get() = scratchoffController as LoggingScratchoffController

    private class LoggingScratchoffController(view: View): ScratchoffController(view) {

        var drawCount: Int = 0
            private set

        override fun draw(canvas: Canvas?) {
            drawCount += 1
        }
    }
}