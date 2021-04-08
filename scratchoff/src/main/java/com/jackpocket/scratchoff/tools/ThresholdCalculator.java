package com.jackpocket.scratchoff.tools;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class ThresholdCalculator {

    private int unscratchedColor;

    public ThresholdCalculator(int unscratchedColor) {
        this.unscratchedColor = unscratchedColor;
    }

    public float calculate(Bitmap bitmap, List<Rect> regions) {
        float matchesSum = 0F;

        for (Rect region : regions) {
            matchesSum += calculate(
                    countNotMatching(bitmap, region),
                    region.width(),
                    region.height());
        }

        return matchesSum / regions.size();
    }

    public float calculate(int scratchedCount, int width, int height) {
        return Math.min(1, Math.max(0, ((float) scratchedCount) / (width * height)));
    }

    public int countNotMatching(Bitmap bitmap, Rect region) {
        int pixelCount = region.width() * region.height();
        int[] pixels = new int[pixelCount];

        bitmap.getPixels(
                pixels,
                0,
                region.width(),
                region.left,
                region.top,
                region.width(),
                region.height());

        return pixelCount - countMatching(pixels);
    }

    int countMatching(int[] pixels) {
        int scratched = 0;

        for (int pixel : pixels) {
            if (pixel == unscratchedColor)
                scratched++;
        }

        return scratched;
    }

    public static List<Rect> createFullSizeThresholdRegion(Bitmap source) {
        ArrayList<Rect> regions = new ArrayList<Rect>();
        regions.add(new Rect(0, 0, source.getWidth(), source.getHeight()));

        return regions;
    }
}
