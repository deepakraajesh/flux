package com.unbxd.skipper.dictionary.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.dao.DictionaryDAO;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;
import com.unbxd.skipper.dictionary.validator.ErrorCode;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.unbxd.skipper.dictionary.filter.FilterConstants.*;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@NoArgsConstructor
@Log4j2
public class DefaultStopwordsFilter implements AssetFilter {


    private DictionaryDAO mongoDao;
    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    public DefaultStopwordsFilter(DictionaryDAO mongoDao) {
        this.mongoDao = mongoDao;
    }

    @Override
    public void filter(DictionaryContext dictionaryContext) {
        try {
            if (isEmpty(dictionaryContext.getDictionaryData().getEntries())) return;
            List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());
            Set<String> stopwords = mongoDao.findMatchesForDictionary(dictionaryContext.getSiteKey(),
                    ASSET_DATA, keywords, asList(STOPWORDS, STOPWORDS_FRONT));
            if (isEmpty(stopwords)) return;
            List<Omission> omissions = new ArrayList<>();
            Iterator<DictionaryEntry> iterator = dictionaryContext.getDictionaryData().getEntries().iterator();
            while (iterator.hasNext()) {
                DictionaryEntry entry = iterator.next();
                DictionaryEntry copy = entry.copy();
                boolean dictionaryEntryModified = false;

                // remove stopwords in keyword / name
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

                if (dictionaryEntryModified) {
                    omissions.add(Omission.getInstance(ErrorCode.STOPWORDS.getCode(),
                            "Cleaned entry having stopwords. old entry = " + mapper.writeValueAsString(copy)
                                    + " , new entry = " +  mapper.writeValueAsString(entry), copy));
                }
            }
            if (isNotEmpty(omissions)) {
                dictionaryContext.getDictionaryData().getOmissions().addAll(omissions);
            }
            if (dictionaryContext.getDictionaryData().getEntries().isEmpty()) {
                throw new AssetException("All the entries contains stopwords.", ErrorCode.STOPWORDS.getCode());
            }
        } catch (JsonProcessingException e) {
            log.error("error while striping stopwords for coreName:" + dictionaryContext.getSiteKey() +
                    " error message: " + e.getMessage());
            throw new AssetException("Stopwords filtering failed.", ErrorCode.JSON_PARSE_ERROR.getCode());
        } catch (DAOException e) {
            log.error("Exception while trying to filter " +
                    "stopwords in dictionaries for corename: " +
                    dictionaryContext.getSiteKey() + ", message" + e.getMessage());
        }

    }

    protected List<String> getNames(List<DictionaryEntry> entries) {
        List<String> names = new ArrayList<>();
        for (DictionaryEntry entry : entries) {
            names.add(entry.getName());
        }
        return names;
    }

    protected boolean removeStopwords(List<String> content, Set<String> stopwords) {
        boolean modifiedContent = false;
        for (int index = 0; index < content.size(); index++) {
            StringBuilder result = new StringBuilder();
            boolean stopwordsRemoved = removeStopwords(content.get(index), result ,stopwords);
            String newRow = result.toString();
            if (!stopwordsRemoved) continue;
            modifiedContent = true;
            if (newRow.isEmpty())
                content.remove(index);
            else
                content.set(index, newRow);
        }
        return modifiedContent;
    }

    protected boolean removeStopwords(String entry, StringBuilder result, Set<String> stopwords) {
        if(StringUtils.isEmpty(entry)) return false;
        List<String> tokens = new ArrayList<>(Arrays
                .asList(entry.trim().split("\\s+")));
        int oldSize = tokens.size();
        tokens.removeIf(token -> stopwords.stream().anyMatch(token::equalsIgnoreCase));
        result.append(String.join(StringUtils.SPACE, tokens)); // if tokens.size() == 0 empty string is returned
        return oldSize != tokens.size();
    }


}
