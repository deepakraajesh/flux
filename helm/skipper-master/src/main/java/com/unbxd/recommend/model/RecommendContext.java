package com.unbxd.recommend.model;

import lombok.Data;

@Data
public class RecommendContext {
    private int page;
    private int count;
    private String sitekey;
    private String language;
    private Operation operation;
    public static RecommendContext getInstance(int page,
                                               int count,
                                               String sitekey,
                                               String language,
                                               Operation operation) {
        RecommendContext recommendContext = new RecommendContext();
        recommendContext.setOperation(operation);
        recommendContext.setLanguage(language);
        recommendContext.setSitekey(sitekey);
        recommendContext.setCount(count);
        recommendContext.setPage(page);
        return recommendContext;
    }
}
