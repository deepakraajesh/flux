package com.unbxd.skipper.relevancy.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RelevancyStringField implements Field {
    private String field;

    private RelevancyStringField(String field) { this.field = field; }

    public static RelevancyStringField getInstance(String field) { return new RelevancyStringField(field); }
}
