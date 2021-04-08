package com.jackpocket.scratchoff.paths;

import java.util.Collection;

public interface ScratchPathPointsAggregator {
    public void addScratchPathPoints(Collection<ScratchPathPoint> events);
}
