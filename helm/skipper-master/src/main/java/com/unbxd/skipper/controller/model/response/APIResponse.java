package com.unbxd.skipper.controller.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.unbxd.skipper.controller.model.Diagnostics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class APIResponse<T> {

    private T data;
    private Diagnostics diagnostics;
    private List<ErrorResponse> errors;
    private Integer code = 200;
    
    public APIResponse(T data) {
        this.data = data;
    }

    public APIResponse(List<ErrorResponse> errors) { this.errors = errors; }

    public static APIResponse getInstance(ErrorResponse errorResponse, Integer code) {
        APIResponse resp = new APIResponse(Collections.singletonList(errorResponse));
        resp.setCode(code);
        return resp;
    }

    @JsonIgnore
    public boolean isSuccessful() { return CollectionUtils.isEmpty(errors); }
}
