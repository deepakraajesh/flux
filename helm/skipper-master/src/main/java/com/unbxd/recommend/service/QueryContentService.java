package com.unbxd.recommend.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.unbxd.recommend.dao.ContentDao;
import com.unbxd.recommend.exception.RecommendException;
import com.unbxd.recommend.model.NavigateResponse;
import com.unbxd.recommend.model.RecommendContext;
import com.unbxd.recommend.model.RectifyResponse;
import com.unbxd.skipper.dictionary.dao.DictionaryDAO;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryData;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.transformer.AssetTransformer;

import java.util.List;
import java.util.Map;

import static com.unbxd.recommend.model.NavigateResponse.getInstance;
import static com.unbxd.skipper.dictionary.model.DictionaryData.getInstance;

@Singleton
public class QueryContentService {

    private static final String SYNONYMS = "synonyms";

    private final ContentDao contentDao;
    private final DictionaryDAO dictionaryDao;
    private final Map<String, AssetTransformer> transformerMap;

    @Inject
    public QueryContentService(ContentDao contentDao,
                               DictionaryDAO dictionaryDao,
                               Map<String, AssetTransformer> transformerMap) {
        this.contentDao = contentDao;
        this.dictionaryDao = dictionaryDao;
        this.transformerMap = transformerMap;
    }

    public NavigateResponse getNavigateResponse(String sitekey)
            throws RecommendException {
        return getInstance(contentDao.getLanguages(sitekey));
    }

    public RectifyResponse getRectifiableQueries
            (RecommendContext recommendContext)
            throws RecommendException {
        int count = recommendContext.getCount();
        int page = recommendContext.getPage();

        return RectifyResponse.getInstance(page, count, contentDao
                .countQueryStats(recommendContext), contentDao
                .getQueryStats(recommendContext));
    }

    public DictionaryData getDictionaryData(int page,
                                            int count,
                                            String filter,
                                            String sitekey,
                                            String dictionaryName)
            throws RecommendException{
        try {
            List<String> rowIds = contentDao.getRowIds(filter, sitekey);
            List<DictionaryMongo> filteredSynonyms = dictionaryDao
                    .getFilteredData(page, count, sitekey, rowIds,
                            dictionaryName);
            List<DictionaryEntry> dictionaryEntries = transformerMap
                    .get(SYNONYMS).toEntries(filteredSynonyms);
            long totalCount = dictionaryDao.countEntries
                    (sitekey, dictionaryName);

            DictionaryData dictionaryData = getInstance(page, count,
                    totalCount, 0);
            dictionaryData.setEntries(dictionaryEntries);
            dictionaryData.setTotal(rowIds.size());
            return dictionaryData;
        } catch (DAOException e) {
            throw new RecommendException(e);
        }
    }
}
