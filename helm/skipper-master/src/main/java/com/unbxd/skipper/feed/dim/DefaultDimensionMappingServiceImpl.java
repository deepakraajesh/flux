package com.unbxd.skipper.feed.dim;

import com.google.inject.Inject;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.FieldMapping;
import com.unbxd.field.service.FieldService;
import com.unbxd.skipper.feed.dim.dao.DimensionMapSuggestion;
import com.unbxd.skipper.feed.dim.model.DimensionMap;

import java.util.Map;

public class DefaultDimensionMappingServiceImpl implements DimensionMappingService {
    private DimensionMapSuggestion suggestionDAO;
    private FieldService fieldService;

    @Inject
    public DefaultDimensionMappingServiceImpl(DimensionMapSuggestion suggestionDAO, FieldService fieldService) {
        this.suggestionDAO = suggestionDAO;
        this.fieldService = fieldService;
    }

    @Override
    public DimensionMap get(String siteKey, String vertical) throws DimException {
        try {
            DimensionMap mapConfig = suggestionDAO.get(vertical);
            if(mapConfig == null)
                throw new DimException("No dimension mapping config exists for vertical : " + vertical, 400);
            FieldMapping mapping = fieldService.getFieldMapping(siteKey);
            if(mapping != null)
                mapConfig.addMapping(mapping.getProperties());
            return mapConfig;
        } catch (FieldException e) {
            throw new DimException("Error while fetching dimension, " + e.getMessage(), e.getCode());
        }
    }

    @Override
    public void save(DimensionMap data) throws DimException {
        suggestionDAO.save(data);
    }

    @Override
    public void save(String siteKey, DimensionMap data) throws DimException {
        Map<String, String> mapping = data.getMapping();
        try {
            fieldService.saveDimensionMap(siteKey, mapping);
        } catch (FieldException e) {
            throw new DimException("Error while save dimension, " + e.getMessage(), e.getCode());
        }
    }
}

