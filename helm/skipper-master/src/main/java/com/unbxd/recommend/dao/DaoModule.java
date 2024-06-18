package com.unbxd.recommend.dao;

import com.google.inject.AbstractModule;

public class DaoModule extends AbstractModule {
    @Override
    public void configure() {
        bind(ContentDao.class).to(QueryContentDao.class).asEagerSingleton();
    }
}
