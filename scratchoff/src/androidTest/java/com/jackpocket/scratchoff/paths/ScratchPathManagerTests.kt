package com.jackpocket.scratchoff.paths

import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchPathManagerTests {

    @Test
    fun testActionDownAlwaysCreatesNewPath() {
        val manager = ScratchPathManager()

        assertEquals(0, manager.paths.size)

        manager.handleTouchDown(0, 0f, 0f)
        manager.handleTouchDown(0, 0f, 0f)
        manager.handleTouchDown(0, 0f, 0f)

        assertEquals(3, manager.paths.size)
    }

    @Test
    fun testActionMoveChangesActivePathAndDoesNotCreateNewPath() {
        val manager = ScratchPathManager()
        manager.handleTouchDown(0, 0f, 0f)

        assertEquals(1, manager.paths.size)
        manager.paths[0].assertEmpty(true)

        manager.handleTouchMove(0, 1f, 1f)

        assertEquals(1, manager.paths.size)
        manager.paths[0].assertEmpty(false)
    }

    @Test
    fun testPointerUpFollowedByMoveCreatesNewPath() {
        val manager = ScratchPathManager()

        val events = listOf(
                ScratchPathPoint(0, 1f, 1f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 2f, 2f, MotionEvent.ACTION_POINTER_UP),
                ScratchPathPoint(0, 3f, 3f, MotionEvent.ACTION_MOVE)
        )

        manager.addScratchPathPoints(events)

        assertEquals(2, manager.paths.size)
    }

    private fun Path.assertEmpty(value: Boolean) {
        val pathBounds = RectF()

        this.computeBounds(pathBounds, true)

        assertEquals(value, pathBounds.isEmpty)
    }

    @Test
    fun testTouchDownAndMovePassthroughIncludesScalar() {
        val manager = object: ScratchPathManager() {
            override fun handleTouchDown(pointerIndex: Int, x: Float, y: Float) {
                assertEquals(0.5f, x)
                assertEquals(0.5f, y)
            }

            override fun handleTouchMove(pointerIndex: Int, x: Float, y: Float) {
                assertEquals(1f, x)
                assertEquals(1f, y)
            }
        }
        manager.setScale(0.5f)

        val events = listOf(
                ScratchPathPoint(0, 1f, 1f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 2f, 2f, MotionEvent.ACTION_MOVE)
        )

        manager.addScratchPathPoints(events)
    }
}