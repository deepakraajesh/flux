package com.unbxd.skipper.autosuggest.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.unbxd.autosuggest.AutosuggestService;
import com.unbxd.autosuggest.exception.AutosuggestException;
import com.unbxd.autosuggest.model.AutosuggestIndexingStatus;
import com.unbxd.autosuggest.model.KeywordSuggestion;
import com.unbxd.autosuggest.model.PopularProductField;
import com.unbxd.autosuggest.model.InField;
import com.unbxd.config.Config;
import com.unbxd.field.service.FieldService;
import com.unbxd.field.exception.FieldException;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.autosuggest.dao.PpFilterDAO;
import com.unbxd.skipper.autosuggest.exception.SuggestionServiceException;
import com.unbxd.skipper.autosuggest.model.*;
import com.unbxd.skipper.autosuggest.service.HagridRemoteService;
import com.unbxd.skipper.autosuggest.service.SuggestionService;
import com.unbxd.skipper.search.exception.AutosuggestServiceException;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

import static com.unbxd.skipper.autosuggest.model.DataType.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
public class SuggestionServiceImpl implements SuggestionService {
    private AutosuggestService autosuggestService;
    private FieldService fieldService;
    private HagridRemoteService hagridRemoteService;
    private PpFilterDAO ppFilterDAO;
    private Config config;
    private com.unbxd.skipper.search.service.AutosuggestService searchService;

    // these following two constants are used in createValueStringForStringType method
    private final static String NEGATIVE_CLAUSE = "negative_clause";
    private final static String POSITIVE_CLAUSE = "positive_clause";
    // internal field
    private final static String INTERNAL_FIELD_SUFFIX = "_unbxdInternal";
    private final static String DUMMY_FILTER_EXPRESSION = "uniqueId:[* TO *]";


    @Inject
    public SuggestionServiceImpl(AutosuggestService autosuggestService,
                                 FieldService fieldService,
                                 HagridRemoteService hagridRemoteService,
                                 PpFilterDAO ppFilterDAO,
                                 Config config,
                                 com.unbxd.skipper.search.service.AutosuggestService searchService){
        this.autosuggestService = autosuggestService;
        this.fieldService = fieldService;
        this.hagridRemoteService = hagridRemoteService;
        this.ppFilterDAO = ppFilterDAO;
        this.config = config;
        this.searchService = searchService;
    }

    @Override
    public Suggestions getSuggestions(String siteKey) throws SuggestionServiceException {
        Suggestions result = new Suggestions();
        try {
            result.setKeywordSuggestions(autosuggestService.getKeywordSuggestions(siteKey));
            result.setInFields(getInFields(siteKey));
            result.setPopularProducts(getPopularProducts(siteKey));
            TopQueriesConfig topQueriesConfig = new TopQueriesConfig();
            topQueriesConfig.setCount(getTopQueriesCount(siteKey));
            result.setTopQueries(topQueriesConfig);
            return result;
        } catch (AutosuggestException e) {
            log.error("Error from autosuggest service for siteKey:"+siteKey +" , errorMessage: "+e.getMessage());
            throw new SuggestionServiceException(e.getStatusCode() , e.getMessage());
        }
    }

    private List<String> getInFields(String siteKey) throws AutosuggestException {
        List<InField> inFields = autosuggestService.getInFields(siteKey);
        List<String> result = new ArrayList<>(inFields.size());
        inFields.forEach(
                inField -> result.add(inField.getFieldName())
        );
        return result;
    }

    private List<String> getPopularProductDisplayFields(String siteKey) throws AutosuggestException {
        List<PopularProductField> popularProductFields = autosuggestService.getPopularProducts(siteKey);
        List<String> result = new ArrayList<>(popularProductFields.size());
        for (PopularProductField field : popularProductFields)
            result.add(field.getFieldName());
        return result;
    }

    private PopularProducts getPopularProducts(String siteKey) throws  AutosuggestException {
        PopularProducts popularProducts = new PopularProducts();
        popularProducts.setDisplay(autosuggestService.getPopularProducts(siteKey));
        popularProducts.setSearchable(autosuggestService.getPPSearchableFields(siteKey));
        PopularProductsFilter popularProductsFilter = ppFilterDAO.get(siteKey);
        if(nonNull(popularProductsFilter))
            popularProducts.setFilterable(popularProductsFilter.getFilters());
        return popularProducts;
    }

    @Override
    public void addSuggestions(String siteKey,
                               Suggestions suggestions,
                               boolean ignoreDuplicateFieldError) throws SuggestionServiceException {
        List<KeywordSuggestion> keywordSuggestions = suggestions.getKeywordSuggestions();
        if(nonNull(keywordSuggestions) && !keywordSuggestions.isEmpty())
            addKeywordSuggestions(siteKey, keywordSuggestions, ignoreDuplicateFieldError);

        List<String> inFields = suggestions.getInFields();
        if(nonNull(inFields) && !inFields.isEmpty())
            addInFields(siteKey, inFields, ignoreDuplicateFieldError);

        if(nonNull(suggestions.getPopularProducts()))
            addPopularProducts(siteKey,suggestions.getPopularProducts(), ignoreDuplicateFieldError);

        if(nonNull(suggestions.getTopQueries()) && nonNull(suggestions.getTopQueries().getCount()))
            setTopQueriesCount(siteKey, suggestions.getTopQueries().getCount());
    }

    private void addKeywordSuggestions(String siteKey ,
                                       List<KeywordSuggestion> keywordSuggestions,
                                       boolean ignoreDuplicateFieldError) throws SuggestionServiceException {
        validateKeywordSuggestions(siteKey,keywordSuggestions);
        for (KeywordSuggestion keywordSuggestion : keywordSuggestions) {
            try {
                autosuggestService.setKeywordSuggestion(siteKey, keywordSuggestion , ignoreDuplicateFieldError);
            } catch (AutosuggestException e) {
                throw new SuggestionServiceException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
            }
        }
    }

    private void validateKeywordSuggestions(String siteKey,
                                            List<KeywordSuggestion> keywordSuggestions) throws SuggestionServiceException {
        List<String> fieldNames = new ArrayList<>();
        for(KeywordSuggestion keywordSuggestion : keywordSuggestions)
            fieldNames.addAll(keywordSuggestion.getFields());
        // fieldNames in keyword suggestions are not validated in gimli
        try {
            fieldService.validateFieldNames(siteKey,fieldNames);
        } catch (FieldException e) {
            throw new SuggestionServiceException(e.getCode(),e.getMessage());
        }
    }

    private void addInFields(String siteKey ,
                             List<String> fieldNames,
                             boolean ignoreDuplicateFieldError ) throws SuggestionServiceException{
        for(String fieldName : fieldNames){
            try {
                autosuggestService.setInfield(siteKey, fieldName, ignoreDuplicateFieldError);
            } catch (AutosuggestException e) {
                throw new SuggestionServiceException(e.getStatusCode(),e.getMessage());
            }
        }
    }

    private void addPopularProducts(String siteKey,
                                    PopularProducts popularProducts,
                                    boolean ignoreDuplicateFieldError) throws  SuggestionServiceException{
        try {
            if (nonNull(popularProducts.getSearchable()) && !popularProducts.getSearchable().isEmpty()) {
                for (String fieldName : popularProducts.getSearchable()) {
                    autosuggestService.setPPSearchableField(siteKey, fieldName, ignoreDuplicateFieldError);
                }
            }

            if (nonNull(popularProducts.getDisplay()) && !popularProducts.getDisplay().isEmpty()) {
                for (PopularProductField field : popularProducts.getDisplay()) {
                     PopularProductField popularProductField = new PopularProductField();
                     popularProductField.setRequired(field.isRequired());
                     popularProductField.setFieldName(field.getFieldName());
                     autosuggestService.setPopularProductField(siteKey, popularProductField, ignoreDuplicateFieldError);
                }
            }

            if (nonNull(popularProducts.getFilterable())) {
                setPopularProductsFilter(siteKey, popularProducts.getFilterable());
            }
        } catch (AutosuggestException e) {
            throw new SuggestionServiceException(e.getStatusCode(), e.getMessage());
        }
    }

    private void setPopularProductsFilter(String siteKey,
                                          List<List<Filter>> filterable) throws SuggestionServiceException {
        validateFilters(siteKey, filterable);
        String parsedFilters = parseFilters(filterable);
        if(parsedFilters != null) {
            if (noPopularProductsFound(siteKey, parsedFilters))
                throw new SuggestionServiceException(400,
                        ErrorCode.ZeroResultsFoundForPopularProductsFilter.getCode(),
                        "Zero results found for PopularProducts filter");
        }
        configureFilters(siteKey, parsedFilters);
        PopularProductsFilter popularProductsFilter = new PopularProductsFilter();
        popularProductsFilter.setSiteKey(siteKey);
        popularProductsFilter.setFilters(filterable);
        ppFilterDAO.save(popularProductsFilter);
    }

    private void validateFilters(String siteKey,
                                 List<List<Filter>> filterable) throws SuggestionServiceException {
        for(List<Filter> filtersGroup : filterable) {
            if(isNull(filtersGroup) || filtersGroup.isEmpty())
                throw new SuggestionServiceException(400, "invalid filter expression");
            for(Filter filter: filtersGroup) {
                if(isNull(filter.getFieldName())
                        || isNull(filter.getType())
                        || isNull(filter.getCondition())
                        || isNull(filter.getValues())
                        || filter.getValues().isEmpty())
                    throw new SuggestionServiceException(400,"any of the fields in filter cannot be empty");
                if(filter.getType().equals(STRING)){
                    if (filter.getCondition() == Condition.MORE_THAN
                            || filter.getCondition() == Condition.LESS_THAN
                            || filter.getCondition() == Condition.IN_BETWEEN) {
                        throw new SuggestionServiceException(400, "invalid filter expression");
                    }
                }
                else {
                    if (filter.getCondition() == Condition.CONTAINS
                            || filter.getCondition() == Condition.NOT_CONTAINS
                            || filter.getCondition() == Condition.NOT_EQUALS) {
                        throw new SuggestionServiceException(400, "invalid filter expression");
                    }
                    if(filter.getCondition().equals(Condition.IN_BETWEEN) && filter.getValues().size() != 2)
                        throw new SuggestionServiceException(400,"invalid filter expression");
                }
            }
        }

        // validate fieldNames
        List<String> fieldNames = new ArrayList<>();
        for(List<Filter> filtersGroup : filterable) {
            for (Filter filter : filtersGroup)
                fieldNames.add(filter.getFieldName());
        }
        try {
            fieldService.validateFieldNames(siteKey,fieldNames);
        } catch (FieldException e) {
            throw new SuggestionServiceException(e.getCode(),e.getMessage());
        }
    }

    @Override
    public void deleteSuggestions(String siteKey,
                                  Suggestions suggestions) throws SuggestionServiceException {
        List<KeywordSuggestion> keywordSuggestions = suggestions.getKeywordSuggestions();
        if (nonNull(keywordSuggestions) && !keywordSuggestions.isEmpty())
            deleteKeywordSuggestions(siteKey, keywordSuggestions);

        List<String> inFields = suggestions.getInFields();
        if (nonNull(inFields) && !inFields.isEmpty())
            deleteInFields(siteKey, inFields);

        if (nonNull(suggestions.getPopularProducts()))
            deletePopularProducts(siteKey, suggestions.getPopularProducts());
    }

    private void deleteKeywordSuggestions(String siteKey ,
                                          List<KeywordSuggestion> keywordSuggestions)
            throws SuggestionServiceException {
        for (KeywordSuggestion keywordSuggestion : keywordSuggestions) {
            try {
                autosuggestService.deleteKeywordSuggestion(siteKey, keywordSuggestion.getName());
            } catch (AutosuggestException e) {
                throw new SuggestionServiceException(e.getStatusCode(),e.getMessage());
            }
        }
    }

    private void deleteInFields(String siteKey,
                                List<String> fieldNames) throws SuggestionServiceException{
        for(String fieldName : fieldNames){
            try {
                autosuggestService.deleteInField(siteKey,fieldName);
            } catch (AutosuggestException e) {
                throw new SuggestionServiceException(e.getStatusCode(),e.getMessage());
            }
        }
    }

    private void deletePopularProducts(String siteKey,
                                       PopularProducts popularProducts) throws SuggestionServiceException {
        if(nonNull(popularProducts.getSearchable()) && !popularProducts.getSearchable().isEmpty()) {
            for (String fieldName : popularProducts.getSearchable()) {
                try {
                    autosuggestService.deletePPSearchableField(siteKey, fieldName);
                } catch (AutosuggestException e) {
                    throw new SuggestionServiceException(e.getStatusCode(), e.getMessage());
                }
            }
        }

        if(nonNull(popularProducts.getDisplay()) && !popularProducts.getDisplay().isEmpty()) {
            for (PopularProductField field : popularProducts.getDisplay()) {
                try {
                    autosuggestService.deletePopularProductField(siteKey,field.getFieldName());
                } catch (AutosuggestException e) {
                    throw new SuggestionServiceException(e.getStatusCode(),e.getMessage());
                }
            }
        }
    }

    private String parseFilters(List<List<Filter>> filters){
        if(filters.isEmpty())
        {
            // TODO support delete filter operation in search service (hagrid)
            return null;
        }
        // build filter queries
        Iterator<List<Filter>> i = filters.iterator();
        StringBuilder fq = new StringBuilder();
        while (i.hasNext()) {
            List<Filter> ANDfilterGroup = i.next();
            StringBuilder ANDexpression = new StringBuilder("(");

            for (Filter filterQuery : ANDfilterGroup) {
                String fieldName = filterQuery.getFieldName();
                DataType type = filterQuery.getType();
                List<String> values = filterQuery.getValues();
                if (type.equals(STRING)) {
                    if (values.size() > 1) { // if multiple values present
                        switch (filterQuery.getCondition()) {
                            case NOT_EQUALS:
                                ANDexpression.append("-").append(fieldName).append(INTERNAL_FIELD_SUFFIX).append(":");
                                ANDexpression.append(buildValueStringForStringType(NEGATIVE_CLAUSE, values));
                                break;
                            case CONTAINS:
                                ANDexpression.append(fieldName).append(":");
                                ANDexpression.append(buildValueStringForStringType(POSITIVE_CLAUSE, values));
                                break;
                            case NOT_CONTAINS:
                                ANDexpression.append("-").append(fieldName).append(":");
                                ANDexpression.append(buildValueStringForStringType(NEGATIVE_CLAUSE, values));
                                break;
                            default:
                                // equals condition
                                ANDexpression.append(fieldName).append(INTERNAL_FIELD_SUFFIX).append(":");
                                ANDexpression.append(buildValueStringForStringType(POSITIVE_CLAUSE, values));
                        }
                    } else {
                        String value = values.get(0).replaceAll("\"", "\\\\\"");
                        switch (filterQuery.getCondition()) {
                            case NOT_EQUALS:
                                ANDexpression.append("-").append(fieldName).append(INTERNAL_FIELD_SUFFIX).append(":");
                                break;
                            case CONTAINS:
                                ANDexpression.append(fieldName).append(":");
                                break;
                            case NOT_CONTAINS:
                                ANDexpression.append("-").append(fieldName).append(":");
                                break;
                            default:
                                // equals condition
                                ANDexpression.append(fieldName).append(INTERNAL_FIELD_SUFFIX).append(":");
                        }
                        ANDexpression.append("\"").append(value).append("\"");
                    }

                } else if (type.equals(NUMBER) || type.equals(DATE_TIME)) {
                    ANDexpression.append(fieldName).append(":");
                    switch (filterQuery.getCondition()) {
                        case MORE_THAN:
                            ANDexpression.append("{").append(values.get(0)).append(" TO ")
                                    .append("*").append("]");
                            break;
                        case LESS_THAN:
                            ANDexpression.append("[").append("*").append(" TO ").append(values.get(0))
                                    .append("}");
                            break;
                        case IN_BETWEEN:
                            ANDexpression.append("[").append(values.get(0)).append(" TO ")
                                    .append(values.get(1)).append("]");
                            break;
                        default: // equals condition
                            if (values.size() > 1) // if multiple values present
                                ANDexpression.append(buildValueStringForNumberType(values));
                            else
                                ANDexpression.append(values.get(0));
                            break;
                    }
                }
                ANDexpression.append(" AND ");
            }
                ANDexpression.append("*:*");
                ANDexpression.append(")");
                fq.append(ANDexpression);
                if (i.hasNext()) {
                    fq.append(" OR ");
                }
            }
            return fq.toString();
    }

    private StringBuilder buildValueStringForStringType(String clause, List<String> values) {

        String operator = null;

        switch (clause)
        {
            case NEGATIVE_CLAUSE:
                operator = " AND ";
                break;
            case POSITIVE_CLAUSE:
                operator = " OR ";
        }

        StringBuilder valueString = new StringBuilder();
        valueString.append("( ");
        for(String value :values) {
            valueString.append("\"")
                    .append(value.toLowerCase().trim().replaceAll("\"", "\\\\\""))
                    .append("\"")
                    .append(operator);
        }
        valueString.delete(valueString.length()-operator.length(), valueString.length()).append(" )");
        return valueString;
    }

    private StringBuilder buildValueStringForNumberType(List<String> values) {
        StringBuilder valueString = new StringBuilder().append("( ");
        valueString.append(String.join(" OR ",values)).append(" )");
        return valueString;
    }

    private void configureFilters(String siteKey,
                                  String parsedFilters) throws SuggestionServiceException {
        JsonObject request = createRequest(parsedFilters);
        try {
            Response<JsonObject> response = hagridRemoteService.setConfig(siteKey,
                    "application/json", request).execute();
            if(!response.isSuccessful()) {
                log.error("Unable to configure  popular products filter in Hagrid , statusCode: " + response.code()
                        + " , reason: "+response.errorBody().string() + " for siteKey: " + siteKey);
                throw new SuggestionServiceException(response.code(),"Unable to configure  popular products filters");
            }
        } catch (IOException e) {
            log.error("Error while configuring popular products filter in Hagrid  for siteKey: " + siteKey +
            " ,error message: "+ e.getMessage());
            throw new SuggestionServiceException(500,"Unable to configure  popular products filters");
        }
    }

    private boolean noPopularProductsFound(String siteKey, String ppFilter) throws SuggestionServiceException {
        try {
           return searchService.noOfPopularProductsFound(siteKey, ppFilter) == 0;
        } catch (AutosuggestServiceException e) {
            throw new SuggestionServiceException(e.getStatusCode(), e.getErroCode(), e.getMessage());
        }
    }

    private JsonObject createRequest(String parsedFilters) {
        if(parsedFilters == null || parsedFilters.strip().length() == 0) {
            parsedFilters = "uniqueId:[* TO *]";
        }
        JsonObject rootObject =  new JsonObject();
        JsonArray autosuggestConfig = new JsonArray();
        JsonObject pPFilterConfig = new JsonObject();
        pPFilterConfig.add("popularProducts.filter", new JsonPrimitive(parsedFilters));
        autosuggestConfig.add(pPFilterConfig);
        rootObject.add("autosuggest",autosuggestConfig);
        return rootObject;
    }

    private void setTopQueriesCount(String siteKey ,
                                    Integer topQueriesCount) throws SuggestionServiceException {
        try {
            autosuggestService.setTopQueriesCount(siteKey ,topQueriesCount);
        } catch (AutosuggestException e) {
            throw new SuggestionServiceException(e.getStatusCode(), e.getMessage());
        }
    }

    private Integer getTopQueriesCount(String siteKey) throws AutosuggestException {
        Integer topQueriesCount = autosuggestService.getTopQueriesCount(siteKey);
        if (isNull(topQueriesCount))
            return  Integer.parseInt(config.getProperty("top.queries.count","500"));
        else
            return topQueriesCount;
    }

    @Override
    public void indexSuggestions(String siteKey) throws SuggestionServiceException {
        try {
            autosuggestService.indexSuggestions(siteKey);
        } catch (AutosuggestException e) {
            throw new SuggestionServiceException(e.getStatusCode(), e.getMessage());
        }
    }

    @Override
    public AutosuggestIndexingStatus getIndexingStatus(String siteKey) throws SuggestionServiceException {
        try {
            return autosuggestService.getIndexingStatus(siteKey);
        } catch (AutosuggestException e) {
            throw new SuggestionServiceException(e.getStatusCode(), e.getMessage());
        }
    }

}
