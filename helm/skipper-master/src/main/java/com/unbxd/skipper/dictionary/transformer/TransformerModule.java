package com.unbxd.skipper.dictionary.transformer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import static com.unbxd.skipper.dictionary.transformer.AssetTransformer.*;

public class TransformerModule extends AbstractModule {

    @Override
    public void configure() {
        bindTransformers();
    }

    public void bindTransformers() {
        MapBinder<String, AssetTransformer> transformerBinder = MapBinder
                .newMapBinder(binder(), String.class, AssetTransformer.class);
        transformerBinder.addBinding(STOPWORDS).to(BasicTransformer.class).asEagerSingleton();
        transformerBinder.addBinding(MANDATORY).to(BasicTransformer.class).asEagerSingleton();
        transformerBinder.addBinding(STEMDICT).to(StemdictTransformer.class).asEagerSingleton();
        transformerBinder.addBinding(MULTIWORDS).to(PhrasesTransformer.class).asEagerSingleton();
        transformerBinder.addBinding(SYNONYMS).to(SynonymsV2Transformer.class).asEagerSingleton();
        transformerBinder.addBinding(ASCII_MAPPING).to(AsciiMappingTransformer.class).asEagerSingleton();
        transformerBinder.addBinding(EXCLUDE_TERMS).to(ExcludeTermsTransformer.class).asEagerSingleton();
    }
}
