package com.unbxd.skipper.dictionary;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.skipper.dictionary.dao.DictionaryDAO;
import com.unbxd.skipper.dictionary.dao.MongoDictionaryDao;
import com.unbxd.skipper.dictionary.exceptionHandler.ExceptionModule;
import com.unbxd.skipper.dictionary.filter.FilterModule;
import com.unbxd.skipper.dictionary.knowledgeGraph.KnowledgeGraphModule;
import com.unbxd.skipper.dictionary.service.DictionaryService;
import com.unbxd.skipper.dictionary.service.DictionaryServiceImpl;
import com.unbxd.skipper.dictionary.transformer.TransformerModule;
import com.unbxd.skipper.dictionary.validator.ValidatorModule;
import ro.pippo.controller.Controller;

public class DictionaryModule extends AbstractModule {
    @Override
    public void configure() {
        install(new FilterModule());
        install(new ValidatorModule());
        install(new TransformerModule());
        install(new KnowledgeGraphModule());
        install(new ExceptionModule());

        bindDAO();
        bindService();
        bindControllers();
    }

    private void bindDAO() {
        bind(DictionaryDAO.class).to(MongoDictionaryDao.class).asEagerSingleton();
    }

    private void bindService() {
        bind(DictionaryService.class).to(DictionaryServiceImpl.class).asEagerSingleton();
    }


    protected void bindControllers() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(DictionaryController.class).asEagerSingleton();
    }

}
