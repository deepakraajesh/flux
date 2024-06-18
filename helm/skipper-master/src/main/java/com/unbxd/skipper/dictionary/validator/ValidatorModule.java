package com.unbxd.skipper.dictionary.validator;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.unbxd.skipper.dictionary.validator.provider.DictionaryValidationProvider;

import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.dictionary.validator.AssetValidator.*;

public class ValidatorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AssetValidator.class)
                .annotatedWith(Validators.Assets.IsTabSeparated.class)
                .to(IsTabSeparatedValidator.class).asEagerSingleton();
        bind(AssetValidator.class)
                .annotatedWith(Validators.Assets.IsForBidden.class)
                .to(IsForbiddenAssetValidator.class).asEagerSingleton();
        bind(AssetValidator.class)
                .annotatedWith(Validators.Assets.MWConfig.class)
                .to(MWConfigValidator.class).asEagerSingleton();

        MapBinder<String, AssetValidatorManager> assetValidatorBinder = MapBinder.newMapBinder(
                binder(), String.class, AssetValidatorManager.class
        );
        assetValidatorBinder
                .addBinding(ASSETS)
                .toProvider(AssetValidatorManager.AssetNameValidatorProvider.class);
        assetValidatorBinder
                .addBinding("stemdict.txt")
                .toProvider(AssetValidatorManager.StemdictValidatorProvider.class);
        assetValidatorBinder
                .addBinding("multiwords.txt")
                .toProvider(AssetValidatorManager.MultiWordsValidatorProvider.class);

        bindValidators();
        bindDictionaryValidations();
    }

    public void bindValidators() {
        MapBinder<String, AssetValidator> validatorBinder = MapBinder
                .newMapBinder(binder(), String.class, AssetValidator.class);
        validatorBinder.addBinding(SYMBOLS).to(SymbolsValidator.class).asEagerSingleton();
        validatorBinder.addBinding(EMPTYNESS).to(EmptinessValidator.class).asEagerSingleton();
        validatorBinder.addBinding(DUPLICATE).to(DuplicateValidator.class).asEagerSingleton();
        validatorBinder.addBinding(MULTITERM).to(MultiTermValidator.class).asEagerSingleton();
        validatorBinder.addBinding(STOPWORDS).to(StopwordsValidator.class).asEagerSingleton();
        validatorBinder.addBinding(SINGLETERM).to(SingleTermValidator.class).asEagerSingleton();
        validatorBinder.addBinding(ALPHA_NUMERIC).to(AlphaNumericValidator.class).asEagerSingleton();
        validatorBinder.addBinding(REDUNDANT_SYNONYM).to(SynonymsRedundancyValidator.class).asEagerSingleton();
        validatorBinder.addBinding(STEMWORDS_EMPTINESS).to(StemwordsEmptinessValidator.class).asEagerSingleton();
        validatorBinder.addBinding(EXCLUDE_TERMS_EMPTINESS).to(ExcludeTermsEmptinessValidator.class).asEagerSingleton();
        validatorBinder.addBinding(BLACKLIST_VALIDATION).to(DefaultBlacklistValidator.class).asEagerSingleton();
        validatorBinder.addBinding(SYNONYMS_BLACKLIST_VALIDATION).to(SynonymsBlacklistValidator.class).asEagerSingleton();
        validatorBinder.addBinding(EXCLUDE_TERMS_BLACKLIST_VALIDATION).to(ExcludeTermBlacklistValidator.class).asEagerSingleton();
    }

    public void bindDictionaryValidations() {
        /* Cannot remove explicit type parameters below due to a bug in jdk 11 */
        MapBinder<String, Map<String, List<AssetValidator>>> dictionaryValidationsBinder = MapBinder
                .newMapBinder(binder(), new TypeLiteral<String>(){}, new TypeLiteral<Map<String,
                        List<AssetValidator>>>(){});
        dictionaryValidationsBinder.addBinding(VALIDATIONS).toProvider(DictionaryValidationProvider.class)
                .asEagerSingleton();
    }
}
