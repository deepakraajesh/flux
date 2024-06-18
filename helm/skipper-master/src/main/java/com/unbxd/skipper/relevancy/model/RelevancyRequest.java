package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.unbxd.config.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Data
@Log4j2
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class RelevancyRequest {

    public static final String RELEVANCY_STATUS_WEBHOOK = "/site/relevancy/status";

    private String[] jobs;
    private String platform;
    private String language;
    private String vertical;
    private String webhookUrl;
    private List<String> secondaryLanguages;
    private String workflowName = "get-relevance-configs";

    public RelevancyRequest(Config config,
                            String vertical,
                            String language,
                            String feedPlatform) {
        setStatusWebhook(config.getProperty("domain.name"));
        this.vertical = vertical;
        this.language = language;
        this.platform = feedPlatform;
    }

    public RelevancyRequest(String workflowName,
                            String[] jobs, Config config,
                            String vertical, String language,
                            String feedPlatform){
        this(config, vertical, language, feedPlatform);
        this.workflowName = workflowName;
        this.jobs = jobs;
    }

    public void setStatusWebhook(String domainName) {
        this.webhookUrl = "http://" + domainName + RELEVANCY_STATUS_WEBHOOK;
    }

    public static RelevancyRequest getInstance(Config config,
                                               String jobName,
                                               String vertical,
                                               String language,
                                               String feedPlatform) {
        RelevancyRequest request = new RelevancyRequest(config,
                vertical, language, feedPlatform);
        request.setJobs(new String[]{ jobName });
        return request;
    }

    public static RelevancyRequest getInstance(Config config,
                                               String[] jobs,
                                               String vertical,
                                               String language,
                                               String feedPlatform,
                                               List<String> secondaryLanguages) {
        RelevancyRequest request = new RelevancyRequest(config,
                vertical, language, feedPlatform);
        request.setSecondaryLanguages(secondaryLanguages);
        request.setJobs(jobs);
        return request;
    }
}
