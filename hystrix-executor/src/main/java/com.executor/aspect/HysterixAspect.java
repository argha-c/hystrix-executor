package com.executor.aspect;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
@Slf4j
@SuppressWarnings({"PMD"})
public class HysterixAspect {

    @Around("@annotation(com.executor.aspect.CircuitBreaker)")
    public Object circuitBreakerAround(final ProceedingJoinPoint joinPoint) {
        final String methodName = joinPoint.getSignature().getName();
        final Object targetObject = joinPoint.getTarget();
        final Class<?> clazz = targetObject.getClass();
        final String targetMethod = clazz.getSimpleName() + "::" + methodName;

        final Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(targetMethod))
                .andCommandKey(HystrixCommandKey.Factory.asKey(methodName))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                                .withFallbackEnabled(true)
                                .withExecutionTimeoutInMilliseconds(10000));

        HystrixCommand command = new HystrixCommand(setter) {
            @Override
            protected Object run() throws Exception{
                try {
                    return joinPoint.proceed();
                } catch (Exception ex) {
                    log.error("JointPoint proceed failed.");
                    throw ex;
                } catch (Throwable throwable) {
                    log.error("JointPoint proceed failed.");
                    throw new Exception();
                }
            }

            @Override
            protected Object getFallback() {
                try {
                    final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                    final CircuitBreaker annotation = methodSignature.getMethod().getAnnotation(CircuitBreaker.class);
                    final Method execute = clazz.getDeclaredMethod(annotation.fallback(), null);

                    return execute.invoke(targetObject);

                } catch (NoSuchMethodException e) {
                    log.error("No fallback method defined for this command.");
                } catch (InvocationTargetException e) {
                    log.error("Fallback failed for this command.");
                } catch (IllegalAccessException e) {
                    log.error("Fallback failed for this command.");
                }
                return null;
            }
        };
        return command.execute();
    }

}
