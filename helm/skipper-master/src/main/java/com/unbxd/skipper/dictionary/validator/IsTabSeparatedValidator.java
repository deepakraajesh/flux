package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;

import java.util.List;

/**
 * This validator validates if entries are separated by <code>\t</code>
 * On splitting by <code>\t</code>, only two elements should be produced
 */
public class IsTabSeparatedValidator implements AssetValidator {

    @Override
    public boolean validate(String content) throws AssetException {
        if (content == null) {
            throw new AssetException("asset entry can't be null");
        } else if (content.startsWith("#")) {
            // ignore comments
            return true;
        }

        content = content.trim();
        if (content.isEmpty()) {
            // ignore empty lines for validation
            return true;
        } else {
            String[] mapping = content.split("\t", 2);
            if (mapping.length != 2) throw new AssetException(
                    "Invalid asset entry in the content, {entry:"+ content +"}"
            );
        }
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
