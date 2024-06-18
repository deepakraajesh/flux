package com.unbxd.analyser.model;

import lombok.Data;

import java.util.List;

@Data
public class UpdateStopWordsRequest {
    private boolean addDefaultStopWords;
    private List<String> manualStopWords;
}
