package com.unbxd.skipper;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.analyser.AnalyserModule;
import com.unbxd.auth.AuthModule;
import com.unbxd.config.ConfigModule;
import com.unbxd.console.ConsoleModule;
import com.unbxd.event.ReportModule;
import com.unbxd.field.FieldModule;
import com.unbxd.gcp.GCPModule;
import com.unbxd.mongo.MongoModule;
import com.unbxd.pim.PIMModule;
import com.unbxd.pim.event.EventModule;
import com.unbxd.recommend.RecommendModule;
import com.unbxd.s3.S3Module;
import com.unbxd.skipper.dictionary.DictionaryModule;
import com.unbxd.skipper.feed.dim.DIMModule;
import com.unbxd.skipper.analyser.migration.AnalyserMigrationModule;
import com.unbxd.skipper.plugins.PluginModule;
import com.unbxd.skipper.variants.VariantsModule;
import com.unbxd.search.SearchModule;
import com.unbxd.search.feed.SearchFeedModule;
import com.unbxd.skipper.autosuggest.AutosuggestModule;
import com.unbxd.skipper.controller.*;
import com.unbxd.skipper.feed.FeedModule;
import com.unbxd.skipper.relevancy.RelevancyModule;
import com.unbxd.skipper.site.SiteModule;
import com.unbxd.skipper.states.StateModule;
import ro.pippo.controller.Controller;

import java.util.Properties;

import static com.amazonaws.regions.Regions.US_WEST_1;

public class SkipperModule extends AbstractModule {


    @Override
    protected void configure() {
        bindS3Module();
        bindConfigModule();
        buildAuthModule();
        buildMongoModule();
        buildGimliModule();
        buildPIMModule();
        buildFeedModule();
        buildAsterixModule();
        bindDictionaryModule();
        buildSiteModule();
        buildEventModule();
        install(new StateModule());
        buildConsoleModule();
        install(new ControllerModule());
        bindRecommendModule();
        buildRelevancyModule();
        buildSkipperSearchModule();
        buildSkipperAutoSuggestModule();
        buildAutosuggestModule();
        bindDIMModule();
        bindControllerModule();
        buildSearchModule();
        buildVariantsModule();
        buildShopifyModule();
        buildAnalyserMigrationModule();
        bindReportModule();
        bindGCPModule();
    }

    private void bindS3Module() {
        install(new S3Module());
    }

    private void bindDictionaryModule() {
        install(new DictionaryModule());
    }

    protected void bindDIMModule() {
        install(new DIMModule());
    }

    protected void buildAnalyserMigrationModule() {
        install(new AnalyserMigrationModule());
    }
    protected void buildShopifyModule() {
        install(new PluginModule());
    }

    protected void buildSiteModule() {
        install(new SiteModule());
    }

    protected void buildFeedModule() {
        install(new SearchFeedModule());
        install(new FeedModule());
    }

    protected void buildPIMModule() {
        install(new PIMModule());
    }

    protected void buildAuthModule() {
        install(new AuthModule());
    }

    protected void buildEventModule() { install(new EventModule()); }

    protected void buildMongoModule() {
        install(new MongoModule());
    }

    protected void buildConsoleModule() { install(new ConsoleModule()); }

    protected void buildRelevancyModule() { install(new RelevancyModule()); }

    protected void buildVariantsModule(){ install(new VariantsModule()); }

    protected void bindControllerModule() {
        Multibinder<Controller> controllers = Multibinder.newSetBinder(binder(), Controller.class);
        controllers.addBinding().to(SiteConfigController.class).asEagerSingleton();
        controllers.addBinding().to(UserController.class).asEagerSingleton();
        controllers.addBinding().to(MonitorController.class).asEagerSingleton();
        controllers.addBinding().to(FeedController.class).asEagerSingleton();
        controllers.addBinding().to(FieldController.class).asEagerSingleton();
        controllers.addBinding().to(AnalyserController.class).asEagerSingleton();
        controllers.addBinding().to(AutosuggestController.class).asEagerSingleton();
        controllers.addBinding().to(VariantsConfigController.class).asEagerSingleton();
    }

    protected void buildGimliModule() {
        install(new FieldModule());
    }

    protected void buildAsterixModule() { install(new AnalyserModule());}

    protected void buildAutosuggestModule() {install(new com.unbxd.autosuggest.AutosuggestModule());}

    protected void buildSkipperAutoSuggestModule() { install(new AutosuggestModule()); }

    protected void buildSkipperSearchModule() { install(new com.unbxd.skipper.search.SearchModule());}

    protected void bindConfigModule() { install(new ConfigModule()); }

    protected void bindReportModule() { install(new ReportModule()); }

    protected void buildSearchModule() { install((new SearchModule()));}

    protected void bindGCPModule() { install(new GCPModule()); }

    protected void bindRecommendModule() { install(new RecommendModule()); }
}

