package com.unbxd.skipper.analyser.migration;

import com.google.inject.AbstractModule;

public class AnalyserMigrationModule extends AbstractModule {
    @Override
    protected void configure() {
     bind(MigrationService.class).to(MigrationServiceImpl.class).asEagerSingleton();
    }
}
