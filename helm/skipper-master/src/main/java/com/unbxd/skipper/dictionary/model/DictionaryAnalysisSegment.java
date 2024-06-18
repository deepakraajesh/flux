package com.unbxd.skipper.dictionary.model;

import com.unbxd.skipper.dictionary.model.Query;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DictionaryAnalysisSegment {
    Integer count;
    List<Query> queries;
}
