package com.unbxd.autosuggest.model;

import lombok.Data;

import java.util.List;

@Data
public class PPFieldsResponse extends GimliBaseResponse {

    List<PopularProductField> PopularProductFields;
}

