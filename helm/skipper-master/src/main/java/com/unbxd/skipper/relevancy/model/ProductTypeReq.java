package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unbxd.console.model.ProductType;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductTypeReq {
    @JsonProperty(value = "product_type")
    ProductType productType;
}

