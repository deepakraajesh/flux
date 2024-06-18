package com.unbxd.skipper.dictionary.validator.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.unbxd.skipper.dictionary.validator.AssetValidator;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.util.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryValidationProvider implements
        Provider<Map<String, List<AssetValidator>>> {

    public static final String VALIDATION_CONFIG_NAME = "validations.json";

    public static final Map<String, List<String>> VALIDATIONS_CONFIG = getValidationJson();

    @Inject
    private Map<String, AssetValidator> validatorMap;

    @Override
    public Map<String, List<AssetValidator>> get() {
        Map<String, List<AssetValidator>> dictionaryValidations = new HashMap<>();

        for (Map.Entry<String, List<String>> entry: VALIDATIONS_CONFIG.entrySet()) {
            List<AssetValidator> assetValidators = dictionaryValidations
                    .computeIfAbsent(entry.getKey(), v -> new ArrayList<>());
            for (String validation: entry.getValue()) {
                AssetValidator assetValidator = validatorMap.get(validation);
                if (assetValidator == null) {
                    throw new IllegalArgumentException("Incorrect validation" +
                            " name provided in validations.json: " + validation);
                }
                assetValidators.add(assetValidator);
            }
        }
        return dictionaryValidations;
    }

    private static Map<String, List<String>> getValidationJson() {
        try (InputStream stream = DictionaryValidationProvider.class.getClassLoader()
                .getResourceAsStream(VALIDATION_CONFIG_NAME)) {

            byte[] bytes = IoUtils.getBytes(stream);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, new TypeReference<>() {});

        } catch (IOException e){
            throw new PippoRuntimeException("Error while loading config validations.json :"+ e.getMessage());
        }
    }
}
