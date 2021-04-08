package com.jackpocket.scratchoff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathPointsAggregator;
import com.jackpocket.scratchoff.views.ScratchableLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ScratchoffController implements OnTouchListener,
        ScratchableLayoutDrawer.Delegate,
        ScratchoffThresholdProcessor.Delegate,
        ScratchPathPointsAggregator {

    public interface ThresholdChangedListener {

        /**
         * Callback values for scratch percentages are in the range [0.0, 100.0] and will be
         * continuously called on the main Thread as the threshold value changes.
         * <br><br>
         * Updates will cease once the threshold has been reached.
         */
        public void onScratchPercentChanged(ScratchoffController controller, float percentCompleted);

        /**
         * Called once the scratch threshold has been reached.
         */
        public void onScratchThresholdReached(ScratchoffController controller);
    }

    private WeakReference<View> scratchableLayout;
    private WeakReference<View> behindView = new WeakReference<>(null);

    private WeakReference<ThresholdChangedListener> thresholdChangedListener = new WeakReference<>(null);

    private ScratchableLayoutDrawer layoutDrawer;

    private int touchRadiusPx;

    private ScratchoffThresholdProcessor thresholdProcessor;
    private ScratchoffThresholdProcessor.Quality thresholdAccuracyQuality = ScratchoffThresholdProcessor.Quality.HIGH;
    private ScratchoffThresholdProcessor.TargetRegionsProvider thresholdTargetRegionsProvider = new ScratchoffThresholdProcessor.SimpleTargetRegionsProvider();
    private float thresholdCompletionPercent;
    private boolean thresholdReached = false;

    private int[] gridSize;

    private boolean clearOnThresholdReachedEnabled;
    private boolean clearAnimationEnabled;
    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs;

    private boolean scratchableLayoutAvailable = false;
    private boolean touchInteractionIgnored = false;

    private List<OnTouchListener> touchObservers = new ArrayList<>();

    private ScratchoffState statePendingReload;
    private final LinkedBlockingQueue<ScratchPathPoint> history = new LinkedBlockingQueue<ScratchPathPoint>();
    private boolean stateRestorationEnabled;

    /**
     * Create a new {@link ScratchoffController} instance targeting a scratchable layout.
     */
    public ScratchoffController(View scratchableLayout) {
        this.scratchableLayout = new WeakReference<>(scratchableLayout);

        Resources resources = scratchableLayout
                .getContext()
                .getResources();

        this.touchRadiusPx = (int) resources.getDimension(R.dimen.scratch__touch_radius);
        this.thresholdCompletionPercent = resources.getInteger(R.integer.scratch__threshold_completion_percent) / 100f;
        this.clearOnThresholdReachedEnabled = resources.getBoolean(R.bool.scratch__clear_on_threshold_reached_enabled);
        this.clearAnimationDurationMs = resources.getInteger(R.integer.scratch__clear_animation_duration_ms);
        this.clearAnimationEnabled = resources.getBoolean(R.bool.scratch__clear_animation_enabled);
        this.stateRestorationEnabled = resources.getBoolean(R.bool.scratch__state_restoration_enabled);
    }

    /**
     * Set callbacks to be triggered when the percentage of scratched area changes
     * and the scratch threshold has been reached.
     * <br><br>
     * Callback values for scratch percentages are in the range [0.0, 100.0].
     * <br><br>
     * You must maintain a reference to the supplied {@link ThresholdChangedListener} as it will be weakly held.
     */
    public ScratchoffController setThresholdChangedListener(ThresholdChangedListener thresholdChangedListener) {
        this.thresholdChangedListener = new WeakReference<>(thresholdChangedListener);

        return this;
    }

    /**
     * When set, the system will adjust the width/height of the {@link #scratchableLayout} LayoutParams
     * to match that of the view supplied here. If null, no layout-matching will be
     * performed.
     */
    public ScratchoffController setMatchLayoutWithBehindView(View behindView) {
        this.behindView = new WeakReference<>(behindView);

        return this;
    }

    /**
     * Attach (or reset) to the pre-scratched state.
     * <br><br>
     * If the backing {@link #scratchableLayout} state is available, and
     * {@link #stateRestorationEnabled} is true, the history will be
     * automatically restored on the condition that the layout is the
     * same width/height.
     */
    public ScratchoffController attach() {
        View scratchableLayout = this.scratchableLayout.get();

        if (scratchableLayout == null)
            throw new IllegalStateException("Cannot attach to a null View!");

        this.history.clear();

        this.layoutDrawer = createLayoutDrawer()
                .attach(this, scratchableLayout, behindView.get());

        this.thresholdProcessor = createThresholdProcessor();

        scratchableLayout.setOnTouchListener(this);

        return this;
    }

    protected ScratchableLayoutDrawer createLayoutDrawer() {
        return new ScratchableLayoutDrawer(this)
                .setClearAnimationDurationMs(clearAnimationDurationMs)
                .setClearAnimationInterpolator(clearAnimationInterpolator);
    }

    protected ScratchoffThresholdProcessor createThresholdProcessor() {
        return new ScratchoffThresholdProcessor(
                getTouchRadiusPx(),
                getThresholdCompletionPercent(),
                getThresholdAccuracyQuality(),
                this);
    }

    @Override
    public void onScratchableLayoutAvailable(int width, int height) {
        this.gridSize = new int[] { width, height };

        this.scratchableLayoutAvailable = true;
        this.thresholdReached = false;

        prepareThresholdProcessor();
        performStateRestoration();
    }

    protected void prepareThresholdProcessor() {
        ScratchoffThresholdProcessor thresholdProcessor = this.thresholdProcessor;

        if (thresholdProcessor != null)
            thresholdProcessor.prepare(gridSize);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View view, MotionEvent event) {
        if (touchInteractionIgnored)
            return false;

        for (OnTouchListener observer : touchObservers) {
            observer.onTouch(view, event);
        }

        if (!scratchableLayoutAvailable)
            return false;

        List<ScratchPathPoint> events = ScratchPathPoint.create(event);

        addScratchPathPoints(events);

        return true;
    }

    /**
     * Add the collection of {@link ScratchPathPoint} instances to the
     * {@link ScratchableLayoutDrawer}, the {@link ScratchoffThresholdProcessor},
     * and the internal history queue; then invalidate the {@link #scratchableLayout}.
     * <br><br>
     * Warning: this method does not ensure the layout or aggregators
     * are actually available, yet.
     */
    @Override
    public void addScratchPathPoints(Collection<ScratchPathPoint> events) {
        addScratchPathPoints(events, layoutDrawer);
        addScratchPathPoints(events, thresholdProcessor);

        history.addAll(events);

        postInvalidateScratchableLayout();
    }

    protected void addScratchPathPoints(Collection<ScratchPathPoint> events, ScratchPathPointsAggregator listener) {
        if (listener != null)
            listener.addScratchPathPoints(events);
    }

    /**
     * Render the scratched paths to the supplied Canvas through the
     * {@link ScratchableLayoutDrawer} instance.
     */
    public void draw(Canvas canvas) {
        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.draw(canvas);
    }

    public ScratchoffController onDestroy() {
        ScratchoffThresholdProcessor thresholdProcessor = this.thresholdProcessor;

        if (thresholdProcessor != null)
            thresholdProcessor.destroy();

        removeTouchObservers();

        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.destroy();

        return this;
    }

    protected void onThresholdReached() {
        this.thresholdReached = true;

        if (clearOnThresholdReachedEnabled)
            clear();

        ThresholdChangedListener delegate = this.thresholdChangedListener.get();

        if (delegate != null)
            delegate.onScratchThresholdReached(this);
    }

    /**
     * Call this to clear/hide the {@link #scratchableLayout} and reveal the
     * {@link #behindView}. If {@link #clearAnimationEnabled} is true, the contents
     * will be faded out before altering the View's visibility.
     * <br><br>
     * Calling this will stop all processors and prevent any further scratching until
     * {@link #attach()} has been called again.
     */
    @SuppressWarnings("WeakerAccess")
    public ScratchoffController clear() {
        this.scratchableLayoutAvailable = false;

        clearLayoutDrawer(clearAnimationEnabled);

        return this;
    }

    protected void clearLayoutDrawer(boolean clearAnimationEnabled) {
        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.clear(clearAnimationEnabled);
    }

    public View getScratchImageLayout() {
        return scratchableLayout.get();
    }

    /**
     * Set the threshold percentage, between [0.0f, 1.0f], that should trigger the
     * completion callback and clearing, if enabled.
     * <br><br>
     * Note: this must be called before {@link #attach()} or it will have no effect.
     */
    public ScratchoffController setThresholdCompletionPercent(float thresholdCompletionPercent) {
        this.thresholdCompletionPercent = thresholdCompletionPercent;

        return this;
    }

    /**
     * Set whether automatic clearing of the {@link #scratchableLayout} should be performed on reaching the
     * {@link #thresholdCompletionPercent}. If false, no clearing or animations will be performed,
     * and you must manually call {@link #clear()}.
     */
    public ScratchoffController setClearOnThresholdReachedEnabled(boolean clearOnThresholdReachedEnabled) {
        this.clearOnThresholdReachedEnabled = clearOnThresholdReachedEnabled;

        return this;
    }

    /**
     * Set whether to use the fade-out AlphaAnimation, or immediately hide
     * the {@link #scratchableLayout}, on clearing.
     * <br><br>
     * If {@link #clearOnThresholdReachedEnabled} is false, this will have no effect.
     */
    public ScratchoffController setClearAnimationEnabled(boolean clearAnimationEnabled) {
        this.clearAnimationEnabled = clearAnimationEnabled;

        return this;
    }

    /**
     * Set the duration of the fade-out AlphaAnimation run on clearing.
     * <br><br>
     * If {@link #clearOnThresholdReachedEnabled} is false, this will have no effect.
     */
    public ScratchoffController setClearAnimationDuration(long value, TimeUnit unit) {
        this.clearAnimationDurationMs = unit.toMillis(value);

        return this;
    }

    /**
     * Set the Interpolator for the fade-out AlphaAnimation run on clearing. The default
     * is a LinearInterpolator.
     * <br><br>
     * If {@link #clearOnThresholdReachedEnabled} is false, this will have no effect.
     */
    public ScratchoffController setClearAnimationInterpolator(Interpolator clearAnimationInterpolator) {
        this.clearAnimationInterpolator = clearAnimationInterpolator;

        return this;
    }

    /**
     * Set the radius, in DIP, of the circle to be scratched away on MotionEvents.
     * Must be greater than 0, or throws an IllegalStateException.
     * <br><br>
     * Note: this must be called before {@link #attach()} or it will have no effect.
     */
    public ScratchoffController setTouchRadiusDip(Context context, int touchRadius) {
        return setTouchRadiusPx((int) ((touchRadius * context.getResources().getDisplayMetrics().density) + 0.5f));
    }

    /**
     * Set the radius, in pixels, of the circle to be scratched away on MotionEvents.
     * Must be greater than 0, or throws an IllegalStateException.
     * <br><br>
     * Note: this must be called before {@link #attach()} or it will have no effect.
     */
    public ScratchoffController setTouchRadiusPx(int touchRadius) {
        if (touchRadius < 1)
            throw new IllegalArgumentException("touchRadius must be greater than 0");

        this.touchRadiusPx = touchRadius;

        return this;
    }

    /**
     * Set to `true` to block all {@link MotionEvent} instances from being passed to any
     * touch observers or processors. Touches will continue to be ignored until this is
     * called again with a value of `false`.
     * <br><br>
     * This is useful for instrumentation testing or feature demonstration in conjunction
     * with {@link ScratchoffController#addScratchPathPoints(Collection)}.
     *
     * @param touchInteractionIgnored flag indicating if touches should be ignored
     */
    public ScratchoffController setTouchInteractionIgnored(boolean touchInteractionIgnored) {
        this.touchInteractionIgnored = touchInteractionIgnored;

        return this;
    }

    public float getThresholdCompletionPercent() {
        return thresholdCompletionPercent;
    }

    public int getTouchRadiusPx() {
        return touchRadiusPx;
    }

    /**
     * Set the {@link ScratchoffThresholdProcessor.Quality} for the underlying {@link ScratchoffThresholdProcessor}.
     * The default is {@link ScratchoffThresholdProcessor.Quality#HIGH}, which implies no reduction in quality.
     * <br><br>
     * {@link ScratchoffThresholdProcessor.Quality#MEDIUM} will attempt to reduce the quality to 50%, while
     * {@link ScratchoffThresholdProcessor.Quality#LOW} will use the lowest-supported quality value at runtime
     * (1 / min ({@link #touchRadiusPx}, width, height)).
     * <br><br>
     * This reduction is solely applied to elements of the {@link ScratchoffThresholdProcessor}, and does not
     * affect the drawing quality in any way.
     * <br><br>
     * If the supplied quality value is below the runtime-calculated minimum of
     * (1 / min ({@link #touchRadiusPx}, width, height)), or above the maximum (1.0f),
     * it will be ignored in favor of the minimum/maximum values.
     * <br><br>
     * Note: this must be called before {@link #attach()} or it will have no effect.
     */
    public ScratchoffController setThresholdAccuracyQuality(ScratchoffThresholdProcessor.Quality thresholdAccuracyQuality) {
        this.thresholdAccuracyQuality = thresholdAccuracyQuality;

        return this;
    }

    public ScratchoffThresholdProcessor.Quality getThresholdAccuracyQuality() {
        return thresholdAccuracyQuality;
    }

    /**
     * Override the default {@link ScratchoffThresholdProcessor.TargetRegionsProvider} for the underlying
     * {@link ScratchoffThresholdProcessor} to define specific regions of the {@link Bitmap} that should
     * be used to calculate the scratched percentage.
     * <br><br>
     * The size of the Bitmap used by the {@link ScratchoffThresholdProcessor} is determined by the
     * {@link ScratchoffThresholdProcessor.Quality} and the runtime conditions of the scratchable layout. If
     * the quality is not set to {@link ScratchoffThresholdProcessor.Quality#HIGH}, the Bitmap will likely be
     * much smaller than the size on screen.
     * <br><br>
     * It is recommended that you calculate the positions of the desired areas by their relative
     * positioning from the edges of the original Bitmap. e.g. left = 0.25 * bitmap.width
     * <br><br>
     * Warning: If any of the regions returned by the call to
     * {@link ScratchoffThresholdProcessor.TargetRegionsProvider#createScratchableRegions(Bitmap)}
     * exceed the boundaries of the supplied Bitmap, the Threshold processor will break.
     */
    public ScratchoffController setThresholdTargetRegionsProvider(ScratchoffThresholdProcessor.TargetRegionsProvider thresholdTargetRegionsProvider) {
        this.thresholdTargetRegionsProvider = thresholdTargetRegionsProvider;

        return this;
    }

    public boolean isThresholdReached() {
        return thresholdReached;
    }

    /**
     * Set whether or not the history can be restored from the scratchable View's state.
     * This restoration will occur after the call to {@link #attach()}
     * <br><br>
     * If the threshold has already been reached, the restoration will automatically
     * clear the scratchable View to review the behind View, regardless of clearing settings.
     * <br><br>
     * If the scratchable View is restored with a different layout size, no restoration
     * will be performed.
     */
    public ScratchoffController setStateRestorationEnabled(boolean stateRestorationEnabled) {
        this.stateRestorationEnabled = stateRestorationEnabled;

        return this;
    }

    public View getViewBehind() {
        return behindView.get();
    }

    public ScratchableLayoutDrawer getLayoutDrawer(){
        return layoutDrawer;
    }

    /**
     * Add an OnTouchListener to observe MotionEvents as they are passed
     * into this ScratchoffController instance. Events will be forwarded regardless of
     * the ScratchoffController's enabled state, and all return values will be ignored.
     * <br><br>
     * If adding observers (in Activity.onResume), you should also call
     * {@link #removeTouchObservers} (in Activity.onPause).
     *
     * @param touchListener a non-null OnTouchListener
     */
    public ScratchoffController addTouchObserver(OnTouchListener touchListener) {
        this.touchObservers.add(touchListener);

        return this;
    }

    /**
     * Remove a OnTouchListener from this ScratchoffController instance.
     *
     * @param touchListener a non-null OnTouchListener
     */
    public ScratchoffController removeTouchObserver(OnTouchListener touchListener) {
        this.touchObservers.remove(touchListener);

        return this;
    }

    /**
     * Remove all touch observers from this ScratchoffController instance.
     */
    public void removeTouchObservers() {
        this.touchObservers.clear();
    }

    protected List<OnTouchListener> getTouchObservers() {
        return touchObservers;
    }

    @Override
    public List<Rect> createScratchableRegions(Bitmap source) {
        return thresholdTargetRegionsProvider.createScratchableRegions(source);
    }

    public int[] getScratchableLayoutSize() {
        final int[] gridSize = this.gridSize;

        return gridSize == null
            ? new int[] { 0, 0 }
            : new int[] { gridSize[0], gridSize[1] };
    }

    public boolean isScratchableLayoutAvailable() {
        return scratchableLayoutAvailable;
    }

    @Override
    public void postScratchPercentChanged(final float percent) {
        final ThresholdChangedListener thresholdChangedListener = this.thresholdChangedListener.get();

        if (thresholdChangedListener == null)
            return;

        post(new Runnable() {
            public void run() {
                thresholdChangedListener.onScratchPercentChanged(ScratchoffController.this, percent);
            }
        });
    }

    @Override
    public void postScratchThresholdReached() {
        post(new Runnable() {
            public void run() {
                onThresholdReached();
            }
        });
    }

    public void postInvalidateScratchableLayout() {
        View layout = getScratchImageLayout();

        if (layout != null)
            layout.postInvalidate();
    }

    protected void post(Runnable runnable) {
        View layout = getScratchImageLayout();

        if (layout != null)
            layout.post(runnable);
    }

    /**
     * Create a {@link ScratchoffState} that can be used to restore the
     * drawing history of this instance.
     *
     * @return null if {@link #stateRestorationEnabled} is false
     */
    public ScratchoffState parcelize(Parcelable state) {
        if (!stateRestorationEnabled)
            return null;

        return new ScratchoffState(
                state,
                getScratchableLayoutSize(),
                thresholdReached,
                getClonedHistory());
    }

    protected List<ScratchPathPoint> getClonedHistory() {
        return Arrays.asList(history.toArray(new ScratchPathPoint[0]));
    }

    public void setStateRestorationParcel(Parcelable state) {
        if (!(stateRestorationEnabled && state instanceof ScratchoffState))
            return;

        this.statePendingReload = (ScratchoffState) state;
    }

    /**
     * Remove any pending restoration data. Calling this will ensure
     * that a subsequent call to {@link #attach()} will reset to a pre-scratched state.
     * <br><br>
     * If {@link #setStateRestorationParcel(Parcelable)} is called again, this call will have no effect.
     */
    public ScratchoffController removePendingStateRestorationParcel() {
        this.statePendingReload = null;

        return this;
    }

    protected void performStateRestoration() {
        ScratchoffState state = this.statePendingReload;

        if (state == null || !stateRestorationEnabled || !scratchableLayoutAvailable)
            return;

        this.statePendingReload = null;

        performStateRestoration(state);
    }

    protected void performStateRestoration(ScratchoffState state) {
        int[] historicalSize = state.getLayoutSize();
        int[] currentSize = getScratchableLayoutSize();

        if (!(historicalSize[0] == currentSize[0] && historicalSize[1] == currentSize[1]))
            return;

        if (state.isThresholdReached()) {
            final boolean clearAnimationEnabled = this.clearAnimationEnabled;

            this.clearAnimationEnabled = false;
            this.clear();
            this.clearAnimationEnabled = clearAnimationEnabled;

            return;
        }

        List<ScratchPathPoint> history = state.getPathHistory();

        addScratchPathPoints(history);
    }

    /**
     * Find the ScratchoffController instance by the View's ID in the Activity layout hierarchy.
     * <br><br>
     * This is equivalent to calling {@link com.jackpocket.scratchoff.views.ScratchableLinearLayout#getScratchoffController()}
     * or {@link com.jackpocket.scratchoff.views.ScratchableRelativeLayout#getScratchoffController()}.
     *
     * @param resourceId the identifier assigned to the {@link com.jackpocket.scratchoff.views.ScratchableLinearLayout}
     *   or {@link com.jackpocket.scratchoff.views.ScratchableRelativeLayout} instance in the Activity.
     */
    public static ScratchoffController findByViewId(Activity activity, int resourceId) {
        View scratchableLayout = activity.findViewById(resourceId);

        if (!(scratchableLayout instanceof ScratchableLayout))
            return null;

        return ((ScratchableLayout) scratchableLayout)
                .getScratchoffController();
    }

    /**
     * Find the ScratchoffController instance by the View's ID in the parent's layout hierarchy.
     * <br><br>
     * This is equivalent to calling {@link com.jackpocket.scratchoff.views.ScratchableLayout#getScratchoffController()}.
     *
     * @param resourceId the identifier assigned to the {@link com.jackpocket.scratchoff.views.ScratchableLayout}
     *   instance in the ViewGroup.
     */
    public static ScratchoffController findByViewId(ViewGroup parent, int resourceId) {
        View scratchableLayout = parent.findViewById(resourceId);

        if (!(scratchableLayout instanceof ScratchableLayout))
            return null;

        return ((ScratchableLayout) scratchableLayout)
                .getScratchoffController();
    }
}
