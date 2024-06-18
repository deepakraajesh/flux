package com.unbxd.skipper.dictionary.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Omission {
    private int code;
    private String message;
    private DictionaryEntry entry;

    public static Omission getInstance(int code,
                                       String message,
                                       DictionaryEntry entry) {
        Omission omission = new Omission();
        omission.setMessage(message);
        omission.setEntry(entry);
        omission.setCode(code);
        return omission;
    }
}
