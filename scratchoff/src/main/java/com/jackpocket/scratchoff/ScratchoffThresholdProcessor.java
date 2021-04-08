package com.jackpocket.scratchoff;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.jackpocket.scratchoff.paths.ScratchPathManager;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathPointsAggregator;
import com.jackpocket.scratchoff.tools.ThresholdCalculator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ScratchoffThresholdProcessor implements ScratchPathPointsAggregator, Runnable {

    public interface TargetRegionsProvider {
        public List<Rect> createScratchableRegions(Bitmap source);
    }

    public interface Delegate extends TargetRegionsProvider {
        public void postScratchPercentChanged(float percent);
        public void postScratchThresholdReached();
    }

    public enum Quality {
        LOW,
        MEDIUM,
        HIGH
    }

    private static final int SLEEP_DELAY_START = 100;

    private static final int MARKER_UNTOUCHED = 0xFFFFFFFF;
    private static final int MARKER_SCRATCHED = 0xFF000000;

    private static final int PERCENT_SCRATCHED_UNTOUCHED = -1;

    private final WeakReference<Delegate> delegate;

    private Bitmap currentBitmap;
    private Canvas canvas;
    private final Paint markerPaint;

    private double lastPercentScratched = PERCENT_SCRATCHED_UNTOUCHED;

    private final float completionThreshold;
    private boolean thresholdReached = false;

    private final ScratchPathManager pathManager = new ScratchPathManager();
    private final LinkedBlockingQueue<ScratchPathPoint> queue = new LinkedBlockingQueue<ScratchPathPoint>();

    private final int originalTouchRadius;
    private final Quality accuracyQuality;
    private final ThresholdCalculator calculator = new ThresholdCalculator(MARKER_UNTOUCHED);
    private List<Rect> thresholdRegions = new ArrayList<Rect>();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final long evaluationDebounceDurationMs = 50L;
    private final AtomicLong nextScheduledEvaluation = new AtomicLong(0L);

    @SuppressWarnings("WeakerAccess")
    public ScratchoffThresholdProcessor(
            int touchRadiusPx,
            float completionThreshold,
            Quality accuracyQuality,
            Delegate delegate) {

        this.originalTouchRadius = touchRadiusPx;
        this.completionThreshold = completionThreshold;
        this.accuracyQuality = accuracyQuality;
        this.delegate = new WeakReference<>(delegate);

        this.markerPaint = ScratchPathManager.createBaseScratchoffPaint(touchRadiusPx);
        this.markerPaint.setColor(MARKER_SCRATCHED);
        this.markerPaint.setAntiAlias(false);
    }

    public void prepare(int[] layoutSize) {
        synchronized (pathManager) {
            Delegate delegate = this.delegate.get();

            if (delegate == null || layoutSize[0] < 1 || layoutSize[1] < 1)
                return;

            float accuracyQuality = constrainAccuracyQuality(
                    this.originalTouchRadius,
                    this.accuracyQuality,
                    layoutSize);

            float width = layoutSize[0] * accuracyQuality;
            float aspectRatio = layoutSize[1] / (float) layoutSize[0];
            float height = width * aspectRatio;
            float touchRadius = originalTouchRadius * accuracyQuality;

            this.currentBitmap = Bitmap.createBitmap(
                    (int) width,
                    (int) height,
                    Bitmap.Config.RGB_565);

            this.markerPaint.setStrokeWidth(touchRadius * 2);
            this.pathManager.setScale(accuracyQuality);

            this.thresholdRegions = delegate.createScratchableRegions(currentBitmap);

            this.canvas = new Canvas(currentBitmap);
            this.canvas.drawColor(MARKER_UNTOUCHED);

            // The continuous drawing of incomplete Path elements affects the
            // drawing at the pixel-level, and the result may differ from the single-redraw
            // performed when restoring the Path elements from the history.
            // Thus, we cannot expect the threshold calculated from a historical reload
            // to be exactly equal to the original, without emulating the original drawing
            // of the MotionEvents as they came in. Since that would be super-inefficient,
            // I have no more tears to give this problem, the ThresholdProcessor instances are
            // not re-used after resets, and the loss is limited to less than 0.001%,
            // we can just pretend that doesn't really happen and move on with our lives...
            pathManager.drawAndReset(canvas, markerPaint);
        }
    }

    protected static float constrainAccuracyQuality(
            int touchRadius,
            Quality quality,
            int[] layoutSize) {

        switch (quality) {
            case LOW:
                return constrainAccuracyQuality(touchRadius, 0.01f, layoutSize);
            case MEDIUM:
                return constrainAccuracyQuality(touchRadius, 0.5f, layoutSize);
            default:
                return 1.0f;
        }
    }

    protected static float constrainAccuracyQuality(
            int touchRadius,
            float accuracyQuality,
            int[] layoutSize) {

        float minimumAccuracyQuality = 1 / (float) Math.min(touchRadius, Math.min(layoutSize[0], layoutSize[1]));

        return Math.min(1f, Math.max(minimumAccuracyQuality, accuracyQuality));
    }

    @Override
    public void addScratchPathPoints(Collection<ScratchPathPoint> events) {
        synchronized (pathManager) {
            if (currentBitmap == null || thresholdReached)
                return;
        }

        queue.addAll(events);

        debounceThresholdEvaluationScheduling();
    }

    protected void debounceThresholdEvaluationScheduling() {
        long currentScheduledEvaluation = nextScheduledEvaluation.getAndSet(System.currentTimeMillis() + evaluationDebounceDurationMs);

        // If the next check has already been scheduled for some time
        // in the future, drop this schedule request
        if (System.currentTimeMillis() < currentScheduledEvaluation) {
            nextScheduledEvaluation.set(currentScheduledEvaluation);

            return;
        }

        scheduleNextThresholdEvaluation();
    }

    protected void scheduleNextThresholdEvaluation() {
        executor.schedule(this, evaluationDebounceDurationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        synchronized (pathManager) {
            if (currentBitmap == null || thresholdReached)
                return;

            if (!(drawQueuedScratchMotionEvents() || lastPercentScratched == PERCENT_SCRATCHED_UNTOUCHED))
                return;

            processScratchedImagePercent();
        }
    }

    protected boolean drawQueuedScratchMotionEvents() {
        List<ScratchPathPoint> dequeuedEvents = new ArrayList<ScratchPathPoint>();

        queue.drainTo(dequeuedEvents);

        if (dequeuedEvents.size() < 1)
            return false;

        pathManager.addScratchPathPoints(dequeuedEvents);
        pathManager.drawAndReset(canvas, markerPaint);

        return true;
    }

    protected void processScratchedImagePercent() {
        Delegate delegate = this.delegate.get();
        Bitmap currentBitmap = this.currentBitmap;

        if (delegate == null || currentBitmap == null || thresholdReached)
            return;

        float percentScratched = calculator.calculate(currentBitmap, thresholdRegions);

        if (this.lastPercentScratched < percentScratched) {
            delegate.postScratchPercentChanged(percentScratched);
        }

        if (completionThreshold <= percentScratched) {
            this.thresholdReached = true;

            delegate.postScratchThresholdReached();
        }

        this.lastPercentScratched = percentScratched;
    }

    public void destroy() {
        try {
            synchronized (pathManager) {
                if (currentBitmap == null)
                    return;

                currentBitmap.recycle();
                currentBitmap = null;

                canvas = null;
            }
        }
        catch(Exception e){ e.printStackTrace(); }
    }

    protected Delegate getDelegate() {
        return delegate.get();
    }

    public static class SimpleTargetRegionsProvider implements TargetRegionsProvider {

        @Override
        public List<Rect> createScratchableRegions(Bitmap source) {
            return ThresholdCalculator.createFullSizeThresholdRegion(source);
        }
    }
}
