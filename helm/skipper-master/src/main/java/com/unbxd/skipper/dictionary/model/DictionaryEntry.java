package com.unbxd.skipper.dictionary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DictionaryEntry implements Comparable<DictionaryEntry> {
    private Type type;
    private String id;
    private String name;
    private String stemmed;
    private String mapping;
    private List<String> oneWay;
    private List<String> twoWay;
    private List<String> moreIds;
    private List<String> excludeTerms;
    private List<Query> good;
    private List<Query> bad;
    private List<Query> ok;
    private List<Query> best;
    private String reason;
    private Map<String, Map<String,String>> reasons;

    @JsonIgnore
    private static ObjectMapper mapper = new ObjectMapper();

    public static DictionaryEntry getInstance(String name) {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setName(name);
        return entry;
    }

    @Override
    public int compareTo(DictionaryEntry otherEntry) {
        return StringUtils.compare(otherEntry.id, id);
    }

    public enum Type {right, left, full;}

    public DictionaryEntry copy() {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setType(type);
        entry.setId(id);
        entry.setName(name);
        entry.setStemmed(stemmed);
        entry.setMapping(mapping);
        if (nonNull(oneWay)) {
            entry.setOneWay(new ArrayList<>(oneWay));
        }
        if (nonNull(twoWay)) {
            entry.setTwoWay(new ArrayList<>(twoWay));
        }
        if (nonNull(excludeTerms)) {
            entry.setExcludeTerms(new ArrayList<>(excludeTerms));
        }
        return entry;
    }

    public Boolean isEqual(DictionaryEntry input) {
        if(isNull(input)) return false;

        if((nonNull(name) ) && !name.equals(input.getName()))
            return false;
        if(nonNull(stemmed) && !stemmed.equals(input.getStemmed()))
            return false;
        if(nonNull(mapping) && !mapping.equals(input.getMapping()))
            return false;
        if(nonNull(oneWay) && !oneWay.equals(input.getOneWay()))
            return false;
        if(nonNull(twoWay) && !twoWay.equals(input.getTwoWay()))
            return false;
        if(nonNull(excludeTerms) && !excludeTerms.equals(input.getExcludeTerms()))
            return false;


        if((isNull(name) ) && nonNull(input.getName()))
            return false;
        if(isNull(stemmed) && nonNull(input.getStemmed()))
            return false;
        if(isNull(mapping) && nonNull(input.getMapping()))
            return false;
        if(isNull(oneWay) && nonNull(input.getOneWay()))
            return false;
        if(isNull(twoWay) && nonNull(input.getTwoWay()))
            return false;
        if(isNull(excludeTerms) && nonNull(input.getExcludeTerms()))
            return false;

        return true;
    }

}

