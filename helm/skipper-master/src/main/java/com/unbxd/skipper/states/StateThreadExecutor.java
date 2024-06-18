package com.unbxd.skipper.states;

import com.google.inject.Singleton;
import com.unbxd.skipper.states.ServeState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class StateThreadExecutor {

    private ExecutorService executorService;

    public StateThreadExecutor() {
        executorService = Executors.newFixedThreadPool(30);
    }

    public void submitState(ServeState state) {
        executorService.submit(state);
    }
}
