package com.unbxd.field.model;

import com.unbxd.skipper.relevancy.model.SearchableField;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FSSearchableField { // FS = Field Service
    private String fieldName;
    private Integer searchWeightage;

    public static List<FSSearchableField> transform(List<SearchableField> searchableFields) {
        List<FSSearchableField> resultantData = new ArrayList<>(searchableFields.size());
        FSSearchableField temporaryField;
        for(SearchableField field:searchableFields){
            temporaryField = new FSSearchableField();
            temporaryField.setFieldName(field.getFieldName());
            temporaryField.setSearchWeightage(field.getSearchWeightageForFieldService());
            resultantData.add(temporaryField);
        }
        return resultantData;
    }
}
