package com.unbxd.skipper;

import com.unbxd.analyser.AnalyserTestModule;
import com.unbxd.auth.AuthTestModule;
import com.unbxd.autosuggest.AutosuggestTestModule;
import com.unbxd.config.Config;
import com.unbxd.console.ConsoleTestModule;
import com.unbxd.field.GimliTestModule;
import com.unbxd.mongo.MongoTestModule;
import com.unbxd.pim.PIMTestModule;
import com.unbxd.report.ReportTestModule;
import com.unbxd.search.feed.FeedTestModule;
import com.unbxd.skipper.feed.FeedModule;
import com.unbxd.skipper.plugins.PluginTestModule;
import com.unbxd.skipper.relevancy.RelevancyTestModule;
import com.unbxd.skipper.search.SearchTestModule;
import com.unbxd.skipper.site.SiteModule;

public class SkipperTestModule extends SkipperModule {

    @Override
    protected void buildSiteModule() {
        System.setProperty("region", "us");
        System.setProperty(Config.AWS_DEFAULT_REGION,"us-west-1");
        install(new SiteModule());
    }

    @Override
    protected void buildMongoModule() {
        install(new MongoTestModule());
    }

    @Override
    protected void buildAuthModule() {
        install(new AuthTestModule());
    }

    @Override
    protected void buildPIMModule() {
        install(new PIMTestModule());
    }

    @Override
    protected void buildConsoleModule() {
        install(new ConsoleTestModule());
    }

    @Override
    protected void buildRelevancyModule() { install(new RelevancyTestModule()); }

    protected void buildFeedModule() {
        install(new FeedTestModule());
        install(new FeedModule());
    }

    @Override
    protected void buildGimliModule() {
        install(new GimliTestModule());
    }

    @Override
    protected void buildAsterixModule() {
        install(new AnalyserTestModule());}


    @Override
    protected void buildSkipperSearchModule() {
        install(new SearchTestModule());
    }

    @Override
    protected void buildAutosuggestModule() {install(new AutosuggestTestModule());}

    @Override
    protected void buildSkipperAutoSuggestModule() {
        install(new com.unbxd.skipper.autosuggest.AutosuggestTestModule());
    }

    @Override
    protected void bindReportModule() {
        install(new ReportTestModule());
    }

    @Override
    protected void buildShopifyModule() {
        install(new PluginTestModule());
    }

    @Override
    protected void buildSearchModule() { install((new com.unbxd.search.SearchTestModule()));}


}

