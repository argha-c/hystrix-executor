package com.executor.service;

import static com.executor.config.HysterixConstants.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.executor.config.HysterixProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HystrixExecutorServiceTest {

    @Mock
    private MockComponent mockComponent;

    private HysterixProperties hysterixProperties = new HysterixProperties(ERROR_THRESHOLD_PERCENTAGE_DEFAULT,
                                                                        EXECUTION_TIMEOUT_DEFAULT,
                                                                        REQUEST_VOLUME_THRESHOLD_DEFAULT,
                                                                        SLEEP_WINDOW_DEFAULT,
                                                                        ROLLING_STATISTICAL_WINDOW_DEFAULT);

    @Test
    public void shouldInvokeFallbackWhenRequestedByClient() throws Exception {
        final HystrixExecutorService executorService = new HystrixExecutorService(mockComponent.forExecution(),
                mockComponent.forFallback(),
                "with_fallback", hysterixProperties);
        executorService.executeWithFallback(true);

        verify(mockComponent, times(1)).forExecution();
        verify(mockComponent, times(1)).forFallback();
    }

    @Test(expected = HystrixRuntimeException.class)
    public void shouldNotInvokeFallbackWhenNotRequested() throws Exception {
        final HystrixExecutorService executorService = new HystrixExecutorService(mockComponent.forExecution(),
                "without_fallback", hysterixProperties);
        executorService.executeWithoutFallback(true);
    }

    @Test(expected = HystrixRuntimeException.class)
    public void shouldNotEnableCircuitBreakerWhenToggledOff() {
        final HystrixExecutorService executorService = new HystrixExecutorService(mockComponent.forExecution(),
                "without_fallback", hysterixProperties);

        final boolean circuitBreakerEnabled = false;
        executorService.executeWithoutFallback(circuitBreakerEnabled);

    }

}