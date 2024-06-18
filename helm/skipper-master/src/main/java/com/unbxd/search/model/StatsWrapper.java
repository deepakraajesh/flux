package com.unbxd.search.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

@Data
public class StatsWrapper {
    private Stats stats;
    @JsonAnySetter
    void setStats(String name , Stats value){
        this.stats = value;
    }
}
