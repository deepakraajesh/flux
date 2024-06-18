package com.unbxd.skipper.dictionary.validator;

import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.exception.AssetException;

import java.util.List;
import java.util.Map;

/**
 * Wrapper for map containing {@link AssetValidatorManager}
 */
public class AssetValidatorHelper {

    private Map<String, AssetValidatorManager> assetValidatorManagers;

    @Inject
    public AssetValidatorHelper(
            Map<String, AssetValidatorManager> assetValidatorManagers
    ) {
        this.assetValidatorManagers = assetValidatorManagers;
    }

    public void validate(String assetname, List<String> content) throws AssetException {
        AssetValidatorManager assetValidatorManager = assetValidatorManagers.get(assetname);
        // TODO: should we have default validator??
        if (assetValidatorManager == null)
            return;
        boolean isValid =  assetValidatorManager.validate(content);
        if (!isValid) throw new AssetException(
                "Validation failed for asset. {asset:"+ assetname + "}"
        );
    }

    public void validate(String assetname, String content) throws AssetException {
        AssetValidatorManager assetValidatorManager = assetValidatorManagers.get(assetname);
        // TODO: should we have default validator??
        if (assetValidatorManager == null)
            return;
        boolean isValid = assetValidatorManager.validate(content);
        if (!isValid) throw new AssetException(
                "Validation failed for asset. {asset:"+ content + "}"
        );
    }
}
