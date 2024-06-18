package com.unbxd.skipper.dictionary.validator;

import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.exception.AssetException;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *
 * Checks if given name/names are allowed as asset names
 */
public class IsForbiddenAssetValidator implements AssetValidator {

    private List<String> forBiddenAssets;

    @Inject
    public IsForbiddenAssetValidator(Properties properties) {
        forBiddenAssets = Arrays.asList(
                properties.getProperty(
                        FORBIDDEN_ASSETS, ""
                ).split(",")
        );
    }


    @Override
    public boolean validate(String content) throws AssetException {
        if (forBiddenAssets.contains(content)) throw new AssetException(
                "Forbidden asset name. {asset:"+ content + "}"
        );
        return true;
    }

    @Override
    public boolean validate(List<String> content) throws AssetException {
        for (String entry : content) {
            boolean val = validate(entry);
            if (!val) return false;
        }
        return true;
    }
}
