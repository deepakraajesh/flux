package com.unbxd.skipper.dictionary.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.util.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface AssetFilter {

    Map<String, List<String>> FILTERS_CONFIG = getFiltersJson();
    String FILTER_CONFIG_NAME = "filters.json";

    static Map<String, List<String>> getFiltersJson() {
        try (InputStream stream = AssetFilter.class.getClassLoader()
                .getResourceAsStream(FILTER_CONFIG_NAME)) {

            byte[] bytes = IoUtils.getBytes(stream);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, new TypeReference<>() {});

        } catch (IOException e){
            throw new PippoRuntimeException("Error while loading config filter.json :"+ e.getMessage());
        }
    }

    void filter(DictionaryContext dictionaryContext);
}
