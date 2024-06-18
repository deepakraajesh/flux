package com.unbxd.field.model;

import lombok.Data;

@Data
public class PageResponse extends FieldServiceBaseResponse{
    private int total;
    private int count;
}
