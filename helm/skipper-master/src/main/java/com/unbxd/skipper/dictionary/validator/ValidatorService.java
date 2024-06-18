package com.unbxd.skipper.dictionary.validator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;

import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.dictionary.validator.AssetValidator.VALIDATIONS;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Singleton
public class ValidatorService {

    private Map<String, List<AssetValidator>> validatorMap;

    @Inject
    public ValidatorService(Map<String, Map<String, List<AssetValidator>>> validatorMap) {
        this.validatorMap = validatorMap.get(VALIDATIONS);
    }

    public void validate(DictionaryContext dictionaryContext) throws AssetException {
        List<AssetValidator> assetValidators = validatorMap.get(dictionaryContext.getDictionaryName());

        if (isEmpty(assetValidators)) {
            throw new AssetException("Could not find validations for dictionary: "
                    + dictionaryContext.getDictionaryName());
        }

        for (AssetValidator assetValidator: assetValidators) {
            assetValidator.validateDictionary(dictionaryContext);
        }
    }

    public void validateSilently(DictionaryContext dictionaryContext) {
        List<AssetValidator> assetValidators = validatorMap.get(dictionaryContext.getDictionaryName());

        if (isNotEmpty(assetValidators)) {
            for (AssetValidator assetValidator: assetValidators) {
                assetValidator.validateSilently(dictionaryContext);
            }
        }
    }
}
