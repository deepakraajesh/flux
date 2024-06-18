package com.unbxd.analyser.model;

import lombok.Data;

import java.util.List;

@Data
public class Concepts {
    Integer noOfDefaultConcepts;
    List<String> manualConcepts;
}
