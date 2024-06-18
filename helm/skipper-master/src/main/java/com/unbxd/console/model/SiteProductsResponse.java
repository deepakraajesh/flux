package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteProductsResponse {
    @JsonIgnore
    private int code = 200;
    private String  errors;
    private List<String> products;

    private SiteProductsResponse(String errors, int code) {
        this.errors = errors;
        this.code = code;
    }

    public static SiteProductsResponse getInstance(String errors, int code) {
        return new SiteProductsResponse(errors, code);
    }

    @JsonIgnore
    public boolean isSuccessful() { return StringUtils.isEmpty(errors); }
}
