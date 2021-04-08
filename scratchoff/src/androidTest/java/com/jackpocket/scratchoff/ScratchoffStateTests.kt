package com.jackpocket.scratchoff

import android.os.Parcel
import android.view.AbsSavedState
import android.view.MotionEvent
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ScratchoffStateTests {

    @Test
    fun testStateRestorationFromParcel() {
        val expectedPathPoint = ScratchPathPoint(0, 5f, 5f, MotionEvent.ACTION_DOWN)
        val origin = ScratchoffState(
                AbsSavedState.EMPTY_STATE,
                intArrayOf(10, 20),
                true,
                listOf(expectedPathPoint))

        val parcel = parcelizeForRead(origin)
        val state = ScratchoffState.CREATOR.createFromParcel(parcel)

        assertEquals(10, state.layoutSize[0])
        assertEquals(20, state.layoutSize[1])
        assertEquals(true, state.isThresholdReached)
        assertEquals(expectedPathPoint, state.pathHistory[0])
    }

    private fun parcelizeForRead(state: ScratchoffState): Parcel {
        val parcel = Parcel.obtain()

        state.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        return parcel
    }
}