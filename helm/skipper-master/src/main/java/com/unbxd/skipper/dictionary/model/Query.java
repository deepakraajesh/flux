package com.unbxd.skipper.dictionary.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@Log4j2
public class Query {
    String query;
    long numberOfProductsBefore;
    long numberOfProductsAfter = 0;
    long diffCount = 0;
    long hits = 0;
    String concept;
    String phrase;
    boolean hasConcept;
    boolean hasPhrase;
    boolean hasQueryRule;
    boolean hasNoAlgo;
    String segment;

    public Query(String string) {
        String[] splittedTokens = string.split(Pattern.quote("|"));
        query = splittedTokens[0];

        if(splittedTokens.length > 1 && !splittedTokens[1].trim().isEmpty())
            numberOfProductsBefore = parse(string, splittedTokens[1].trim());
        if(splittedTokens.length > 2 && !splittedTokens[2].trim().isEmpty())
            numberOfProductsAfter = parse(string, splittedTokens[2].trim());
        if(splittedTokens.length > 3 && !splittedTokens[3].trim().isEmpty())
            hits = parse(string, splittedTokens[3].trim());
        diffCount = numberOfProductsAfter - numberOfProductsBefore;

        if(splittedTokens.length > 4 && !splittedTokens[4].trim().isEmpty()) {
            concept = splittedTokens[4].trim().replace("#", ",");
            hasConcept = true;
        }
        if(splittedTokens.length > 5 && !splittedTokens[5].trim().isEmpty()) {
            phrase = splittedTokens[5].trim().replace("#", ",");
            hasPhrase = true;
        }
        if(splittedTokens.length > 6 && !splittedTokens[6].trim().isEmpty())
            hasQueryRule =  Boolean.parseBoolean(splittedTokens[6].trim());
        if(splittedTokens.length > 7 && !splittedTokens[7].trim().isEmpty())
            segment = splittedTokens[7].trim();
        hasNoAlgo = (hasConcept || hasPhrase || hasQueryRule)?false:true;
    }

    private Long parse(String actualString, String string) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            log.error("Irregular data found " + actualString);
            return 0L;
        }
    }
}
