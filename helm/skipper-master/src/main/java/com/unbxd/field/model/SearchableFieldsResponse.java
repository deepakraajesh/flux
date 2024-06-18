package com.unbxd.field.model;

import lombok.Data;

import java.util.List;

@Data
public class SearchableFieldsResponse extends PageResponse {
    private List<FSSearchableField> searchableFields;
}
