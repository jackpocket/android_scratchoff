# Scratchoff

[![Download](https://img.shields.io/maven-central/v/com.jackpocket/scratchoff)](https://search.maven.org/artifact/com.jackpocket/scratchoff)

A simple library for implementing scratchable Views.

![Scratchoff Sample](https://github.com/jackpocket/android_scratchoff/raw/master/scratchoff.gif)

## Installation

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile('com.jackpocket:scratchoff:2.0.0')
}
```

## Usage

### Layout Setup

First, you need a parent `ViewGroup` that can vertically stack: 

1. a behind-View to be revealed
2. a foreground-View to be scratched away

Here is an example using the `ScratchableLinearLayout`:

```xml
<RelativeLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent" >

    <RelativeLayout
        android:id="@+id/scratch_view_behind"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#818B8D"
        android:padding="25dip" >

        <ImageView
            android:layout_width="fill_parent"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            android:adjustViewBounds="true"
            android:src="@drawable/some_drawable_to_be_revealed" />

    </RelativeLayout>

    <com.jackpocket.scratchoff.views.ScratchableLinearLayout
        android:id="@+id/scratch_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#3C9ADF"
        android:padding="25dip" >

        <ImageView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:adjustViewBounds="true"
            android:gravity="center"
            android:src="@drawable/some_top_drawable" />

    </com.jackpocket.scratchoff.views.ScratchableLinearLayout>
</RelativeLayout>
```

By default, the `ScratchoffController` will not adjust the width or height of the scratchable layout's `LayoutParams`. To enable this behavior, call `setMatchLayoutWithBehindView(View)` with the behind-View whose width/height should be matched with before `attach()`.

If you choose not to utilize the available `ScratchableLinearLayout` or the `ScratchableRelativeLayout`, you will need to manually handle delegation to `ScratchoffController.onDraw(Canvas)`.

### Scratch Threshold Changed / Threshold Reached Callback Setup

The `ScratchoffController` will call the supplied `ScratchoffController.ThresholdChangedListener` methods when the scratched threshold has changed or has been reached, but a strong reference to it must be maintained.

```java
public class MainActivity extends Activity implements ScratchoffController.ThresholdChangedListener {

    @Override
    public void onScratchPercentChanged(ScratchoffController controller, float percentCompleted) {
        // This will be called on the main thread any time the scratch threshold has changed.
        // The values will be between [0.0, 100.0]
    }

    @Override
    public void onScratchThresholdReached(ScratchoffController controller) {
        // This is called on the main thread the moment we know the scratched threshold has been reached.
        // If the fade-on-clear animation is enabled, it will already have been started, but not completed.
    }
}
```

### Finding the `ScratchoffController` 

Find the `ScratchoffController` from the `ScratchableLayout` instances defined in the layout resource.

```java
// Find the ScratchoffController in your Activity layout
ScratchoffController.findByViewId(activity, R.id.scratch_view)

// Find the ScratchoffController in some View's layout
ScratchoffController.findByViewId(view, R.id.scratch_view)

// Find the ScratchoffController manually
((ScratchableLayout) activity.findViewById(R.id.scratch_view))
    .getScratchoffController()
``` 

### Listening for threshold change / completion events

In order to receive threshold change and completion events, a `ScratchoffController.ThresholdChangedListener` must be set on the `ScratchoffController` instance.

```java
ScratchoffController.findByViewId(activity, R.id.scratch_view)
    .setThresholdChangedListener(listener)
    ...
```

Note: The `ScratchoffController.ThresholdChangedListener` instance is weakly-held

### Tweaking the settings

Certain properties, like thresholds, scratch sizes, or clearing animation behavior, can be overridden before attaching the `ScratchoffController` instance.

```java
ScratchoffController.findByViewId(activity, R.id.scratch_view)
    ...
    .setThresholdPercent(0.40f)
    .setTouchRadiusDip(context, 30)
    .setClearAnimationEnabled(true)
    .setClearOnThresholdReached(true)
    ...
```

### Attaching the `ScratchoffController`

To start the processors and allow scratching, call `attach()` on the `ScratchoffController` instance. 

```java
ScratchoffController.findByViewId(activity, R.id.scratch_view)
    ...
    .attach();
```

If the `ScratchableLayout` View has been restored, the dimensions match the persisted values, and state-restoration is enabled on the `ScratchoffController` instance, then attaching will attempt to restore the scratched path history from the cached state. If the restored state's threshold has already been reached, the content will be automatically cleared, regardless of desired clear animation behavior. 

### Lifecycle

Ensure that `onDestroy()` is called from the correct lifecycle method so that resources can be properly recycled.

```java
@Override
public void onDestroy(){
    controller.onDestroy();

    super.onDestroy();
}
```

### Re-using the `ScratchoffController`

The `ScratchoffController` can be reset with the same call that started it: `ScratchController.attach()`. 

However, **the background color of your scratchable layout must be manually set back to something opaque before calling it**, as the `ScratchableLayoutDrawer` will set the background to transparent when scratching is enabled.

```java
public void onScratchThresholdReached(ScratchoffController controller) {
    // Make sure to set the background of the foreground-View. 
    // Don't worry, it's hidden if it cleared or still clearing.
    findViewById(R.id.scratch_view)
        .setBackgroundColor(0xFF3C9ADF);

    // Reset after a delay, as the clearing animation may still be running at this point
    new Handler(Looper.getMainLooper())
        .postDelayed(() -> controller.attach(), 2000);
}
```

### Extra: Observing MotionEvents

You can add an `OnTouchListener` to the `ScratchoffController` to observe `MotionEvents` as they come in, regardless of enabled state. When adding these observers, make sure to remove them in the appropriate lifecycle methods.

```java
@Override
public void onPause(){
    ...
    controller.removeTouchObservers()

    super.onPause()
}

@Override
public void onResume(){
    super.onResume()

    controller.addTouchObserver((view, event) -> {
        // Do something on a particular MotionEvent?
    });
    ...
}
```

**Note 1**: the return values of `onTouch` will be ignored, as the `ScratchoffController` must maintain control of the touch event collection.

**Note 2**: all touch observers will automatically be removed when calling `ScratchoffController.onDestroy()`

### Extra: Threshold Evaluation Memory Reduction

By default, the `ScratchoffController` will use a Bitmap of the same size as the actual scratchable layout. To reduce the memory impact, you can set a `ScratchoffThresholdProcessor.Quality` value other than `HIGH`. A `MEDIUM` `Quality` would attempt a 50% reduction in size, while `LOW` will attempt to go as low as `1 / min(touchRadius, width, height)`. The scalar is then applied to both the x and y coordinates of the touch events used to calculate the threshold.

```java
ScratchoffController.findByViewId(activity, R.id.scratch_view)
    ...
    .setThresholdAccuracyQuality(ScratchoffThresholdProcessor.Quality.LOW)
```

### Extra: Evaluating Scratched Percentage Of Specific Regions

The `ScratchoffController` can be configured to calculate the thresholds of specific rectangular regions by supplying it with a `ScratchoffThresholdProcessor.TargetRegionsProvider`. Only regions returned by the call to `createScratchableRegions` would be used for evaluating which areas of a scratchable layout are used when determining the total scratched percentage.

```java
ScratchoffController.findByViewId(activity, R.id.scratch_view)
    ...
    .setThresholdTargetRegionsProvider((source) -> {
        ArrayList<Rect> regions = new ArrayList<>();
        regions.add(Rect(0, 0, source.getWidth(), source.getHeight()))

        return regions;
    })
```

The size of the Bitmap used by the `ScratchoffThresholdProcessor` is determined by the `ScratchoffThresholdProcessor.Quality` and the runtime conditions of the scratchable layout. If the quality is not set to `ScratchoffThresholdProcessor.Quality#HIGH`, the Bitmap will likely be much smaller than the size on screen.

It is recommended that you calculate the positions of the desired regions by their relative positioning from the edges of the original Bitmap. e.g. left = 0.25 * bitmap.width

## Upgrading from Version 1.x to Version 2.0.0

Follow the [upgrade guide](https://github.com/jackpocket/android_scratchoff/raw/master/upgrade_1.x-2.0.md).

### Moved to MavenCentral

As of version 2.0.0, scratchoff will be hosted on MavenCentral. Versions 1.x and below will remain on JCenter.

## Additional credits:
+ [scratchoff-test/src/res/drawable/ic_touch_indicator.xml](https://www.svgrepo.com/svg/9543/touch)