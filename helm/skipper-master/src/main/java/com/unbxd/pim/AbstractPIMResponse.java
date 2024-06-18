package com.unbxd.pim;

import lombok.Data;

@Data
public class AbstractPIMResponse<T> {
    private T data;
    private String[] errors;
    private Object[] errorList;

    public boolean isDataCorrect() {
        if (data == null) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}

