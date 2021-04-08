package com.jackpocket.scratchoff

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import com.jackpocket.scratchoff.tools.ThresholdCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchoffThresholdProcessorTests {

    @Test
    fun testRunDoesNothingWhenImageNull() {
        var calls: Int = 0

        val processor = object: ScratchoffThresholdProcessor(1, 1f, ScratchoffThresholdProcessor.Quality.HIGH, LoggingDelegate()) {
            override fun drawQueuedScratchMotionEvents(): Boolean {
                calls += 1

                return false
            }

            override fun processScratchedImagePercent() {
                calls += 1
            }
        }

        processor.run()

        assertEquals(0, calls)
        assertEquals(-1.0f, processor.loggingDelegate.scratchPercent)
    }

    @Test
    fun testRunNotifiesZeroPercentOnFirstRunWithoutEvents() {
        val processor = ScratchoffThresholdProcessor(1, 1f, ScratchoffThresholdProcessor.Quality.HIGH, LoggingDelegate())
        processor.prepare(intArrayOf(1, 1));
        processor.run()

        assertEquals(0.0f, processor.loggingDelegate.scratchPercent)
    }

    @Test
    fun testActionDownWithoutMoveDoesNotDraw() {
        val processor = object: ScratchoffThresholdProcessor(5, 1f, ScratchoffThresholdProcessor.Quality.HIGH, LoggingDelegate()) {
            override fun scheduleNextThresholdEvaluation() { }
        }

        val events = listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN)
        )

        processor.prepare(intArrayOf(10, 10));
        processor.addScratchPathPoints(events)
        processor.drawQueuedScratchMotionEvents()
        processor.processScratchedImagePercent()

        assertEquals(0.0f, processor.loggingDelegate.scratchPercent)
    }

    @Test
    fun testProcessImageTriggersThresholdReachedOnlyOnce() {
        val processor = object: ScratchoffThresholdProcessor(2, 0.5f, ScratchoffThresholdProcessor.Quality.HIGH, LoggingDelegate()) {
            override fun scheduleNextThresholdEvaluation() { }
        }

        val events = listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 9f, MotionEvent.ACTION_MOVE)
        )

        processor.prepare(intArrayOf(2, 10));

        processor.addScratchPathPoints(events)
        processor.drawQueuedScratchMotionEvents()
        processor.processScratchedImagePercent()

        assertEquals(1, processor.loggingDelegate.thresholdReachedCount)

        processor.addScratchPathPoints(events)
        processor.drawQueuedScratchMotionEvents()
        processor.processScratchedImagePercent()

        assertEquals(1, processor.loggingDelegate.thresholdReachedCount)
    }

    @Test
    fun testScratchPercentNotUpdatesAfterThresholdReachedTriggered() {
        val processor = object: ScratchoffThresholdProcessor(1, 0.5f, ScratchoffThresholdProcessor.Quality.HIGH, LoggingDelegate()) {
            override fun scheduleNextThresholdEvaluation() { }
        }

        processor.prepare(intArrayOf(1, 10));
        processor.addScratchPathPoints(listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 2f, MotionEvent.ACTION_MOVE)
        ))
        processor.drawQueuedScratchMotionEvents()
        processor.processScratchedImagePercent()

        assertEquals(0.3f, processor.loggingDelegate.scratchPercent)

        processor.addScratchPathPoints(listOf(
                ScratchPathPoint(0, 0f, 2f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 5f, MotionEvent.ACTION_MOVE)
        ))
        processor.drawQueuedScratchMotionEvents()
        processor.processScratchedImagePercent()

        assertEquals(0.6f, processor.loggingDelegate.scratchPercent)

        processor.addScratchPathPoints(listOf(
                ScratchPathPoint(0, 0f, 6f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 9f, MotionEvent.ACTION_MOVE)
        ))
        processor.drawQueuedScratchMotionEvents()
        processor.processScratchedImagePercent()

        assertEquals(0.6f, processor.loggingDelegate.scratchPercent)
    }

    @Test
    fun testPathRedrawDoesNotAffectHistoricalScratchPercentRedraw() {
        val expectedResult = 0.04654122F

        val processor = object: ScratchoffThresholdProcessor(30, 1f, ScratchoffThresholdProcessor.Quality.HIGH, LoggingDelegate()) {
            override fun scheduleNextThresholdEvaluation() { }
        }

        val events: List<ScratchPathPoint> = listOf(
                ScratchPathPoint(0, 130.979f, 198.98438f, 0),
                ScratchPathPoint(0, 140.96558f, 198.98438f, 2),
                ScratchPathPoint(0, 147.98584f, 198.98438f, 2),
                ScratchPathPoint(0, 153.89212f, 198.98438f, 2),
                ScratchPathPoint(0, 158.99414f, 198.98438f, 2),
                ScratchPathPoint(0, 165.98145f, 198.98438f, 2),
                ScratchPathPoint(0, 182.42917f, 198.98438f, 2),
                ScratchPathPoint(0, 193.96362f, 198.98438f, 2),
                ScratchPathPoint(0, 218.97949f, 198.98438f, 2),
                ScratchPathPoint(0, 239.65051f, 198.98438f, 2),
                ScratchPathPoint(0, 249.96094f, 198.98438f, 2),
                ScratchPathPoint(0, 271.97754f, 198.98438f, 2),
                ScratchPathPoint(0, 300.12894f, 198.98438f, 2),
                ScratchPathPoint(0, 306.97998f, 198.98438f, 2),
                ScratchPathPoint(0, 337.96143f, 202.96875f, 2),
                ScratchPathPoint(0, 379.07147f, 202.96875f, 2),
                ScratchPathPoint(0, 383.97217f, 202.96875f, 2),
                ScratchPathPoint(0, 408.95508f, 202.96875f, 2),
                ScratchPathPoint(0, 433.2675f, 202.96875f, 2),
                ScratchPathPoint(0, 433.97095f, 202.96875f, 2),
                ScratchPathPoint(0, 454.96582f, 205.95703f, 2),
                ScratchPathPoint(0, 468.9734f, 205.95703f, 2),
                ScratchPathPoint(0, 469.67722f, 205.95703f, 2),
                ScratchPathPoint(0, 471.97266f, 205.95703f, 2),
                ScratchPathPoint(0, 485.98022f, 205.95703f, 2),
                ScratchPathPoint(0, 488.45065f, 205.95703f, 2),
                ScratchPathPoint(0, 503.97583f, 205.95703f, 2),
                ScratchPathPoint(0, 510.96313f, 205.95703f, 2),
                ScratchPathPoint(0, 512.4907f, 205.95703f, 2),
                ScratchPathPoint(0, 542.9663f, 205.95703f, 2),
                ScratchPathPoint(0, 559.97314f, 205.95703f, 2),
                ScratchPathPoint(0, 565.09467f, 205.95703f, 2),
                ScratchPathPoint(0, 577.96875f, 205.95703f, 2),
                ScratchPathPoint(0, 595.96436f, 205.95703f, 2),
                ScratchPathPoint(0, 602.2526f, 205.95703f, 2),
                ScratchPathPoint(0, 612.9712f, 205.95703f, 2),
                ScratchPathPoint(0, 630.9668f, 205.95703f, 2),
                ScratchPathPoint(0, 637.606f, 205.95703f, 2),
                ScratchPathPoint(0, 647.97363f, 205.95703f, 2),
                ScratchPathPoint(0, 661.94824f, 205.95703f, 2),
                ScratchPathPoint(0, 666.3833f, 205.95703f, 2),
                ScratchPathPoint(0, 669.9573f, 205.95703f, 2),
                ScratchPathPoint(0, 686.9641f, 205.95703f, 2),
                ScratchPathPoint(0, 691.4526f, 204.03745f, 2),
                ScratchPathPoint(0, 693.9514f, 202.96875f, 2),
                ScratchPathPoint(0, 704.9597f, 202.96875f, 2),
                ScratchPathPoint(0, 710.0168f, 202.96875f, 2),
                ScratchPathPoint(0, 711.947f, 202.96875f, 2),
                ScratchPathPoint(0, 718.9673f, 202.96875f, 2),
                ScratchPathPoint(0, 721.31683f, 202.96875f, 2),
                ScratchPathPoint(0, 721.96655f, 202.96875f, 2),
                ScratchPathPoint(0, 725.9546f, 202.96875f, 2),
                ScratchPathPoint(0, 728.62415f, 199.42236f, 2),
                ScratchPathPoint(0, 728.95386f, 198.98438f, 2),
                ScratchPathPoint(0, 732.89935f, 198.98438f, 2),
                ScratchPathPoint(0, 732.97485f, 198.98438f, 2),
                ScratchPathPoint(0, 735.9741f, 198.98438f, 2),
                ScratchPathPoint(0, 735.97943f, 198.98438f, 2),
                ScratchPathPoint(0, 739.96216f, 198.98438f, 2),
                ScratchPathPoint(0, 742.9614f, 198.98438f, 2),
                ScratchPathPoint(0, 746.94946f, 195.9375f, 2),
                ScratchPathPoint(0, 748.138f, 195.02948f, 2),
                ScratchPathPoint(0, 750.97046f, 195.9375f, 2),
                ScratchPathPoint(0, 757.95776f, 195.9375f, 2),
                ScratchPathPoint(0, 762.573f, 195.9375f, 2),
                ScratchPathPoint(0, 771.96533f, 195.9375f, 2),
                ScratchPathPoint(0, 785.9729f, 195.9375f, 2),
                ScratchPathPoint(0, 792.497f, 195.9375f, 2),
                ScratchPathPoint(0, 806.9678f, 195.9375f, 2),
                ScratchPathPoint(0, 823.9746f, 195.9375f, 2),
                ScratchPathPoint(0, 840.8665f, 195.9375f, 2),
                ScratchPathPoint(0, 855.9448f, 195.9375f, 2),
                ScratchPathPoint(0, 873.9404f, 191.95312f, 2),
                ScratchPathPoint(0, 884.5012f, 191.95312f, 2),
                ScratchPathPoint(0, 890.94727f, 191.95312f, 2),
                ScratchPathPoint(0, 908.9429f, 188.96484f, 2),
                ScratchPathPoint(0, 918.2938f, 188.96484f, 2),
                ScratchPathPoint(0, 922.95044f, 188.96484f, 2),
                ScratchPathPoint(0, 933.95874f, 188.96484f, 2),
                ScratchPathPoint(0, 941.19586f, 188.96484f, 2),
                ScratchPathPoint(0, 943.9453f, 188.96484f, 2),
                ScratchPathPoint(0, 947.9663f, 188.96484f, 2),
                ScratchPathPoint(0, 949.5454f, 188.96484f, 2),
                ScratchPathPoint(0, 947.9663f, 188.96484f, 1),
                ScratchPathPoint(0, 130.979f, 272.9297f, 0),
                ScratchPathPoint(0, 133.97827f, 272.9297f, 2),
                ScratchPathPoint(0, 144.98657f, 272.9297f, 2),
                ScratchPathPoint(0, 149.93568f, 272.9297f, 2),
                ScratchPathPoint(0, 151.97388f, 272.9297f, 2),
                ScratchPathPoint(0, 154.97314f, 272.9297f, 2),
                ScratchPathPoint(0, 158.19516f, 272.9297f, 2),
                ScratchPathPoint(0, 158.99414f, 272.9297f, 2),
                ScratchPathPoint(0, 161.99341f, 272.9297f, 2),
                ScratchPathPoint(0, 163.49304f, 272.9297f, 2),
                ScratchPathPoint(0, 165.98145f, 272.9297f, 2),
                ScratchPathPoint(0, 168.98071f, 272.9297f, 2),
                ScratchPathPoint(0, 169.23843f, 272.9297f, 2),
                ScratchPathPoint(0, 175.96802f, 272.9297f, 2),
                ScratchPathPoint(0, 183.97705f, 272.9297f, 2),
                ScratchPathPoint(0, 184.35025f, 272.9297f, 2),
                ScratchPathPoint(0, 186.97632f, 272.9297f, 2),
                ScratchPathPoint(0, 193.96362f, 272.9297f, 2),
                ScratchPathPoint(0, 195.54608f, 272.9297f, 2),
                ScratchPathPoint(0, 200.98389f, 272.9297f, 2),
                ScratchPathPoint(0, 207.97119f, 272.9297f, 2),
                ScratchPathPoint(0, 210.07579f, 272.9297f, 2),
                ScratchPathPoint(0, 221.97876f, 272.9297f, 2),
                ScratchPathPoint(0, 235.98633f, 272.9297f, 2),
                ScratchPathPoint(0, 239.50812f, 272.9297f, 2),
                ScratchPathPoint(0, 249.96094f, 272.9297f, 2),
                ScratchPathPoint(0, 264.99023f, 272.9297f, 2),
                ScratchPathPoint(0, 270.10095f, 272.9297f, 2),
                ScratchPathPoint(0, 278.96484f, 272.9297f, 2),
                ScratchPathPoint(0, 288.98438f, 272.9297f, 2),
                ScratchPathPoint(0, 291.89465f, 272.9297f, 2),
                ScratchPathPoint(0, 295.97168f, 272.9297f, 2),
                ScratchPathPoint(0, 306.97998f, 269.9414f, 2),
                ScratchPathPoint(0, 311.0307f, 269.9414f, 2),
                ScratchPathPoint(0, 313.9673f, 269.9414f, 2),
                ScratchPathPoint(0, 320.98755f, 269.9414f, 2),
                ScratchPathPoint(0, 324.29193f, 268.05713f, 2),
                ScratchPathPoint(0, 327.97485f, 265.95703f, 2),
                ScratchPathPoint(0, 334.96216f, 265.95703f, 2),
                ScratchPathPoint(0, 340.5169f, 265.95703f, 2),
                ScratchPathPoint(0, 341.98242f, 265.95703f, 2),
                ScratchPathPoint(0, 348.96973f, 265.95703f, 2),
                ScratchPathPoint(0, 358.25937f, 265.95703f, 2),
                ScratchPathPoint(0, 359.97803f, 265.95703f, 2),
                ScratchPathPoint(0, 366.96533f, 265.95703f, 2),
                ScratchPathPoint(0, 373.5903f, 265.95703f, 2),
                ScratchPathPoint(0, 373.9856f, 265.95703f, 2),
                ScratchPathPoint(0, 380.9729f, 265.95703f, 2),
                ScratchPathPoint(0, 383.97217f, 265.95703f, 2),
                ScratchPathPoint(0, 384.34482f, 265.95703f, 2),
                ScratchPathPoint(0, 387.9602f, 265.95703f, 2),
                ScratchPathPoint(0, 388.19406f, 265.95703f, 2),
                ScratchPathPoint(0, 390.95947f, 265.95703f, 2),
                ScratchPathPoint(0, 391.30878f, 265.95703f, 2),
                ScratchPathPoint(0, 394.98047f, 265.95703f, 2),
                ScratchPathPoint(0, 396.99097f, 265.95703f, 2),
                ScratchPathPoint(0, 397.97974f, 265.95703f, 2),
                ScratchPathPoint(0, 399.20795f, 265.95703f, 2),
                ScratchPathPoint(0, 401.96777f, 265.95703f, 2),
                ScratchPathPoint(0, 404.96704f, 265.95703f, 2),
                ScratchPathPoint(0, 405.67795f, 265.95703f, 2),
                ScratchPathPoint(0, 408.95508f, 265.95703f, 2),
                ScratchPathPoint(0, 412.72028f, 264.3543f, 2),
                ScratchPathPoint(0, 415.97534f, 262.96875f, 2),
                ScratchPathPoint(0, 418.9746f, 262.96875f, 2),
                ScratchPathPoint(0, 423.7961f, 262.96875f, 2),
                ScratchPathPoint(0, 426.98364f, 262.96875f, 2),
                ScratchPathPoint(0, 429.9829f, 262.96875f, 2),
                ScratchPathPoint(0, 434.192f, 262.96875f, 2),
                ScratchPathPoint(0, 436.9702f, 262.96875f, 2),
                ScratchPathPoint(0, 440.95825f, 262.96875f, 2),
                ScratchPathPoint(0, 445.9615f, 262.96875f, 2),
                ScratchPathPoint(0, 447.97852f, 262.96875f, 2),
                ScratchPathPoint(0, 454.96582f, 262.96875f, 2),
                ScratchPathPoint(0, 458.45947f, 262.96875f, 2),
                ScratchPathPoint(0, 457.9651f, 262.96875f, 2),
                ScratchPathPoint(0, 461.84256f, 262.96875f, 2),
                ScratchPathPoint(0, 461.95312f, 262.96875f, 2),
                ScratchPathPoint(0, 463.94714f, 262.96875f, 2),
                ScratchPathPoint(0, 464.98535f, 258.98438f, 2),
                ScratchPathPoint(0, 468.9734f, 258.98438f, 2),
                ScratchPathPoint(0, 471.97266f, 258.98438f, 2),
                ScratchPathPoint(0, 472.71838f, 258.98438f, 2),
                ScratchPathPoint(0, 478.95996f, 258.98438f, 2),
                ScratchPathPoint(0, 485.98022f, 258.98438f, 2),
                ScratchPathPoint(0, 487.99054f, 258.98438f, 2),
                ScratchPathPoint(0, 492.96753f, 258.98438f, 2),
                ScratchPathPoint(0, 503.97583f, 255.9375f, 2),
                ScratchPathPoint(0, 505.47452f, 255.9375f, 2),
                ScratchPathPoint(0, 507.96387f, 255.9375f, 2),
                ScratchPathPoint(0, 517.9834f, 255.9375f, 2),
                ScratchPathPoint(0, 520.58905f, 255.9375f, 2),
                ScratchPathPoint(0, 524.9707f, 255.9375f, 2),
                ScratchPathPoint(0, 528.95874f, 255.9375f, 2),
                ScratchPathPoint(0, 530.5238f, 255.9375f, 2),
                ScratchPathPoint(0, 531.958f, 255.9375f, 2),
                ScratchPathPoint(0, 535.979f, 255.9375f, 2),
                ScratchPathPoint(0, 537.9895f, 255.9375f, 2),
                ScratchPathPoint(0, 538.9783f, 255.9375f, 2),
                ScratchPathPoint(0, 545.6665f, 255.9375f, 2),
                ScratchPathPoint(0, 545.9656f, 255.9375f, 2),
                ScratchPathPoint(0, 563.9612f, 255.9375f, 2),
                ScratchPathPoint(0, 588.6337f, 255.9375f, 2),
                ScratchPathPoint(0, 588.97705f, 255.9375f, 2)
        )

        processor.prepare(intArrayOf(1080, 1584))

        events.forEach({
            processor.addScratchPathPoints(listOf(it))
            processor.drawQueuedScratchMotionEvents()
            processor.processScratchedImagePercent()
        })

        assertEquals(expectedResult, processor.loggingDelegate.scratchPercent)

        processor.processScratchedImagePercent()

        // Before the ThresholdProcessor was set to only notify on
        // threshold updates that were larger than the previous ones,
        // there could be up to a 0.001% loss in historical reload accuracy,
        // due to the difference in drawing between the events as they come
        // in, and the single-redraw used when restoring the history
        // val maximumLoss = 0.00001f
        //
        // assert(scratchPercent <= expectedResult)
        // assert(expectedResult - scratchPercent < maximumLoss)

        assertEquals(expectedResult, processor.loggingDelegate.scratchPercent)
    }

    @Test
    fun constrainAccuracyQualityBoundedToMinMax() {
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(100, ScratchoffThresholdProcessor.Quality.HIGH, intArrayOf(100, 100)))
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, ScratchoffThresholdProcessor.Quality.HIGH, intArrayOf(100, 100)))
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, ScratchoffThresholdProcessor.Quality.HIGH, intArrayOf(100, 100)))

        assertEquals(0.5f, ScratchoffThresholdProcessor.constrainAccuracyQuality(100, ScratchoffThresholdProcessor.Quality.MEDIUM, intArrayOf(100, 100)))
        assertEquals(0.5f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, ScratchoffThresholdProcessor.Quality.MEDIUM, intArrayOf(100, 100)))
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, ScratchoffThresholdProcessor.Quality.MEDIUM, intArrayOf(100, 100)))

        assertEquals(0.01f, ScratchoffThresholdProcessor.constrainAccuracyQuality(100, ScratchoffThresholdProcessor.Quality.LOW, intArrayOf(100, 100)))
        assertEquals(0.1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, ScratchoffThresholdProcessor.Quality.LOW, intArrayOf(100, 100)))
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, ScratchoffThresholdProcessor.Quality.LOW, intArrayOf(100, 100)))
    }

    @Test
    fun constrainAccuracyQualityValuesBoundedToMinMax() {
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, 10.0f, intArrayOf(100, 100)))
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, 0.0f, intArrayOf(100, 100)))

        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, 10.0f, intArrayOf(1, 1)))
        assertEquals(1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, 0.0f, intArrayOf(1, 1)))

        assertEquals(0.01f, ScratchoffThresholdProcessor.constrainAccuracyQuality(100, 0.0f, intArrayOf(100, 100)))
        assertEquals(0.1f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, 0.0f, intArrayOf(100, 100)))
        assertEquals(1.0f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, 0.0f, intArrayOf(100, 100)))

        assertEquals(0.5f, ScratchoffThresholdProcessor.constrainAccuracyQuality(100, 0.5f, intArrayOf(100, 100)))
        assertEquals(0.5f, ScratchoffThresholdProcessor.constrainAccuracyQuality(10, 0.5f, intArrayOf(100, 100)))
        assertEquals(1.0f, ScratchoffThresholdProcessor.constrainAccuracyQuality(1, 0.5f, intArrayOf(100, 100)))

        assertEquals(0.5f, ScratchoffThresholdProcessor.constrainAccuracyQuality(2, 0.5f, intArrayOf(100, 100)))
    }

    @Test
    fun testQualityLimitsDoNotAffectThresholdOfPerfectSquare() {
        val processors = ScratchoffThresholdProcessor.Quality
                .values()
                .map({
                    object: ScratchoffThresholdProcessor(10, 1f, it, LoggingDelegate()) {
                        override fun scheduleNextThresholdEvaluation() { }
                    }
                })

        val events = listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 100f, MotionEvent.ACTION_MOVE)
        )

        processors.forEach({
            it.prepare(intArrayOf(100, 100))
            it.addScratchPathPoints(events)
            it.drawQueuedScratchMotionEvents()
            it.processScratchedImagePercent()
        })

        processors.forEach({
            assertEquals(.1f, it.loggingDelegate.scratchPercent)
        })
    }

    @Test
    fun testDebounceSchedulingCallsScheduleOnlyOnceInRange() {
        var count: Int = 0

        val processor = object: ScratchoffThresholdProcessor(10, 1f, Quality.HIGH, LoggingDelegate()) {
            override fun scheduleNextThresholdEvaluation() {
                count += 1
            }
        }

        processor.prepare(intArrayOf(1, 1))

        val events = listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN)
        )

        0.until(10)
                .forEach({
                    processor.addScratchPathPoints(events)
                })

        assertEquals(1, count)
    }

    private class LoggingDelegate: ScratchoffThresholdProcessor.Delegate {

        var scratchPercent: Float = -1F
            private set

        var thresholdReachedCount: Int = 0
            private set

        override fun createScratchableRegions(source: Bitmap): MutableList<Rect> {
            return ThresholdCalculator.createFullSizeThresholdRegion(source)
        }

        override fun postScratchThresholdReached() {
            thresholdReachedCount += 1
        }

        override fun postScratchPercentChanged(percent: Float) {
            scratchPercent = percent
        }
    }

    private val ScratchoffThresholdProcessor.loggingDelegate: LoggingDelegate
        get() = this.delegate as LoggingDelegate
}