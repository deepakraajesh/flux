package com.unbxd.console.model;

import lombok.Data;

@Data
public class ConsoleResponse {

    private int status;
    private String error;
    private String siteKey;
    private String success;
    private String id;
}
