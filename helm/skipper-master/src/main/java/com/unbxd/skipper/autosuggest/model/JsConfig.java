package com.unbxd.skipper.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsConfig {
    boolean popularProductsPresent;
}
