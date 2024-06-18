package com.unbxd.skipper.dictionary.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class BaseDictionaryEntry {
    DictionaryEntry.Type type;
    String id;
    String name;
    String stemmed;
    String mapping;
    List<String> oneWay;
    List<String> twoWay;
    List<String> moreIds;
    List<String> excludeTerms;
}
