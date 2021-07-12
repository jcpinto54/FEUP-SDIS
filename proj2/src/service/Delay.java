package service;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Delay {
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(16);

    public static ScheduledFuture<?> executePeriodically(Runnable command, long delay) {
        return executor.scheduleAtFixedRate(command, delay, delay, TimeUnit.SECONDS);
    }
}
