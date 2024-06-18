package com.unbxd.field.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fields {

    public static final String CATEGORY_PATH = "categoryPath";
    public static final String CATEGORY_PATH1 = "categoryPath1";
    public static final String CATEGORY_PATH2 = "categoryPath2";
    public static final String CATEGORY_PATH3 = "categoryPath3";
    public static final String CATEGORY_PATH4 = "categoryPath4";

    private String fieldName;
    private String fieldType;
}
