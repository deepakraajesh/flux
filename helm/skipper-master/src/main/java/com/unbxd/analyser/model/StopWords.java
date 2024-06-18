package com.unbxd.analyser.model;

import lombok.Data;

import java.util.List;

@Data
public class StopWords {
    Integer noOfDefaultStopWords;
    List<String> manualStopWords;
}
