package com.unbxd.skipper.feed.dim;

import com.unbxd.skipper.feed.dim.model.DimensionMap;

public interface DimensionMappingService {
    DimensionMap get(String siteKey, String vertical) throws DimException;
    void save(DimensionMap data) throws DimException;
    void save(String siteKey, DimensionMap data) throws DimException;
}

