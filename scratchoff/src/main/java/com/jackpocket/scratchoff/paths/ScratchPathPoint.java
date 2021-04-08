package com.jackpocket.scratchoff.paths;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class ScratchPathPoint implements Parcelable {

    public final int pointerIndex;
    public final float x;
    public final float y;
    public final int action;

    public ScratchPathPoint(
            int pointerIndex,
            float x,
            float y,
            int action) {

        this.pointerIndex = pointerIndex;
        this.x = x;
        this.y = y;
        this.action = action;
    }

    protected ScratchPathPoint(Parcel in) {
        this.pointerIndex = in.readInt();
        this.x = in.readFloat();
        this.y = in.readFloat();
        this.action = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pointerIndex);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeInt(action);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ScratchPathPoint))
            return false;

        ScratchPathPoint another = (ScratchPathPoint) obj;

        return another.x == this.x
                && another.y == this.y
                && another.action == this.action;
    }

    public static List<ScratchPathPoint> create(MotionEvent event) {
        final int historySize = event.getHistorySize();
        final int pointersCount = event.getPointerCount();

        ArrayList<ScratchPathPoint> events = new ArrayList<ScratchPathPoint>((historySize * pointersCount) + pointersCount);

        for (int historyIndex = 0; historyIndex < historySize; historyIndex++) {
            for (int pointerIndex = 0; pointerIndex < pointersCount; pointerIndex++) {
                events.add(
                        new ScratchPathPoint(
                                pointerIndex,
                                event.getHistoricalX(pointerIndex, historyIndex),
                                event.getHistoricalY(pointerIndex, historyIndex),
                                MotionEvent.ACTION_MOVE));
            }
        }

        for (int pointerIndex = 0; pointerIndex < pointersCount; pointerIndex++) {
            events.add(
                    new ScratchPathPoint(
                            pointerIndex,
                            event.getX(pointerIndex),
                            event.getY(pointerIndex),
                            event.getActionMasked()));
        }

        return events;
    }

    public static final Creator<ScratchPathPoint> CREATOR = new Creator<ScratchPathPoint>() {

        @Override
        public ScratchPathPoint createFromParcel(Parcel in) {
            return new ScratchPathPoint(in);
        }

        @Override
        public ScratchPathPoint[] newArray(int size) {
            return new ScratchPathPoint[size];
        }
    };
}
