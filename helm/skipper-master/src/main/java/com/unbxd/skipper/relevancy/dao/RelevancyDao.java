package com.unbxd.skipper.relevancy.dao;

import com.unbxd.skipper.relevancy.model.Field;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyOutputModel;
import com.unbxd.skipper.relevancy.model.SearchableField;

import java.util.List;

public interface RelevancyDao {

    String SITEKEY = "siteKey";
    String MONGO_ID = "_id";
    String DATA = "data";
    RelevancyOutputModel fetchRelevancyOutput(JobType type, String siteKey);

    void saveRelevancyOutput(JobType type, RelevancyOutputModel relevancyOutputModel);

    List<SearchableField> getSearchableFields(String siteKey, List<String> fieldNames);

    void appendData(JobType jobType, String siteKey, List<SearchableField> searchableFields);
}
