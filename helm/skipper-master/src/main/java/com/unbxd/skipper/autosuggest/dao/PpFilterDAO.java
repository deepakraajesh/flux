package com.unbxd.skipper.autosuggest.dao;

import com.unbxd.skipper.autosuggest.model.PopularProductsFilter;

public interface PpFilterDAO { // PP = popularProducts
    void save(PopularProductsFilter popularProductsFilter);
    PopularProductsFilter get(String siteKey);
}
