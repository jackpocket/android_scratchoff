# Upgrading from version 1.x to version 2.0.0

Version 1.x | Version 2.0.0
--- | --- 
`new ScratchoffController(Context, Runnable)` | `new ScratchoffController(View)` 
`ScratchoffController.setCompletionCallback(Runnable)` | `ScratchoffController.setThresholdChangedListener(ScratchoffController.ThresholdChangedListener)`
`ScratchoffController.setScratchValuedChangedListener(ScratchValueChangedListener)` | `ScratchoffController.setThresholdChangedListener(ScratchoffController.ThresholdChangedListener)`
`ScratchoffController.setThresholdPercent(double)` | `ScratchoffController.setThresholdCompletionPercent(float)`
`ScratchoffController.setFadeOnClear(boolean)` | `ScratchoffController.setClearAnimationEnabled(float)`
`ScratchoffController.setClearOnThresholdReached(boolean)` | `ScratchoffController.setClearOnThresholdReachedEnabled(float)`
`ScratchoffController.attach(View, View)` | `ScratchoffController.attach()`
`ScratchoffController.reset()` | `ScratchoffController.attach()`
`ScratchoffController.addPaths(List<Path>`) | `ScratchoffController.addScratchPathPoints(Collection<ScratchPathPoint>)`
`ScratchoffController.isProcessingAllowed()` | None
`ScratchoffController.onPause()` | None
`ScratchoffController.onResume()` | None
`int ScratchoffController.getTotalGridItemsCount()` | `int[] ScratchoffController.getScratchableLayoutSize()`
`ScratchValueChangedListener.onScratchPercentChanged(double)` | `ScratchoffController.Delegate.onScratchPercentChanged(ScratchoffController, float)`
`ScratchableLayout.initialize(ScratchoffController)` | None
`R.bool.scratch__clear_on_threshold_reached` | `R.bool.scratch__clear_on_threshold_reached_enabled`
`R.bool.scratch__fade_on_clear` | `R.bool.scratch__clear_animation_enabled`
`R.int.scratch__threshold_percent` | `R.int.scratch__threshold_completion_percent`

A `ScratchoffController` instance should no longer be manually created. Instead, utilize the controller instance created by the `ScratchableLayout` via `getScratchoffController()`.

```java
new ScratchoffController(context) -> ScratchoffController.findByViewId(activity, R.id.scratch_view)
```

The `ScratchoffController`'s threshold completed/changed callbacks have been merged into a weakly-held `ScratchoffController.ThresholdChangedListener`. The `ThresholdChangedListener` instance can be defined by calling `setThresholdChangedListener` on the instance.

The signature for `attach(View, View)` has been reduced to `attach()`, and `reset()` has been dropped entirely (in favor of re-using `attach()`). In order to enable the previous behavior of automated layout matching of the width/height, call the new method `setMatchLayoutWithBehindView(View)` with the behind-View param previously passed to `attach(View, View)`.

The need to directly call `ScratchoffController.onPause()` and `ScratchoffController.onResume()` has been removed, as there are no longer any long-running processes to stop or start.