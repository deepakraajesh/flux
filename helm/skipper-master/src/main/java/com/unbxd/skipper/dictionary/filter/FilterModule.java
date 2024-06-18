package com.unbxd.skipper.dictionary.filter;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.unbxd.skipper.dictionary.filter.provider.FilterProvider;

import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.dictionary.filter.FilterConstants.*;


public class FilterModule extends AbstractModule {

    @Override
    public void configure() {
        bindFilters();
        bindDictionaryFilters();
    }

    public void bindFilters() {
        MapBinder<String, AssetFilter> filters =  MapBinder
                .newMapBinder(binder(), String.class, AssetFilter.class);
        filters.addBinding(SYNONYMS_STOPWORDS_FILTER).to(SynonymsStopwordsFilter.class).asEagerSingleton();
        filters.addBinding(STEM_DICT_STOPWORDS_FILTER).to(StemDictStopwordsFilter.class).asEagerSingleton();
        filters.addBinding(DEFAULT_STOPWORDS_FILTER).to(DefaultStopwordsFilter.class).asEagerSingleton();
    }

    public void bindDictionaryFilters() {
        /* Cannot remove explicit type parameters below due to a bug in jdk 11 */
        MapBinder<String, Map<String, List<AssetFilter>>> dictionaryFiltersBinder = MapBinder
                .newMapBinder(binder(), new TypeLiteral<String>(){}, new TypeLiteral<Map<String,
                        List<AssetFilter>>>(){});
        dictionaryFiltersBinder.addBinding(FILTERS).toProvider(FilterProvider.class)
                .asEagerSingleton();
    }
}
