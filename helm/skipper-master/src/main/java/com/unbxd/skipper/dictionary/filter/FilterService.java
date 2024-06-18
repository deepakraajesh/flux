package com.unbxd.skipper.dictionary.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;

import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.dictionary.filter.FilterConstants.FILTERS;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Singleton
public class FilterService {
    private Map<String, List<AssetFilter>> filterMap;

    @Inject
    public FilterService(Map<String, Map<String, List<AssetFilter>>> filterMap) {
        this.filterMap = filterMap.get(FILTERS);
    }

    public void filter(DictionaryContext dictionaryContext) throws AssetException {
        List<AssetFilter> AssetFilters = filterMap.get(dictionaryContext.getDictionaryName());
        if (isEmpty(AssetFilters))
            return;
        for (AssetFilter AssetFilter: AssetFilters) {
            AssetFilter.filter(dictionaryContext);
        }
    }
}
