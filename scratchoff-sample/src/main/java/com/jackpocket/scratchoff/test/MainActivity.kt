package com.jackpocket.scratchoff.test

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jackpocket.scratchoff.ScratchoffController
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MainActivity: AppCompatActivity(), ScratchoffController.ThresholdChangedListener, View.OnTouchListener {

    private lateinit var controller: ScratchoffController
    private var scratchPercentTitleView = WeakReference<TextView>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        this.scratchPercentTitleView = WeakReference(findViewById(R.id.scratch_value_title))

        this.controller = ScratchoffController.findByViewId(this, R.id.scratch_view)
                .setThresholdChangedListener(this)
                .setTouchRadiusDip(this, 25)
                .setThresholdCompletionPercent(.4f)
                .setClearAnimationEnabled(true)
                .setClearAnimationDuration(1, TimeUnit.SECONDS)
                .setClearAnimationInterpolator(LinearInterpolator())
                // .setTouchRadiusPx(25)
                // .setThresholdAccuracyQuality(Quality.LOW)
                // .setThresholdTargetRegionsProvider({
                //     val inset = (min(it.width, it.height) * 0.15F).toInt()
                //
                //     listOf(
                //             Rect(inset, inset, it.width - inset, it.height - inset)
                //     )
                // })
                // .setMatchLayoutWithBehindView(findViewById(R.id.scratch_view_behind))
                // .setStateRestorationEnabled(false)
                .attach()
    }

    override fun onResume() {
        super.onResume()

        this.controller.addTouchObserver(this)
    }

    override fun onPause() {
        super.onPause()

        this.controller.removeTouchObservers()
    }

    override fun onDestroy() {
        this.controller.onDestroy()

        super.onDestroy()
    }

    override fun onScratchThresholdReached(controller: ScratchoffController) {
        // Do something?
    }

    override fun onScratchPercentChanged(controller: ScratchoffController, percentCompleted: Float) {
        scratchPercentTitleView.get()?.text = "Scratched ${percentCompleted * 100}%"
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> Log.d(TAG, "Observed ACTION_DOWN")
            MotionEvent.ACTION_UP -> Log.d(TAG, "Observed ACTION_UP")
        }

        // Our return is ignored here
        return false
    }

    fun resetActionClicked(view: View?) {
        // Reset the scratchable View's background color, as the ScratchableLayoutDrawer
        // will set the background to Color.TRANSPARENT after capturing the content
        findViewById<ScratchableLinearLayout>(R.id.scratch_view)
                .setBackgroundColor(-0xc36521)

        controller.attach()
    }

    companion object {

        val TAG = "ScratchoffTest"
    }
}