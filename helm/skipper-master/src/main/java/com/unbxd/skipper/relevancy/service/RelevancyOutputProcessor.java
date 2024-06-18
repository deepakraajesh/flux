package com.unbxd.skipper.relevancy.service;

import com.unbxd.skipper.relevancy.model.Field;

import java.util.List;

public interface RelevancyOutputProcessor {

    List<? extends Field> getOutputFields(String bucketData);
}
