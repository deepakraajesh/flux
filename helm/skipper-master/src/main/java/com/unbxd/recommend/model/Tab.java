package com.unbxd.recommend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tab {
    private String name;
    private String path;
    private String message;
    private List<String> columns;

    public static Tab clone(Tab other) {
        Tab tabClone = new Tab();
        tabClone.setName(other.getName());
        tabClone.setPath(other.getPath());
        tabClone.columns = new ArrayList<>();
        tabClone.setMessage(other.getMessage());
        tabClone.columns.addAll(other.getColumns());

        return tabClone;
    }
 }
