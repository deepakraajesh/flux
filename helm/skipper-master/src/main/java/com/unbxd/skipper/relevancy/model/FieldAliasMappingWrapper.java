package com.unbxd.skipper.relevancy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldAliasMappingWrapper {
    private List<FieldAliasMapping> data;
}

