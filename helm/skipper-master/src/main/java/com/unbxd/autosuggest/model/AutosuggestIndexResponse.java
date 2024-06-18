package com.unbxd.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutosuggestIndexResponse {
  private String feedId;
  private String createdAt;
  private String status;
  private Integer code;
  private Map<String,String> updates;
}
