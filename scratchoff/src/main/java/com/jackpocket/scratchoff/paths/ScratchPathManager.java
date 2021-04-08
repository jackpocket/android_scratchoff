package com.jackpocket.scratchoff.paths;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collection;

public class ScratchPathManager implements ScratchPathPointsAggregator {

    private static final int POINTER_LIMIT = 10;

    private Path[] activePaths = new Path[POINTER_LIMIT];
    private int[] lastActiveActions = new int[POINTER_LIMIT];

    private final ArrayList<Path> paths = new ArrayList<>();
    private float scale = 1f;

    public ScratchPathManager() { }

    public ScratchPathManager setScale(float scale) {
        this.scale = scale;

        return this;
    }

    @Override
    public void addScratchPathPoints(Collection<ScratchPathPoint> events) {
        for (ScratchPathPoint event : events)
            addScratchPathPoint(event);
    }

    public void addScratchPathPoint(ScratchPathPoint event) {
        if (POINTER_LIMIT <= event.pointerIndex)
            return;

        synchronized (paths) {
            switch (event.action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    handleTouchDown(
                            event.pointerIndex,
                            event.x * scale,
                            event.y * scale);

                    break;
                default:
                    handleTouchMove(
                            event.pointerIndex,
                            event.x * scale,
                            event.y * scale);

                    break;
            }

            lastActiveActions[event.pointerIndex] = event.action;
        }
    }

    protected void handleTouchDown(int pointerIndex, float x, float y) {
        createPath(pointerIndex, x, y);
    }

    protected void handleTouchMove(int pointerIndex, float x, float y) {
        // If the last event for this pointer was MotionEvent.ACTION_POINTER_UP
        // then it's possible the position has changed, so we should recreate
        // the Path to avoid errors
        if (MotionEvent.ACTION_POINTER_UP == lastActiveActions[pointerIndex])
            createPath(pointerIndex, x, y);

        Path activePath = this.activePaths[pointerIndex];

        // If the active Path has been drawn, it would have been reset to an empty state
        if (activePath.isEmpty())
            activePath.moveTo(x, y);

        activePath.lineTo(x, y);
    }

    protected void createPath(int pointerIndex, float x, float y) {
        Path activePath = new Path();
        activePath.moveTo(x, y);

        this.activePaths[pointerIndex] = activePath;

        this.paths.add(activePath);
    }

    /**
     * Draw the current Path segments and reset them to an empty state.
     *
     * @param canvas The Canvas to draw the un-rendered path segments to
     * @param paint The paint to draw the un-rendered paths segments with
     */
    public void drawAndReset(Canvas canvas, Paint paint) {
        synchronized (paths) {
            for (Path path : paths) {
                canvas.drawPath(path, paint);

                path.reset();
            }
        }
    }

    public void clear() {
        synchronized (paths) {
            this.activePaths = new Path[POINTER_LIMIT];
            this.lastActiveActions = new int[POINTER_LIMIT];
            this.paths.clear();
        }
    }

    protected ArrayList<Path> getPaths() {
        return paths;
    }

    public static Paint createBaseScratchoffPaint(int touchRadiusPx) {
        Paint markerPaint = new Paint();
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeCap(Paint.Cap.ROUND);
        markerPaint.setStrokeJoin(Paint.Join.ROUND);
        markerPaint.setStrokeWidth(touchRadiusPx * 2);

        return markerPaint;
    }
}
