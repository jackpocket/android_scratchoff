package com.jackpocket.scratchoff.tools

import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jackpocket.scratchoff.ScratchoffThresholdProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThresholdCalculatorTests {

    @Test
    fun testCalculatePercentScratchCompletedAlwaysBetweenZeroAndOne() {
        val calculator = ThresholdCalculator(0)
        val acceptedRange = 0.0..1.0

        assert(calculator.calculate(-10, 1, 1) in acceptedRange)
        assert(calculator.calculate(10, 1, 1) in acceptedRange)
    }

    @Test
    fun testCountColorMatches() {
        val calculator = ThresholdCalculator(0)
        val subject = intArrayOf(0, 0, 1, 1, 1)

        assertEquals(2, calculator.countMatching(subject))
    }

    @Test
    fun testCreateFullSizeThresholdRegionsMatchesSourceBounds() {
        val subject = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val expected = Rect(0, 0, subject.width, subject.height)

        assertEquals(expected, ThresholdCalculator.createFullSizeThresholdRegion(subject).first())
        assertEquals(expected, ScratchoffThresholdProcessor.SimpleTargetRegionsProvider().createScratchableRegions(subject).first())
    }

    @Test
    fun testCalculatePercentageOfBitmapScratchedWithFullSizeRegionTarget() {
        val calculator = ThresholdCalculator(Color.WHITE)
        val subject = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val fullSizeRegion = ThresholdCalculator.createFullSizeThresholdRegion(subject)

        val canvas = Canvas(subject)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(0f, 0f, 5f, 5f, Paint().apply({ this.color = Color.BLACK }))

        assertEquals(25, calculator.countNotMatching(subject, fullSizeRegion.first()))
        assertEquals(0.25f, calculator.calculate(subject, fullSizeRegion))
    }

    @Test
    fun testCalculatePercentageOfBitmapScratchedWithMultiRegionTarget() {
        val calculator = ThresholdCalculator(Color.WHITE)
        val subject = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val topLeftAndBottomRight = listOf(
                Rect(0, 0, 5, 5),
                Rect(5, 5, 10, 10)
        )

        val canvas = Canvas(subject)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(0f, 0f, 10f, 5f, Paint().apply({ this.color = Color.BLACK }))

        assertEquals(25, calculator.countNotMatching(subject, topLeftAndBottomRight.first()))
        assertEquals(0.50f, calculator.calculate(subject, topLeftAndBottomRight))
    }
}