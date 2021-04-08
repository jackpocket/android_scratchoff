package com.jackpocket.scratchoff.tools;

import android.view.View;
import android.view.ViewGroup;

public class ViewGroupVisibilityController {

    public void hide(View view) {
        if (view == null)
            return;

        view.setVisibility(View.GONE);
    }

    public void hideChildren(View view) {
        if (view == null)
            return;

        if (view instanceof ViewGroup){
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.GONE);
        }
    }

    public void showChildren(View view) {
        if (view == null)
            return;

        if (view instanceof ViewGroup){
            ViewGroup group = (ViewGroup) view;

            for(int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.VISIBLE);
        }
    }
}
