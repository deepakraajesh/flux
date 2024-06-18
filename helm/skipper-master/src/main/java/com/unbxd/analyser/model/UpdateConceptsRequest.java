package com.unbxd.analyser.model;

import lombok.Data;

import java.util.List;

@Data
public class UpdateConceptsRequest {
   private boolean addDefaultConcepts;
   private List<String> manualConcepts;
}
