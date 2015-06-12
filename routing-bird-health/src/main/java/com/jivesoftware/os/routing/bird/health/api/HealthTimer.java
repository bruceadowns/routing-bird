package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.mlogger.core.CountersAndTimers;
import com.jivesoftware.os.mlogger.core.Timer;

/**
 *
 * @author jonathan.colt
 */
public class HealthTimer {

    private final CountersAndTimers countersAndTimers;
    private final String name;
    private final HealthChecker<Timer> healthChecker;

    public HealthTimer(CountersAndTimers countersAndTimers, String name, HealthChecker<Timer> healthChecker) {
        this.countersAndTimers = countersAndTimers;
        this.name = name;
        this.healthChecker = healthChecker;
    }

    public void startTimer() {
        countersAndTimers.startTimer(name);
    }

    public long stopTimer(String description, String resolution) {
        Timer timer = countersAndTimers.stopAndGetTimer(name, name, 5000);
        healthChecker.check(timer, description, resolution);
        return timer.getLastSample();
    }

}
