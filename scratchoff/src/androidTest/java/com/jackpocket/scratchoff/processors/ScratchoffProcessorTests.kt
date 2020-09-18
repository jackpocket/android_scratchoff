package com.jackpocket.scratchoff.processors

import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import com.jackpocket.scratchoff.processors.InvalidationProcessor.Delegate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchoffProcessorTests {

    @Test
    fun testScratchoffProcessorAddsNewPathsOnMotionEventAndDequeues() {
        val processor = ScratchoffProcessor(null, null, null)

        assertEquals(0, processor.queuedEvents.size)

        val events = listOf(
                ScratchPathPoint(1f, 1f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(2f, 2f, MotionEvent.ACTION_MOVE),
                ScratchPathPoint(3f, 3f, MotionEvent.ACTION_MOVE),
                ScratchPathPoint(4f, 4f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(5f, 5f, MotionEvent.ACTION_MOVE)
        )

        events.forEach(processor::synchronouslyQueueEvent)

        assertEquals(5, processor.queuedEvents.size)

        events.forEachIndexed({ index, item ->
            assertEquals(item, processor.queuedEvents[index])
        })

        val dequeued = processor.synchronouslyDequeueEvents()

        assertEquals(0, processor.queuedEvents.size)

        events.forEachIndexed({ index, item ->
            assertEquals(item, dequeued[index])
        })
    }

    @Test
    fun testScratchoffProcessorSendsPathsToSubProcessors() {
        val collectedPaths = mutableListOf<ScratchPathPoint>()

        val processor = object: ScratchoffProcessor(
                ScratchoffProcessor.Delegate {
                    collectedPaths.addAll(it)
                },
                object: ThresholdProcessor(0, 0.0, object: ThresholdProcessor.Delegate {
                    override fun postScratchPercentChanged(percent: Double) { }
                    override fun postScratchThresholdReached() { }
                    override fun getScratchableLayoutSize(): IntArray {
                        return intArrayOf()
                    }
                }) {
                    override fun postNewScratchedMotionEvents(events: MutableList<ScratchPathPoint>) {
                        collectedPaths.addAll(events)
                    }
                },
                object: InvalidationProcessor(Delegate {  }) {
                    override fun postNewScratchedMotionEvents(events: MutableList<ScratchPathPoint>) {
                        collectedPaths.addAll(events)
                    }
                }) {
            override fun isActive(id: Long): Boolean {
                return collectedPaths.isEmpty()
            }
        }

        assertEquals(0, collectedPaths.size)

        val events = listOf(
                ScratchPathPoint(1f, 1f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(2f, 2f, MotionEvent.ACTION_MOVE)
        )

        events.forEach(processor::synchronouslyQueueEvent)

        processor.run()

        assertEquals(0, processor.queuedEvents.size)
        assertEquals(6, collectedPaths.size)

        collectedPaths.forEach({ point ->
            assertEquals(3, collectedPaths.count({ point == it }))
        })
    }

    @Test
    fun testScratchoffProcessorStartsAndStopsSubProcessors() {
        var startCalls: Int = 0
        var stopCalls: Int = 0

        val processor = ScratchoffProcessor(
                null,
                object: ThresholdProcessor(0, 0.0, object: ThresholdProcessor.Delegate {
                    override fun postScratchPercentChanged(percent: Double) { }
                    override fun postScratchThresholdReached() { }
                    override fun getScratchableLayoutSize(): IntArray {
                        return intArrayOf()
                    }
                }) {
                    override fun start() {
                        startCalls++
                    }

                    override fun stop() {
                        stopCalls++
                    }
                },
                object: InvalidationProcessor(Delegate {  }) {
                    override fun start() {
                        startCalls++
                    }

                    override fun stop() {
                        stopCalls++
                    }
                })

        processor.start()

        // Starting a Processor should always trigger a stop()
        assertEquals(2, stopCalls)
        assertEquals(2, startCalls)

        processor.stop()

        assertEquals(4, stopCalls)
    }
}