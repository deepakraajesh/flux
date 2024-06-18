package com.unbxd.pim.workflow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import com.unbxd.pim.workflow.service.pimworkflow.model.*;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.unbxd.pim.workflow.service.PIMOrchestrationService.DATA_KEY;
import static com.unbxd.pim.workflow.service.pimworkflow.model.WorkflowNode.CALLBACK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Log4j2
public class ExportToNetwork implements WorkflowProcessor {

    private PIMRemoteService pimRemoteService;

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String WORKFLOW_CALLBACK_URL = "/admin/sites/%s/event/feed/callback";
    public static final String UPLOAD_COMPLETE_CALLBACK_URL = "/admin/sites/%s/event/feed/pimUploadComplete";

    @Inject
    public ExportToNetwork(PIMRemoteService pimRemoteService) {
        this.pimRemoteService = pimRemoteService;
    }

    @Override
    public String nextWorkflowProcessor() {
        return START_WORKFLOW;
    }

    @Override
    public void processWorkflow(WorkflowContext workflowContext) throws IOException, PimWorkflowException {
        JsonObject addNodeRequest = fetchConfigObject(workflowContext.getTemplateObject(), EXPORT_TO_NETWORK);
        if(addNodeRequest == null) {
            throw new PimWorkflowException("No config set for " + EXPORT_TO_NETWORK + " in pim workflow");
        }
        String uploadNodeId = updateCallBackNode(String.format(UPLOAD_COMPLETE_CALLBACK_URL,
                workflowContext.getSiteKey()),true,"Unbxd search self serve" +
                " service will be taking care of updating import properties to channel", workflowContext);
        String certifyToNetworkNodeId = addCertifyToNetwork(workflowContext);

        String exportNetworkId = addExportAdapter(workflowContext);
        String callbackNodeId = updateCallBackNode(String.format(WORKFLOW_CALLBACK_URL,
                workflowContext.getSiteKey()),false, "Notifying Unbxd" +
                " search for tracing",workflowContext);

        updateWorkflow(certifyToNetworkNodeId, exportNetworkId, uploadNodeId, callbackNodeId, workflowContext);
    }

    private String updateCallBackNode(String urlPath,
                                      boolean isSync,
                                      String description,
                                      WorkflowContext workflowContext)
            throws IOException, PimWorkflowException {
        String workflowId = workflowContext.getWorkflowId();

        if(isNotEmpty(workflowId)) {
            WorkflowNode<CallbackConfig> callbackNode = WorkflowNode
                    .getInstance(workflowContext.getOrgId(), urlPath,
                            isSync, workflowId, description);
            String nodeId = addWorkflow(callbackNode, workflowContext);
            return nodeId;
        }
        return EMPTY;
    }

    private void updateWorkflow(String certifyToNetworkNodeId,
                                String exportNetworkId,
                                String updateNodeId,
                                String callbackId,
                                WorkflowContext context) throws IOException, PimWorkflowException {
        UpdateWorkflowNode updateWorkflowNode = new UpdateWorkflowNode();
        List<WorkflowNodeDetails> workflowNodeList = getWorkflowNodeList(callbackId,
                updateNodeId, exportNetworkId, certifyToNetworkNodeId);
        updateWorkflowNode.setExportToNetwork(workflowNodeList);
        Response<JsonObject> resp = pimRemoteService.updateWorkflow(context.getAuthToken(),
                context.getCookie(),
                context.getOrgId(),
                context.getWorkflowId(),
                new Gson().fromJson(mapper.writeValueAsString(updateWorkflowNode), JsonObject.class)).execute();
        if(!resp.isSuccessful()) {
            throw new PimWorkflowException("Error while adding PIM Workflow code:" + resp.code() +
                    " reason:" + resp.errorBody().string());
        }
    }

    private List<WorkflowNodeDetails> getWorkflowNodeList(String callbackId,
                                                          String updateNodeId,
                                                          String exportNetworkId,
                                                          String certifyToNetworkNodeId) {
        List<WorkflowNodeDetails> workflowNodeList = new ArrayList<>();
        workflowNodeList.add(new WorkflowNodeDetails(updateNodeId, CALLBACK));
        workflowNodeList.add(new WorkflowNodeDetails(certifyToNetworkNodeId, "CERTIFY_TO_NETWORK"));
        workflowNodeList.add(new WorkflowNodeDetails(exportNetworkId, "EXPORT"));
        workflowNodeList.add(new WorkflowNodeDetails(callbackId, CALLBACK));

        return workflowNodeList;
    }

    private String addCertifyToNetwork(WorkflowContext context) throws IOException, PimWorkflowException {
        WorkflowNode node = new WorkflowNode(null, "CERTIFY_TO_NETWORK", context.getOrgId(),
                context.getWorkflowId(), new CertifytoNetworkConfig());
        return addWorkflow(node, context);
    }

    public String addWorkflow(WorkflowNode node, WorkflowContext context) throws IOException, PimWorkflowException {
        JsonObject nodeObject = new Gson().fromJson(mapper.writeValueAsString(node), JsonObject.class);
        Response<JsonObject> resp = pimRemoteService.addWorkflowNode(context.getAuthToken(),
                context.getCookie(),
                context.getOrgId(),
                context.getWorkflowId(),
                nodeObject).execute();

        if(!resp.isSuccessful()) {
            throw new PimWorkflowException("Error while adding PIM Workflow node:" + resp.code() +
                    " reason:" + resp.errorBody().string());
        }
        JsonObject jsonResponse = resp.body();
        JsonObject dataObject = jsonResponse.getAsJsonObject(DATA_KEY);
        if(dataObject == null) {
            throw new PimWorkflowException("Incorrect response from PIM while creating workflow");
        }
        if(!dataObject.has("node_id"))
            throw new PimWorkflowException("Incorrect response from PIM while creating workflow");
        return dataObject.get("node_id").getAsString();
    }

    private String addExportAdapter(WorkflowContext context) throws IOException, PimWorkflowException {
        ExportNodeConfig config = new ExportNodeConfig(Boolean.FALSE,
                context.getChannelId(), context.getOrgAppId(), context.getAdapterId(),
                "CHANNEL_EXPORT", "SCHEDULED_CHANNEL_EXPORT", context.getOrgId());
        WorkflowNode node = new WorkflowNode(null, "EXPORT",
                context.getOrgId(), context.getWorkflowId(), config);
        return addWorkflow(node, context);
    }
}

