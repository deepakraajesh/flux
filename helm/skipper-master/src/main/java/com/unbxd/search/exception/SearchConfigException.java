package com.unbxd.search.exception;

import lombok.Data;

@Data
public class SearchConfigException extends Exception {
    private Integer code;
    public SearchConfigException(Integer code, String messgae) {
        super(messgae);
        this.code = code;
    }
}
