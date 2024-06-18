package com.unbxd.skipper.search.service;

import com.unbxd.skipper.search.exception.FacetStatServiceException;
import com.unbxd.skipper.search.model.PathFacetDetail;
import com.unbxd.skipper.search.model.RangeFacetDetail;
import com.unbxd.skipper.search.model.SampleValues;
import com.unbxd.skipper.search.model.TextFacetDetail;

public interface FacetStatService {

    TextFacetDetail fetchTextFacetDetails(String siteKey,
                                          String facetName, Integer count)
            throws FacetStatServiceException;

    RangeFacetDetail fetchRangeFacetDetails(String siteKey,
                                            String facetName,
                                            Integer start,
                                            Integer end,
                                            Integer gap)
            throws FacetStatServiceException;

    PathFacetDetail fetchPathFacetDetails(String siteKey,
                                          String facetName,
                                          Integer count)
            throws FacetStatServiceException;

    SampleValues fetchFieldValues(String siteKey,
                                  String fieldName,
                                  Integer count,
                                  Boolean statsRequired)
           throws FacetStatServiceException;

}
