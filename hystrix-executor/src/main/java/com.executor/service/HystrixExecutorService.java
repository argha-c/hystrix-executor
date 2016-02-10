package com.executor.service;

import com.executor.config.HysterixProperties;
import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.UnusedPrivateField"})
public class HystrixExecutorService {
    
    private HysterixProperties hysterixProperties;

    private final Callable executionBlock;
    private Callable fallback;
    private final String groupKey;

    public HystrixExecutorService(Callable executionBlock, String groupKey, HysterixProperties hysterixProperties) {
        this.hysterixProperties = hysterixProperties;
        this.executionBlock = executionBlock;
        this.fallback = defaultFallback();
        this.groupKey = groupKey;
    }

    public HystrixExecutorService(Callable executionBlock, Callable fallback, String groupKey,
                                  HysterixProperties hysterixProperties) {
        this.hysterixProperties = hysterixProperties;
        this.executionBlock = executionBlock;
        this.fallback = fallback;
        this.groupKey = groupKey;
    }

    public Object executeWithoutFallback(boolean circuitBreakerEnabled) {
        final HystrixCommand command = new HystrixCommand(withFallbackSetTo(circuitBreakerEnabled, false)) {
            @Override
            protected Object run() throws Exception {
                log.debug("op=circuit_breaker, status=OK, desc=Fault tolerant code execution.");
                return executionBlock.call();
            }
        };
        log.debug("op=circuit_breaker, status=OK, desc=Circuit Open : {}", command.isCircuitBreakerOpen());
        return command.execute();
    }

    public Object executeWithFallback(boolean circuitBreakerEnabled) {
        final HystrixCommand command = new HystrixCommand(withFallbackSetTo(circuitBreakerEnabled, true)) {
            @Override
            protected Object run() throws Exception {
                log.debug("op=circuit_breaker, status=OK, desc=Fault tolerant code execution.");
                return executionBlock.call();
            }

            @Override
            protected Object getFallback() {
                try {
                    log.debug("op=circuit_breaker, status=OK, desc=Executing fallback for group key : {}", groupKey);
                    return fallback.call();
                } catch (Exception e) {
                    log.debug("op=circuit_breaker, status=KO, desc=Fallback execution failed.");
                }
                return defaultFallback();
            }
        };
        log.debug("op=circuit_breaker, status=OK, desc=Circuit Open : {}", command.isCircuitBreakerOpen());
        return command.execute();
    }


    private Callable defaultFallback(){
        return new Callable() {
            @Override
            public Object call(){
                return null;
            }
        };
    }

    private HystrixCommand.Setter withFallbackSetTo(boolean circuitBreakerEnabled, boolean fallBackEnabled) {
        final HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(groupKey);
        final HystrixCircuitBreaker instance = HystrixCircuitBreaker.Factory.getInstance(commandKey);
        if (instance != null){
            log.debug("op=circuit_breaker, status=OK, desc=Circuit Status : {}", instance.isOpen());
        }

        return HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(commandKey)
                .andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter()
                .withCircuitBreakerEnabled(circuitBreakerEnabled)
                .withCircuitBreakerErrorThresholdPercentage(this.hysterixProperties.getErrorThresholdPercentage())
                .withCircuitBreakerSleepWindowInMilliseconds(this.hysterixProperties.getSleepWindowInMilliseconds())
                .withCircuitBreakerRequestVolumeThreshold(this.hysterixProperties.getRequestVolumeThreshold())
                .withMetricsRollingStatisticalWindowInMilliseconds(
                                    this.hysterixProperties.getMetricsRollingStatisticalWindowInMilliseconds())
                .withFallbackEnabled(fallBackEnabled)
                .withExecutionTimeoutInMilliseconds(this.hysterixProperties.getExecutionTimeoutInMilliseconds()));
    }

}

