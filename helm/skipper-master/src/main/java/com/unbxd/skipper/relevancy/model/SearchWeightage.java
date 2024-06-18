package com.unbxd.skipper.relevancy.model;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum SearchWeightage {
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    NON_SEARCHABLE(0);

    private int value; // corresponding value of the enum in field service
    private static final Map<Integer,SearchWeightage> lookupMap;

    static {
        lookupMap = new HashMap<>(4);
        for(SearchWeightage searchWeightage : EnumSet.allOf(SearchWeightage.class))
            lookupMap.put(searchWeightage.getValue(), searchWeightage);
    }

    SearchWeightage(int value) { this.value = value; }

    public int getValue() { return this.value; }

    public static SearchWeightage get(Integer value) { return lookupMap.get(value); }
}
