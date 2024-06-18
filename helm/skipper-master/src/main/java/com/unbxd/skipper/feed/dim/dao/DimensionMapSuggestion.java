package com.unbxd.skipper.feed.dim.dao;

import com.unbxd.skipper.feed.dim.model.DimensionMap;

public interface DimensionMapSuggestion {
    DimensionMap get(String vertical);
    void save(DimensionMap mapping);
}

