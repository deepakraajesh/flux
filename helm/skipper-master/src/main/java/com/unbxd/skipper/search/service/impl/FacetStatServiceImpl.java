package com.unbxd.skipper.search.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.field.service.FieldService;
import com.unbxd.field.exception.FieldException;
import com.unbxd.search.SearchRemoteService;
import com.unbxd.search.model.*;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.search.exception.FacetStatServiceException;
import com.unbxd.skipper.search.model.*;
import com.unbxd.skipper.search.service.FacetStatService;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.gson.JsonParser.parseString;
import static com.unbxd.skipper.search.constants.Constants.STATS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
public class FacetStatServiceImpl implements FacetStatService {
    private FieldService fieldService;
    private SearchRemoteService searchRemoteService;
    private SiteService siteService;
    private static final String ERROR = "error";
    private static final String MESSAGE = "msg";
    private static final String PATH = "path";
    private static final String TEXT = "text";
    private static final String RANGE = "range";
    private static final String FILTER_FIELD_SUFFIX = "_uFilter";
    private static final Integer DEFAULT_NO_OF_PRODUCTS_TO_FETCH = 100;
    private static final String FACET_SORT_ORDER = "product_count";
    private static final String VARIANTS = "variants";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String FIELDS = "fields";
    private static final String ROWS = "rows";
    private static final String PRODUCTS = "products";
    private static final String RESPONSE = "response";

    @Inject
    public FacetStatServiceImpl(FieldService fieldService,
                                SearchRemoteService searchRemoteService,
                                SiteService siteService) {
        this.fieldService = fieldService;
        this.searchRemoteService = searchRemoteService;
        this.siteService = siteService;
    }

    @Override
    public TextFacetDetail fetchTextFacetDetails(String siteKey,
                                                 String facetName,
                                                 Integer count)
            throws FacetStatServiceException {
        try {
            String apiKey = fieldService.getSiteDetails(siteKey).getApiKey();
            Map<String,String> facetQueries = buildTextFacetQueries(facetName,true, count);
            FacetDetailResponse facetDetailsResponse = fetchFacetDetails(siteKey,apiKey,facetQueries,TEXT);
            TextFacetDetail result =  getTextFacetDetail(siteKey, facetName, facetDetailsResponse);
            // when result is null check if the field is undefined by passing field name without suffix '_uFilter',
            // if the field is not defined 400 error is thrown
            if(isNull(result)) {
                facetQueries = buildTextFacetQueries(facetName,false);
                facetDetailsResponse = fetchFacetDetails(siteKey,apiKey,facetQueries,TEXT);
                result = getTextFacetDetail(siteKey,facetName,facetDetailsResponse);
            }
            return isNull(result)? new TextFacetDetail(): result;
        } catch(FieldException e) {
            log.error("Unable to fetch TextFacet Details  for siteKey: "+ siteKey + " due to "+e.getMessage());
            throw new FacetStatServiceException(500,"Unable to fetch Text Facet Details");
        }
    }

    @Override
   public RangeFacetDetail fetchRangeFacetDetails(String siteKey,
                                                  String facetName,
                                                  Integer start,
                                                  Integer end,
                                                  Integer gap)
            throws FacetStatServiceException {
        try{
            String apiKey = fieldService.getSiteDetails(siteKey).getApiKey();
            Map<String,String> facetQueries = buildRangeFacetQueries(facetName,start,end,gap);
            FacetDetailResponse facetDetailsResponse  = fetchFacetDetails(siteKey,apiKey,facetQueries,RANGE);
            return getRangeFacetDetail(siteKey, facetName, facetDetailsResponse);
        } catch (FieldException e){
            log.error("Unable to fetch Range Facet Details  for siteKey: "+ siteKey +" due to "+e.getMessage());
            throw new FacetStatServiceException(500,"Unable to fetch Range Facet Details");
        }
    }

    @Override
    public PathFacetDetail fetchPathFacetDetails(String siteKey,
                                                 String facetName,
                                                 Integer count)
            throws FacetStatServiceException {
        try {
            String apiKey = fieldService.getSiteDetails(siteKey).getApiKey();
            Map<String, String> facetQueries = buildTextFacetQueries(facetName,false, count);
            FacetDetailResponse facetDetailResponse = fetchFacetDetails(siteKey,apiKey,facetQueries,PATH);
            return getPathFacetDetail(siteKey, apiKey, facetName, facetDetailResponse);
        }
        catch(FieldException e) {
            log.error("Unable to fetch Path Facet Details  for siteKey: "+ siteKey + " due to "+e.getMessage());
            throw new FacetStatServiceException(500,"Unable to fetch Path  Facet Details");
        }
    }

    @Override
    public SampleValues fetchFieldValues(String siteKey,
                                         String fieldName,
                                         Integer count,
                                         Boolean statsRequired) throws FacetStatServiceException {
        String errorMsg = "Unable to fetch field values";
        try {
            String apiKey = fieldService.getSiteDetails(siteKey).getApiKey();
            // method logic
            // fetch 100 products from search api
            // iterate over products
            // aggregate top n most occurred sample values of the given field
            // return aggregated values
            Boolean variantsEnabled = siteService.getSiteStatus(siteKey).getVariantsEnabled();
            Map<String, String> filterQuery = buildFilterQuery(fieldName,
                    DEFAULT_NO_OF_PRODUCTS_TO_FETCH, variantsEnabled, statsRequired);
            JsonObject productsResponse = fetchProducts(siteKey, apiKey, filterQuery);
            JsonArray products = productsResponse.getAsJsonObject(RESPONSE).getAsJsonArray(PRODUCTS);
            SampleValues result = new SampleValues();
            result.setValues(getFieldValues(fieldName, products, count));
            if (statsRequired) {
                FacetIntelligence facetIntelligence = new FacetIntelligence();
                JsonObject stats = productsResponse.getAsJsonObject(STATS).getAsJsonObject(fieldName);
                facetIntelligence.setMaxValue(stats.get(MAX).getAsFloat());
                facetIntelligence.setMinValue(stats.get(MIN).getAsFloat());
                result.setFacetIntelligence(facetIntelligence);
            }
            return result;
        } catch (SiteNotFoundException|FieldException e) {
            log.error(errorMsg + " for siteKey:" + siteKey + " due to :" + e.getMessage());
            throw new FacetStatServiceException(500, errorMsg);
        }
    }

    private List<String> getFieldValues(String fieldName,
                                        JsonArray productsArray,
                                        Integer count){
        List<String> values = new ArrayList<>(productsArray.size());
        HashMap<String , Integer> frequencyTable = new HashMap<>();
        getFieldValues(fieldName, productsArray, values, frequencyTable);
        values.sort((entry1, entry2) -> frequencyTable.get(entry2) - frequencyTable.get(entry1));
        return values.stream().limit(count).collect(Collectors.toList());
    }


    private void getFieldValues(String fieldName ,
                                JsonArray productsArray,
                                List<String> values,
                                HashMap<String , Integer> frequencyTable) {
        for(JsonElement product : productsArray) {
            JsonObject productJson =  product.getAsJsonObject();
            if(productJson.has(VARIANTS))
                getFieldValues(fieldName, productJson.getAsJsonArray(VARIANTS), values, frequencyTable);
            if (!productJson.has(fieldName)) continue;
            JsonElement fieldValue = productJson.get(fieldName);
            if(fieldValue.isJsonArray()) { // handle multivalued field
                for(JsonElement value : productJson.get(fieldName).getAsJsonArray())
                    appendFieldValue(value.getAsString(), values, frequencyTable);
            } else {
                appendFieldValue(fieldValue.getAsString(), values, frequencyTable);
            }
        }
    }

    private void appendFieldValue(String fieldValue ,
                                  List<String> values,
                                  HashMap<String , Integer> frequencyTable) {
        Integer frequency = frequencyTable.getOrDefault(fieldValue,0);
        frequencyTable.put(fieldValue, ++frequency);
        if(frequency == 1) values.add(fieldValue);
    }

    private TextFacetDetail getTextFacetDetail(String siteKey,
                                               String facetName,
                                               FacetDetailResponse facetDetailResponse) {
        TextFacetDetail result = null;
        TextFacet textFacet  = getTextFacet(facetDetailResponse,facetName, true);
        if (nonNull(textFacet)) {
            result = new TextFacetDetail();

            FacetIntelligence facetIntelligence = getFacetIntelligence(facetDetailResponse);
            facetIntelligence.setNoOfUniqueValues(textFacet.getCount());
            result.setFacetIntelligence(facetIntelligence);

            List<Object> values = textFacet.getValues();
            List<TextFacetValue> transformedValues = transformToTextFacetValues(values);
            result.setTopFiveValues(transformedValues);
            result.setValues(transformedValues);

            FacetConfig facetConfig = new FacetConfig();
            facetConfig.setFacetLength(transformedValues.size());
            facetConfig.setSortOrder(FACET_SORT_ORDER);
            result.setFacetConfig(facetConfig);

        } else
            log.info( "no details found from search api for the given facetName: " + facetName + ", siteKey:" + siteKey);
        return result;
    }

    private List<TextFacetValue> transformToTextFacetValues(List<Object> values) {
        List<TextFacetValue> transformedValues = new ArrayList<>(5);
        for(int i = 0;i < values.size()-1 ; i+=2) {
            TextFacetValue textFacetValue = new TextFacetValue();
            textFacetValue.setName((String)values.get(i));
            textFacetValue.setCount((Integer)values.get(i+1));
            transformedValues.add(textFacetValue);
        }
        return transformedValues;
    }

    private Map<String,String> buildTextFacetQueries(String facetName , boolean addSuffix) {
        return buildTextFacetQueries(facetName,addSuffix,5);
    }

    private Map<String,String> buildTextFacetQueries(String facetName ,
                                                     boolean addSuffix,
                                                     int limit) {
        Map<String,String> facetQueries = new HashMap<>(4);
        if(addSuffix) facetName = facetName + FILTER_FIELD_SUFFIX;
        facetQueries.put("facet.field", facetName);
        facetQueries.put("f."+facetName+".facet.limit", Integer.toString(limit));
        facetQueries.put("f."+facetName+".facet.count","true");
        facetQueries.put("filter",facetName+":[* TO *]");
        return facetQueries;
    }

    private  Map<String,String> buildRangeFacetQueries(String facetName,
                                                       Integer start,
                                                       Integer end,
                                                       Integer gap) {
        Map<String,String> facetQueries = new HashMap<>(6);
        facetQueries.put("facet.range",facetName);
        facetQueries.put("f."+facetName+".facet.range.start",start.toString());
        facetQueries.put("f."+facetName+".facet.range.end",end.toString());
        facetQueries.put("f."+facetName+".facet.range.gap",gap.toString());
        facetQueries.put("filter",facetName+":[* TO *]");
        facetQueries.put("stats",facetName);
        return facetQueries;
    }

    private Map<String, String> buildFilterQuery(String fieldName,
                                                 Integer numOfProducts,
                                                 Boolean variantsEnabled,
                                                 Boolean statsRequired) {
        Map<String, String> filterQueries = new HashMap<>(4);
        filterQueries.put(ROWS, String.valueOf(numOfProducts));
        filterQueries.put(FIELDS, fieldName);
        filterQueries.put(VARIANTS, String.valueOf(variantsEnabled));
        if (statsRequired) filterQueries.put(STATS, fieldName);
        return filterQueries;
    }

    private RangeFacetDetail getRangeFacetDetail(String siteKey,
                                                 String facetName,
                                                 FacetDetailResponse facetDetailResponse) {
        RangeFacetDetail result = null;
        RangeFacet rangeFacet = getRangeFacet(facetDetailResponse, facetName);
        if (nonNull(rangeFacet)) {
            result = new RangeFacetDetail();
            FacetConfig facetConfig = new FacetConfig();
            RangeFacetValues rangeFacetValues = rangeFacet.getValues();
            facetConfig.setStart(rangeFacetValues.getStart());
            facetConfig.setEnd(rangeFacetValues.getEnd());
            facetConfig.setGap(rangeFacetValues.getGap());
            result.setFacetConfig(facetConfig);

            FacetIntelligence facetIntelligence = getFacetIntelligence(facetDetailResponse);
            result.setFacetIntelligence(facetIntelligence);

            List<Object> values = rangeFacetValues.getCounts();
            List<RangeFacetValue> transformedValues = transformToRangeFacetValues(values);
            result.setValues(transformedValues);
        } else
            log.info("no details found from search api for the given facetName: " + facetName +
                    ", siteKey:" + siteKey);
        return isNull(result) ? new RangeFacetDetail() : result;

    }

    private FacetIntelligence getFacetIntelligence(FacetDetailResponse facetDetailResponse) {
        FacetIntelligence facetIntelligence = new FacetIntelligence();
        facetIntelligence.setNoOfProducts(facetDetailResponse.getResponse().getNumberOfProducts());
        if (!isNull(facetDetailResponse.getStatsWrapper())) {
            Stats stats = facetDetailResponse.getStatsWrapper().getStats();
            facetIntelligence.setMinValue(stats.getMin());
            facetIntelligence.setMaxValue(stats.getMax());
        }
        return facetIntelligence;
    }

    private List<RangeFacetValue> transformToRangeFacetValues(List<Object> values) {
        List<RangeFacetValue> transformedValues = new ArrayList<>(values.size() / 2);
        for (int i = 0; i < values.size() - 1; i += 2) {
            RangeFacetValue facetValue = new RangeFacetValue();
            facetValue.setValue((String) values.get(i));
            facetValue.setCount((Integer) values.get(i + 1));
            transformedValues.add(facetValue);
        }
        return transformedValues;
    }

    private PathFacetDetail getPathFacetDetail(String siteKey,
                                               String apiKey,
                                               String facetName,
                                               FacetDetailResponse facetDetailResponse)
            throws FacetStatServiceException {
        PathFacetDetail result = null;
        TextFacet textFacet = getTextFacet(facetDetailResponse, facetName, false);
        if (nonNull(textFacet)) {
            result = new PathFacetDetail();
            Map<String, Integer> noOfNodesAtEachLevel = new HashMap<>();

            FacetIntelligence facetIntelligence = getFacetIntelligence(facetDetailResponse);
            facetIntelligence.setNoOfUniqueValues(textFacet.getCount());
            result.setFacetIntelligence(facetIntelligence);

            List<Object> values = textFacet.getValues();
            List<TextFacetValue> transformedValues = transformToTextFacetValues(values);
            result.setTopFiveValues(transformedValues);
            result.setValues(transformedValues);

            FacetConfig facetConfig = new FacetConfig();
            facetConfig.setFacetLength(transformedValues.size());
            facetConfig.setSortOrder(FACET_SORT_ORDER);
            result.setFacetConfig(facetConfig);

            noOfNodesAtEachLevel.put("level1", textFacet.getCount());
            countNoOfNodesAtEachLevel(siteKey, apiKey, facetName, noOfNodesAtEachLevel);
            result.setDepth(noOfNodesAtEachLevel.size());
            result.setNoOfNodesAtEachLevel(noOfNodesAtEachLevel);
        } else
            log.info("no details found from search api for the given facetName: " + facetName +
                    ", siteKey:" + siteKey);
        return isNull(result) ? new PathFacetDetail() : result;
    }

    private void countNoOfNodesAtEachLevel(String siteKey,
                                           String apiKey,
                                           String facetName,
                                           Map<String,Integer> noOfNodesAtEachLevel)
            throws FacetStatServiceException{

        final int maxDepth = 30;
        final String level = "level";
        for (int i = 2; i <= maxDepth; i++) {
            try {
                String newFacetName = facetName + i;
                Map<String, String> facetQueries = buildTextFacetQueries(newFacetName, true);
                Response<FacetDetailResponse> response = searchRemoteService.getFacetDetails(apiKey,
                        siteKey, facetQueries).execute();
                if (response.code() == 200) {
                    if (response.body().getResponse().getNumberOfProducts() == 0 ||
                            isNull(getTextFacet(response.body(), newFacetName, true)))
                        break;
                    int noOfCategories = getTextFacet(response.body(), newFacetName, true).getCount();
                    noOfNodesAtEachLevel.put(level + i, noOfCategories);
                } else {
                    log.error("Unable to fetch path facet Details for siteKey: " + siteKey + ", code: " +
                            response.code() + ",  errorMessage: " + response.errorBody().string());
                    throw new FacetStatServiceException(500, "Unable to fetch path Facet Details");
                }

            } catch (IOException e) {
                log.error("Unable to fetch path facet Details  for siteKey: " + siteKey + "due to " + e.getMessage());
                throw new FacetStatServiceException(500, "Unable to fetch path facet Details");
            }
        }
    }

    private TextFacet getTextFacet(FacetDetailResponse facetDetailResponse,
                                   String facetName, boolean addFilterSuffix) {
        if (nonNull(facetDetailResponse) && nonNull(facetDetailResponse.getFacets())) {
            if (addFilterSuffix) facetName = facetName + FILTER_FIELD_SUFFIX;
            for (TextFacet textFacet : facetDetailResponse.getFacets().getText().getList()) {
                if (textFacet.getFacetName().equals(facetName))
                    return textFacet;
            }
        }
        return null;
    }

    private RangeFacet getRangeFacet(FacetDetailResponse facetDetailResponse,
                                     String facetName) {
        if (nonNull(facetDetailResponse) && nonNull(facetDetailResponse.getFacets())) {
            for (RangeFacet rangeFacet : facetDetailResponse.getFacets().getRange().getList())
                if (rangeFacet.getFacetName().equals(facetName))
                    return rangeFacet;
        }
        return null;
    }


    private JsonObject fetchProducts(String siteKey, String apiKey,
                                     Map<String, String> filterQueries)
            throws FacetStatServiceException {
        try {
            Response<String> response = searchRemoteService.getProducts(siteKey, apiKey, filterQueries).execute();
            int statusCode = response.code();
            if (statusCode == 200) {
                if (isNull(response.body())) {
                    log.error("empty response while fetching products from search api for siteKey: " + siteKey);
                    throw new FacetStatServiceException(ErrorCode.EmptyResponseFromDownStream.getCode(),
                            ErrorCode.EmptyResponseFromDownStream.getMessage());
                }
                return parseString(response.body()).getAsJsonObject();
            }
            if (statusCode == 400) {
                JsonObject errorResponse = parseString(response.errorBody().string()).getAsJsonObject();
                JsonElement message = errorResponse.get(ERROR).getAsJsonObject().get(MESSAGE);
                throw new FacetStatServiceException(400, message.getAsString());
            } else {
                log.error("Unable to fetch products from search api for siteKey: " + siteKey + ", code: "
                        + statusCode + ",  errorMessage: " + response.errorBody().string());
                throw new FacetStatServiceException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getMessage());
            }
        } catch (IOException e) {
            log.error("Unable to fetch products from search api for siteKey: " + siteKey + " due to "
                    + e.getMessage());
            throw new FacetStatServiceException(500, ErrorCode.IOError.getMessage());
        }
    }

    private FacetDetailResponse fetchFacetDetails(String siteKey,
                                                  String apiKey,
                                                  Map<String, String> facetQueries,
                                                  String facetType)
            throws FacetStatServiceException {
        try {
            Response<FacetDetailResponse> facetDetailsResponse = searchRemoteService.getFacetDetails(apiKey,
                    siteKey, facetQueries).execute();
            int statusCode = facetDetailsResponse.code();
            if (statusCode == 200)
                return facetDetailsResponse.body();
            else if (statusCode == 400) {
                JsonObject errorResponse = parseString(facetDetailsResponse.errorBody().string()).getAsJsonObject();
                JsonElement message = errorResponse.get(ERROR).getAsJsonObject().get(MESSAGE);
                throw new FacetStatServiceException(400, message.getAsString());
            } else {
                log.error("Unable to fetch " + facetType + " facet Details  for siteKey: " + siteKey + ", code: "
                        + statusCode + ",  errorMessage: " + facetDetailsResponse.errorBody().string());
                throw new FacetStatServiceException(500, "Unable to fetch " + facetType + " Facet Details");
            }
        } catch (IOException e) {
            log.error("Unable to fetch " + facetType + " facet Details  for siteKey: " + siteKey + " due to "
                    + e.getMessage());
            throw new FacetStatServiceException(500, "Unable to fetch Range Facet Details");
        }
    }

}
