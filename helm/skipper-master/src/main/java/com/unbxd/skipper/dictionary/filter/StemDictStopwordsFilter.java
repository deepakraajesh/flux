package com.unbxd.skipper.dictionary.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.dao.MongoDictionaryDao;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;
import com.unbxd.skipper.dictionary.validator.ErrorCode;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.unbxd.skipper.dictionary.filter.FilterConstants.*;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Log4j2
public class StemDictStopwordsFilter extends DefaultStopwordsFilter {

    private MongoDictionaryDao mongoDao;
    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    public StemDictStopwordsFilter(MongoDictionaryDao mongoDao) {
        this.mongoDao = mongoDao;
    }

    @Override
    public void filter(DictionaryContext dictionaryContext) {
        try {
            if (isEmpty(dictionaryContext.getDictionaryData().getEntries())) return;
            List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());
            addStemWords(keywords, dictionaryContext.getDictionaryData().getEntries());
            Set<String> stopwords = mongoDao.findMatchesForDictionary(dictionaryContext.getSiteKey(),
                    ASSET_DATA, keywords, asList(STOPWORDS, STOPWORDS_FRONT));
            if (isEmpty(stopwords)) return;
            List<Omission> omissions = new ArrayList<>();
            Iterator<DictionaryEntry> iterator = dictionaryContext.getDictionaryData().getEntries().iterator();
            while (iterator.hasNext()) {
                DictionaryEntry entry = iterator.next();
                DictionaryEntry copy = entry.copy();
                boolean dictionaryEntryModified = false;

                // remove stopwords in keyword
                StringBuilder result = new StringBuilder();
                boolean stopwordsRemoved = removeStopwords(entry.getName(), result, stopwords);
                if (stopwordsRemoved) {
                    dictionaryEntryModified = true;
                    String newName = result.toString();
                    if (newName.isEmpty()) {
                        iterator.remove();
                        omissions.add(Omission.getInstance(ErrorCode.STOPWORDS.getCode(),
                                "Removed entry having stopwords.", copy));
                        continue;
                    }
                    entry.setName(newName);
                }

                // remove stopwords in stemmed entry
                StringBuilder result1 = new StringBuilder();
                stopwordsRemoved = removeStopwords(entry.getStemmed(), result1, stopwords);
                if (stopwordsRemoved) {
                    dictionaryEntryModified = true;
                    String newStemmedEntry = result1.toString();
                    if (newStemmedEntry.isEmpty()) {
                        iterator.remove();
                        omissions.add(Omission.getInstance(ErrorCode.STOPWORDS.getCode(),
                                "Removed entry having stopwords.", copy));
                        continue;
                    }
                    entry.setStemmed(newStemmedEntry);
                }

                if (dictionaryEntryModified) {
                    omissions.add(Omission.getInstance(ErrorCode.STOPWORDS.getCode(),
                            "Cleaned entry having stopwords. old entry = " + mapper.writeValueAsString(copy)
                                    + " , new entry = " + mapper.writeValueAsString(entry), copy));
                }
            }
            if (isNotEmpty(omissions)) {
                dictionaryContext.getDictionaryData().getOmissions().addAll(omissions);
            }
            if (dictionaryContext.getDictionaryData().getEntries().isEmpty()) {
                throw new AssetException("All the entries contains stopwords", ErrorCode.STOPWORDS.getCode());
            }
        } catch (JsonProcessingException e) {
            log.error("error while filtering stopwords for coreName:" + dictionaryContext.getSiteKey() +
                    " error message: " + e.getMessage());
            throw new AssetException("Stopwords filtering failed", ErrorCode.JSON_PARSE_ERROR.getCode());
        } catch (DAOException e) {
            log.error("Exception while trying to filter " +
                    "stopwords in dictionaries for corename: " +
                    dictionaryContext.getSiteKey() + ", message" + e.getMessage());
        }
    }

    private void addStemWords(List<String> keywords,
                              List<DictionaryEntry> entries) {
        for (DictionaryEntry entry : entries) {
            keywords.add(entry.getStemmed());
        }
    }

}
