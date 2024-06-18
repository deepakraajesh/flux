package com.unbxd.skipper.dictionary.validator;

import com.unbxd.lucene.analysis.trie.MWConfig;
import com.unbxd.skipper.dictionary.exception.AssetException;

import java.util.List;

/**
 * Validate multiwords entries
 */
public class MWConfigValidator implements AssetValidator {

    @Override
    public boolean validate(String content) throws AssetException {
        if (content == null) {
            throw new AssetException("asset entry can't be null");
        } else if (content.startsWith("#")) {
            // ignore comments
            return true;
        }

        content = content.trim();
        if (content.isEmpty() || !content.contains("|")) {
            // ignore empty lines and lines not containing the separator
            return true;
        } else {
            int idx = content.lastIndexOf('|');
            String conf = content.substring(idx + 1);
            try {
                MWConfig.valueOf(conf);
            } catch (IllegalArgumentException e) {
                throw new AssetException("Invalid value for mw config '" + conf + "' in '" + content + "'", e);
            }
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
