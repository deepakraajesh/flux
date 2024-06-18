package com.unbxd.pim.event.model;

import lombok.Data;

@Data
public class EventResponse {
    String errorMessage;
    int statusCode = 200;
}
