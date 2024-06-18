package com.unbxd.skipper.dictionary.model;

import static java.util.Objects.nonNull;

public enum DictionaryType {
    bck, front, ai, suggested, blacklist;

    public static boolean isAI(DictionaryType type){
        return nonNull(type) && (type.equals(ai) ||
                type.equals(suggested));
    }
}
