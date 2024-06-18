package com.unbxd.skipper.search.service.impl;

import com.google.inject.Inject;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.service.FieldService;
import com.unbxd.search.SearchRemoteService;
import com.unbxd.search.model.AutosuggestResponse;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.search.exception.AutosuggestServiceException;
import com.unbxd.skipper.search.service.AutosuggestService;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
public class AutosuggestServiceImpl implements AutosuggestService {

    private FieldService fieldService;
    private SearchRemoteService searchRemoteService;

    @Inject
    public AutosuggestServiceImpl( FieldService fieldService,
                                   SearchRemoteService searchRemoteService) {
        this.fieldService = fieldService;
        this.searchRemoteService = searchRemoteService;
    }

    @Override
    public int noOfPopularProductsFound(String siteKey, String filter)   throws AutosuggestServiceException {
        try {
            String apiKey = fieldService.getSiteDetails(siteKey).getApiKey();
            Response<AutosuggestResponse> response = null;
            if(filter == null) {
                response = searchRemoteService.getPopularProductSuggestion(apiKey, siteKey,
                                filter)
                        .execute();
            } else {
                response = searchRemoteService.getPopularProductSuggestion(apiKey, siteKey).execute();
            }
            if(!response.isSuccessful()){
                log.error("Error while fetching autosuggest for siteKey:"+ siteKey + ", code: "
                        + response.code() + ",  errorMessage: "+ response.errorBody().string());
                throw new AutosuggestServiceException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                        "Error while calling search service");
            }
            if(isNull(response.body())){
                log.error("Empty response from search service while fetching autosuggest for siteKey:"+ siteKey);
                throw new AutosuggestServiceException(500, ErrorCode.EmptyResponseFromDownStream.getCode(),
                        "Error while calling search service");
            }
            if(nonNull(response.body().getError())){
                log.error("Error while fetching autosuggest for siteKey:"+ siteKey + ", code: "
                        +  response.body().getError().getCode() + ",  errorMessage: "
                        + response.body().getError().getMsg());
                throw new AutosuggestServiceException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                        "Error while calling search service");
            }
            if(isNull(response.body().getResponse()) || isNull(response.body().getResponse().getNumberOfProducts())){
                log.error("Invalid response from search service while fetching autosuggest for siteKey:"+ siteKey
                + " response:" + response.body().toString());
                throw new AutosuggestServiceException(500, ErrorCode.InvalidResponseFromDownStream.getCode(),
                        "Error while calling search service");
            }
            return response.body().getResponse().getNumberOfProducts();
        } catch (FieldException e) {
            throw new AutosuggestServiceException(e.getCode(), e.getMessage());
        } catch (IOException e) {
            log.error("Error while fetching autosuggest for siteKey: "+ siteKey + "due to "+e.getMessage());
            throw new AutosuggestServiceException(500, ErrorCode.IOError.getCode(), e.getMessage());
        }
    }
}
