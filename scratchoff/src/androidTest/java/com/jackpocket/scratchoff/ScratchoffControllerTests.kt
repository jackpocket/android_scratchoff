package com.jackpocket.scratchoff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.AbsSavedState
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import com.jackpocket.scratchoff.paths.ScratchPathPointsAggregator
import com.jackpocket.scratchoff.views.ScratchableLayout
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import com.jackpocket.scratchoff.views.ScratchableRelativeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchoffControllerTests {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private val mockScratchableLayout: ScratchableLinearLayout by lazy {
        ScratchableLinearLayout(context)
    }

    private val loggingDelegate = LoggingThresholdChangedListener()

    @Test
    fun testInstantiatesRequirementsOnAttach() {
        var createProcessorCount: Int = 0
        var createLayoutDrawerCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun createLayoutDrawer(): ScratchableLayoutDrawer {
                createLayoutDrawerCount += 1

                return super.createLayoutDrawer()
            }

            override fun createThresholdProcessor(): ScratchoffThresholdProcessor {
                createProcessorCount += 1

                return super.createThresholdProcessor()
            }
        }

        assertEquals(0, createProcessorCount)
        assertEquals(0, createLayoutDrawerCount)

        controller.attach()

        assertEquals(1, createProcessorCount)
        assertEquals(1, createLayoutDrawerCount)
    }

    @Test
    fun testThresholdReachedTriggersCompletionCallbackWithoutClearingEnabled() {
        var clearCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun clear(): ScratchoffController {
                clearCount += 1

                return this
            }
        }
        controller.setThresholdChangedListener(loggingDelegate)
        controller.setClearOnThresholdReachedEnabled(false)
        controller.onThresholdReached()

        assertEquals(1, loggingDelegate.completions)
        assertEquals(0, clearCount)
        assert(controller.isThresholdReached)
    }

    @Test
    fun testThresholdReachedTriggersCompletionCallbackWithClearingEnabled() {
        var clearCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun clear(): ScratchoffController {
                clearCount += 1

                return this
            }
        }
        controller.setThresholdChangedListener(loggingDelegate)
        controller.setClearOnThresholdReachedEnabled(true)
        controller.onThresholdReached()

        assertEquals(1, loggingDelegate.completions)
        assertEquals(1, clearCount)
        assert(controller.isThresholdReached)
    }

    @Test
    fun testClearMakesScratchableLayoutUnavailable() {
        val controller = ScratchoffController(mockScratchableLayout)
        controller.onScratchableLayoutAvailable(1, 1)

        assert(controller.isScratchableLayoutAvailable)

        controller.clear()

        assertFalse(controller.isScratchableLayoutAvailable)
    }

    @Test
    fun testDestroyCallsRemovesObservers() {
        var count: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun removeTouchObservers() {
                count += 1
            }
        }
        controller.onDestroy()

        assertEquals(1, count)
    }

    @Test
    fun testMotionEventsAreAlwaysPassedToObserversUnlessIgnoreFlagSet() {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        val touchLogger = LoggingOnTouchListener()

        val controller = ScratchoffController(mockScratchableLayout)
        controller.addTouchObserver(touchLogger)

        val expectedCallCount = 3

        0.until(3)
                .forEach({
                    controller.onTouch(mockScratchableLayout, event)
                })

        assertEquals(expectedCallCount, touchLogger.touchCallCount)

        controller.setTouchInteractionIgnored(true)

        0.until(3)
                .forEach({
                    controller.onTouch(mockScratchableLayout, event)
                })

        assertEquals(expectedCallCount, touchLogger.touchCallCount)

        controller.setTouchInteractionIgnored(false)
        controller.onTouch(mockScratchableLayout, event)

        assertEquals(expectedCallCount + 1, touchLogger.touchCallCount)
    }

    @Test
    fun testMotionEventsNotEnqueuedBeforeLayoutAvailable() {
        var enqueueCallCount: Int = 0

        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun addScratchPathPoints(events: MutableCollection<ScratchPathPoint>?) {
                enqueueCallCount += 1
            }
        }
        controller.onTouch(View(context), event)

        assertEquals(0, enqueueCallCount)

        controller.onScratchableLayoutAvailable(1, 1)
        controller.onTouch(View(context), event)

        assertEquals(1, enqueueCallCount)
    }

    @Test
    fun testMotionEventsEnqueuedWhenReady() {
        var enqueueCallCount: Int = 0
        var enqueueSpecificListenerCallCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun addScratchPathPoints(events: MutableCollection<ScratchPathPoint>?) {
                enqueueCallCount += 1

                super.addScratchPathPoints(events)
            }

            override fun addScratchPathPoints(events: MutableCollection<ScratchPathPoint>?, listener: ScratchPathPointsAggregator?) {
                // This should be called twice; once with the ScratchableLayoutDrawer, and once
                // with the ThresholdProcessor
                enqueueSpecificListenerCallCount += 1
            }
        }
        controller.onScratchableLayoutAvailable(10, 20)
        controller.onTouch(View(context), MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0))

        assertEquals(1, enqueueCallCount)
        assertEquals(2, enqueueSpecificListenerCallCount)
    }

    @Test
    fun testLayoutSizeIsZeroWhenLayoutIsUnavailable() {
        val controller = ScratchoffController(mockScratchableLayout)

        assertEquals(intArrayOf(0, 0).toList(), controller.scratchableLayoutSize.toList())
    }

    @Test
    fun testLayoutAvailableAttemptsRestoreWhenParcelExistsOnLayoutAvailable() {
        var restoreAttemptCount: Int = 0
        var restoreCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun performStateRestoration() {
                restoreAttemptCount += 1

                super.performStateRestoration()
            }

            override fun performStateRestoration(state: ScratchoffState) {
                restoreCount += 1
            }
        }
        controller.setStateRestorationParcel(
                ScratchoffState(
                        AbsSavedState.EMPTY_STATE,
                        intArrayOf(10, 20),
                        false,
                        listOf()))
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(1, restoreAttemptCount)
        assertEquals(1, restoreCount)
    }

    @Test
    fun testParcelizedDataCanBeRestored() {
        var restoredState: ScratchoffState? = null
        var expectedPoint = ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN)

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun performStateRestoration(state: ScratchoffState) {
                restoredState = state

                super.performStateRestoration(state)
            }
        }
        controller.setStateRestorationParcel(
                ScratchoffState(
                        AbsSavedState.EMPTY_STATE,
                        intArrayOf(10, 20),
                        false,
                        listOf(expectedPoint)))
        controller.onScratchableLayoutAvailable(10, 20)

        val state = controller.parcelize(AbsSavedState.EMPTY_STATE)

        assertEquals(10, state.layoutSize[0])
        assertEquals(20, state.layoutSize[1])
        assertEquals(false, state.isThresholdReached)

        controller.clear()
        controller.setStateRestorationParcel(state)
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(10, restoredState!!.layoutSize[0])
        assertEquals(20, restoredState!!.layoutSize[1])
        assertEquals(false, restoredState!!.isThresholdReached)

        assertEquals(expectedPoint, controller.clonedHistory[0])
    }

    @Test
    fun testRestoreNotAttemptedWhenPendingStateRemovedOnLayoutAvailable() {
        var restoreAttemptCount: Int = 0
        var restoreCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun performStateRestoration() {
                restoreAttemptCount += 1

                super.performStateRestoration()
            }

            override fun performStateRestoration(state: ScratchoffState) {
                restoreCount += 1
            }
        }
        controller.removePendingStateRestorationParcel()
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(1, restoreAttemptCount)
        assertEquals(0, restoreCount)
    }

    @Test
    fun testRestoreWithThresholdReachedClearsWithoutAnimation() {
        var clearCount: Int = 0
        var animationEnabled: Boolean? = null

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun clearLayoutDrawer(clearAnimationEnabled: Boolean) {
                clearCount += 1
                animationEnabled = clearAnimationEnabled
            }
        }
        controller.setClearAnimationEnabled(true)
        controller.setStateRestorationParcel(
                ScratchoffState(
                        AbsSavedState.EMPTY_STATE,
                        intArrayOf(10, 20),
                        true,
                        listOf()))
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(1, clearCount)
        assertEquals(false, animationEnabled)
    }

    @Test
    fun testFindHelperFunctionSearchesParentView() {
        testFindHelperFunctionSearchesParentView(ScratchableLinearLayout(context))
        testFindHelperFunctionSearchesParentView(ScratchableRelativeLayout(context))
    }

    private fun testFindHelperFunctionSearchesParentView(layout: ScratchableLayout) {
        val view = layout as View
        view.id = 100

        val expectedController = layout.scratchoffController

        val parent = FrameLayout(context)
        parent.addView(View(context))
        parent.addView(View(context))
        parent.addView(view)

        assertEquals(expectedController, ScratchoffController.findByViewId(parent, 100))
    }

    @Test
    fun testTouchObserversAreAddedAndRemoved() {
        val observer1 = View.OnTouchListener({ _, _ -> true })
        val observer2 = View.OnTouchListener({ _, _ -> true })

        val controller = ScratchoffController(mockScratchableLayout)
        controller.addTouchObserver(observer1)
        controller.addTouchObserver(observer2)

        assertEquals(2, controller.touchObservers.size)

        controller.removeTouchObserver(observer2)

        assertEquals(1, controller.touchObservers.size)
        assertEquals(observer1, controller.touchObservers[0])

        controller.addTouchObserver(observer2)

        assertEquals(2, controller.touchObservers.size)

        controller.removeTouchObservers()

        assertEquals(0, controller.touchObservers.size)
    }

    @Test
    fun testDelegateCallbacksPostRunnable() {
        var postCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun post(runnable: Runnable?) {
                postCount += 1
            }
        }

        controller.setThresholdChangedListener(loggingDelegate)
        controller.postScratchPercentChanged(0f)
        controller.postScratchThresholdReached()

        assertEquals(2, postCount)
    }

    @Test
    fun testReturnsCustomRegionProviders() {
        val controller = ScratchoffController(mockScratchableLayout)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        assertEquals(Rect(0, 0, 10, 10), controller.createScratchableRegions(bitmap).first())

        controller.setThresholdTargetRegionsProvider({
            listOf(
                    Rect(0, 0, 5, 5)
            )
        })

        assertEquals(Rect(0, 0, 5, 5), controller.createScratchableRegions(bitmap).first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSettingATouchRadiusLessThan1PxThrowsAnException() {
        val controller = ScratchoffController(mockScratchableLayout)
        controller.setTouchRadiusPx(0)
    }

    private class LoggingThresholdChangedListener: ScratchoffController.ThresholdChangedListener {

        var threshold: Float = 0f
            private set

        var completions: Int = 0
            private set

        override fun onScratchPercentChanged(controller: ScratchoffController, percentCompleted: Float) {
            threshold = percentCompleted
        }

        override fun onScratchThresholdReached(controller: ScratchoffController) {
            completions += 1
        }
    }

    private class LoggingOnTouchListener: View.OnTouchListener {

        var touchCallCount: Int = 0
            private set

        var touchReturnValue: Boolean = true

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            touchCallCount += 1

            return touchReturnValue
        }
    }
}