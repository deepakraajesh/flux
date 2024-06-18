package com.unbxd.autosuggest.model;

import lombok.Data;

import java.util.List;

@Data
public class PPSearchableFieldsResponse extends GimliBaseResponse {
    List<String> popularProductSearchableFields;
}
