package com.unbxd.skipper.relevancy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldAliasMapping {
    private String fieldName;
    private String alias;
}

