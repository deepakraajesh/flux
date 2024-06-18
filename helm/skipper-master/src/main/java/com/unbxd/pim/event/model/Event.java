package com.unbxd.pim.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Event {
    private String orgId;
    private EventType name;
    private String siteKey;
    // TODO: Change this name
    /**
     * adapterId is export channel adapter id in case of search
     */
    private String adapterId;
    private Map<String, Object> data;

    private String cookie;
    @JsonIgnore
    private EventResponse eventResponse;
}
