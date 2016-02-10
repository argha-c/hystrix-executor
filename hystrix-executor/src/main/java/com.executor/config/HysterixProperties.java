package com.executor.config;

import lombok.Value;

@Value
public class HysterixProperties {
    private final int errorThresholdPercentage;
    private final int executionTimeoutInMilliseconds;
    private final int requestVolumeThreshold;
    private final int sleepWindowInMilliseconds;
    private final int metricsRollingStatisticalWindowInMilliseconds;
    private static final HysterixProperties DEFAULT = new HysterixProperties(50, 10000, 3, 60000, 60000);
}
