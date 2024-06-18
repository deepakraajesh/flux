package com.unbxd.skipper.dictionary.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.Document;

@Data
@AllArgsConstructor
public class DictionaryAnalysis {
    private DictionaryAnalysisSegment good;
    private DictionaryAnalysisSegment bad;
    private DictionaryAnalysisSegment best;
}
