package com.unbxd.skipper.dictionary.filter.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.unbxd.skipper.dictionary.filter.AssetFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.dictionary.filter.AssetFilter.FILTERS_CONFIG;
import static com.unbxd.skipper.dictionary.filter.FilterConstants.*;

public class FilterProvider implements Provider<Map<String, List<AssetFilter>>> {

    private Map<String,Map<String,String>> filterNames = new HashMap<>(1) {{
        put(STOPWORDS,new HashMap<>(3) {{
            put(SYNONYMS,SYNONYMS_STOPWORDS_FILTER);
            put(STEM_DICT,STEM_DICT_STOPWORDS_FILTER);
            put(DEFAULT,DEFAULT_STOPWORDS_FILTER);
        }});
    }};

    @Inject
    private Map<String, AssetFilter> assetFilterMap;

    @Override
    public Map<String, List<AssetFilter>> get() {
        Map<String, List<AssetFilter>> dictionaryFilters = new HashMap<>();

        for (Map.Entry<String, List<String>> entry: FILTERS_CONFIG.entrySet()) {
            List<AssetFilter> AssetFilters = dictionaryFilters
                    .computeIfAbsent(entry.getKey(), v -> new ArrayList<>());
            for (String filter: entry.getValue()) {
                String defaultStopWordsFilter = filterNames.get(filter).get(DEFAULT);
                AssetFilter AssetFilter = assetFilterMap.get(
                        filterNames.get(filter).getOrDefault(entry.getKey(), defaultStopWordsFilter)
                );
                if (AssetFilter == null) {
                    throw new IllegalArgumentException("Incorrect filter" +
                            " name provided in filters.json: " + filter);
                }
                AssetFilters.add(AssetFilter);
            }
        }
        return dictionaryFilters;
    }
}
