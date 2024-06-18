package com.unbxd.recommend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryStats {
    private int hits;
    private int after;
    private int before;
    private String query;
    private String[] rows;
    private boolean orRectified;
    private boolean spellChecked;
    private boolean conceptCorrected;
    private String secondaryLanguages;

    public int getIncrease() { return after - before; }
}
