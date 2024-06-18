package com.unbxd.skipper.dictionary.validator;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.unbxd.skipper.dictionary.exception.AssetException;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class AssetValidatorManager {

    private Set<AssetValidator> validators;

    public boolean validate(String content) throws AssetException {
        for (AssetValidator validator : validators) {
            boolean val = validator.validate(content);
            if (!val) return false;
        }
        return true;
    }

    public boolean validate(List<String> content) throws AssetException {
        for (AssetValidator validator : validators) {
            boolean val = validator.validate(content);
            if (!val) return false;
        }
        return true;
    }

    /**
     * Provider for stemdict.txt entries
     */
    public static class StemdictValidatorProvider implements Provider<AssetValidatorManager> {

        private Set<AssetValidator> validators;

        @Inject
        public StemdictValidatorProvider(
                @Validators.Assets.IsTabSeparated AssetValidator tabValidator
        ) {
            this.validators = Sets.newHashSet(tabValidator);
        }

        @Override
        public AssetValidatorManager get() {
            return new AssetValidatorManager(validators);
        }
    }

    public static class AssetNameValidatorProvider implements Provider<AssetValidatorManager> {

        private Set<AssetValidator> validators;

        @Inject
        public AssetNameValidatorProvider(@Validators.Assets.IsForBidden AssetValidator forBiddenAsset) {
            this.validators = Sets.newHashSet(forBiddenAsset);
        }

        @Override
        public AssetValidatorManager get() {
            return new AssetValidatorManager(validators);
        }
    }

    public static class MultiWordsValidatorProvider implements Provider<AssetValidatorManager> {

        private Set<AssetValidator> validators;

        @Inject
        public MultiWordsValidatorProvider(@Validators.Assets.MWConfig AssetValidator mwConfigValidator) {
            this.validators = Sets.newHashSet(mwConfigValidator);
        }

        @Override
        public AssetValidatorManager get() {
            return new AssetValidatorManager(validators);
        }
    }
}
