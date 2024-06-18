package com.unbxd.skipper.analyser.migration;


import java.util.Map;

public interface MigrationService {
    Map<String, Object> migrateToSelfServe(String siteKey, String cookie) throws AnalyserMigrationException;

}
