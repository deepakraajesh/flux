package com.unbxd.field.service;

import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.*;
import com.unbxd.skipper.relevancy.model.FieldAliasMapping;
import com.unbxd.skipper.relevancy.model.PageRequest;

import java.util.List;
import java.util.Map;

public interface FieldService {
    FieldMapping getFieldMapping(String siteKey) throws FieldException;
    void saveDimensionMap(String siteKey, Map<String, String> mapping) throws FieldException;
    SiteKeyCred getSiteDetails(String siteKey) throws FieldException;
    List<Fields> getFields(String siteKey, String fieldType) throws FieldException;
    SearchableFieldsResponse getSearchableFields(String siteKey, PageRequest request) throws FieldException;

    /**
     * @param siteKey
     * @param searchableFields
     * @return
     * @throws FieldException
     */
    void updateSearchableFields(String siteKey, List<FSSearchableField> searchableFields)
    throws FieldException;
    void deleteSite(String siteKey) throws FieldException;
    AttributesResponse getAttributes(String siteKey, PageRequest request) throws FieldException;
    void validateFieldNames(String siteKey,List<String> fieldNames) throws FieldException;
    FieldServiceBaseResponse updateMapping(String siteKey, List<FieldAliasMapping> request) throws FieldException;
}
