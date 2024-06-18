package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusWebhook {

    @JsonProperty("callback_url")
    private String callbackUrl;

    private StatusWebhook(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public static StatusWebhook getInstance(String callbackUrl) {
        return new StatusWebhook(callbackUrl);
    }
}
