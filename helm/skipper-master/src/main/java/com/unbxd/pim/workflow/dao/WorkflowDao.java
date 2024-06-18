package com.unbxd.pim.workflow.dao;

import com.google.gson.JsonObject;
import com.unbxd.pim.workflow.model.WorkflowStatus;

public interface WorkflowDao {

    JsonObject fetchTemplate(String searchableField, String searchableValue);

    void saveWorkflowSnapshot(WorkflowStatus status);
}

