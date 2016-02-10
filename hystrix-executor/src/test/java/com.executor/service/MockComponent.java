package com.executor.service;

import java.util.concurrent.Callable;

public class MockComponent {

    public Callable<Object> forExecution(){
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };
    }

    public Callable<Object> forFallback(){
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };
    }

}
