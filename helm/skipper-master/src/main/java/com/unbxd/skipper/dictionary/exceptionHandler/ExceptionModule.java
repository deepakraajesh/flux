package com.unbxd.skipper.dictionary.exceptionHandler;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class ExceptionModule extends AbstractModule {
    @Override
    public void configure() {
        Multibinder<ExceptionHandler> ehandlers = Multibinder.newSetBinder(
                binder(), ExceptionHandler.class
        );
         ehandlers.addBinding().to(AnalyserExceptionHandler.class);
         ehandlers.addBinding().to(AssetExceptionHandler.class);
         ehandlers.addBinding().to(JsonProcessExHandler.class);
    }
}
