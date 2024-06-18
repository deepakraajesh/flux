package com.unbxd.skipper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import ro.pippo.controller.ControllerApplication;
import ro.pippo.core.Pippo;

public class SkipperApp {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new SkipperModule());
        ControllerApplication skipperLauncher = injector.getInstance(SkipperLauncher.class);

        // start pippo Server
        (new Pippo(skipperLauncher)).start();
    }
}

