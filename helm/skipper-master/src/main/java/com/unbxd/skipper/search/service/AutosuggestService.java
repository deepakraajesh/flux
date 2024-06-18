package com.unbxd.skipper.search.service;

import com.unbxd.skipper.search.exception.AutosuggestServiceException;

public interface AutosuggestService {
    int noOfPopularProductsFound(String siteKey, String filterParam) throws AutosuggestServiceException;
}
