package com.jackpocket.scratchoff.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.jackpocket.scratchoff.ScratchoffController;

public class ScratchableRelativeLayout extends RelativeLayout implements ScratchableLayout {

    private final ScratchoffController controller;

    public ScratchableRelativeLayout(Context context) {
        super(context);

        this.controller = createScratchoffController();
    }

    public ScratchableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.controller = createScratchoffController();
    }

    @SuppressLint("NewApi")
    public ScratchableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.controller = createScratchoffController();
    }

    protected ScratchoffController createScratchoffController() {
        return new ScratchoffController(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return controller.parcelize(super.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        controller.setStateRestorationParcel(state);

        super.onRestoreInstanceState(state);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        controller.draw(canvas);
    }

    @Override
    public ScratchoffController getScratchoffController() {
        return controller;
    }
}