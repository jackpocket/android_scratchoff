package com.jackpocket.scratchoff;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;

import java.util.List;

public class ScratchoffState extends View.BaseSavedState {

    private int[] size;
    private boolean thresholdReached;
    private List<ScratchPathPoint> events;

    public ScratchoffState(
            Parcelable state,
            int[] size,
            boolean thresholdReached,
            List<ScratchPathPoint> events) {

        super(state);

        this.size = size;
        this.thresholdReached = thresholdReached;
        this.events = events;
    }

    public ScratchoffState(Parcel in) {
        super(in);

        this.size = new int[] { in.readInt(), in.readInt() };
        this.thresholdReached = in.readInt() == 1;
        this.events = in.createTypedArrayList(ScratchPathPoint.CREATOR);
    }

    public int[] getLayoutSize() {
        return size;
    }

    public boolean isThresholdReached() {
        return thresholdReached;
    }

    public List<ScratchPathPoint> getPathHistory() {
        return events;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeInt(size[0]);
        dest.writeInt(size[1]);
        dest.writeInt(thresholdReached ? 1 : 0);
        dest.writeTypedList(events);
    }

    public static final Parcelable.Creator<ScratchoffState> CREATOR = new Parcelable.Creator<ScratchoffState>() {

        @Override
        public ScratchoffState createFromParcel(Parcel in) {
            return new ScratchoffState(in);
        }

        @Override
        public ScratchoffState[] newArray(int size) {
            return new ScratchoffState[size];
        }
    };
}
