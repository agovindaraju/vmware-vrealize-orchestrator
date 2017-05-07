package com.vmware.vro.jenkins.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vmware.vro.jenkins.plugin.model.BuildParam;
import com.vmware.vro.jenkins.plugin.model.ExecutionOutput;
import com.vmware.vro.jenkins.plugin.model.ExecutionState;
import com.vmware.vro.jenkins.plugin.model.Parameter;
import com.vmware.vro.jenkins.plugin.model.Workflow;
import com.vmware.vro.jenkins.plugin.util.RestClient;

/**
 * OrchestratorClient helps to execute rest APIs on the orchestrator server.
 *
 * Created by agovindaraju on 1/9/2016.
 */
public class OrchestratorClient {
    private static final String WORKFLOWS_SERVICE = "%s/vco/api/workflows";
    private static final String WORKFLOW_SERVICE = "%s/vco/api/workflows/%s";
    private static final String WORKFLOW_EXECUTION_SERVICE = "%s/vco/api/workflows/%s/executions/";

    //JSON keys
    private static final String INPUT_PARAMETERS = "input-parameters";
    private static final String OUTPUT_PARAMETERS = "output-parameters";
    private static final String CONTENT_EXCEPTION = "content-exception";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String VALUE = "value";
    private static final String ID = "id";
    private static final String SDK_OBJECT = "sdk-object";
    private static final String PARAMETERS = "parameters";
    private static final String STATE = "state";
    private static final String LINK = "link";
    private static final String ATTRIBUTES = "attributes";

    private final BuildParam buildParam;
    private final RestClient restClient;

    public OrchestratorClient(BuildParam buildParam) {
        this.buildParam = buildParam;
        this.restClient = new RestClient(buildParam.getServerUrl(), buildParam.getUserName(), buildParam.getPassword(),
                buildParam.getTenant());
    }

    /**
     * Gets all the workflows from the orchestrator server
     */
    public List<Workflow> fetchWorkflows()
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        List<Workflow> workflows = new ArrayList<Workflow>();
        String requestUrl = String.format(WORKFLOWS_SERVICE, buildParam.getServerUrl());
        String workflowsResponse = restClient.httpGet(requestUrl);
        JsonObject workflowsJson = getJsonObject(workflowsResponse);
        if (workflowsJson.has(LINK)) {
            JsonArray jsonArray = workflowsJson.getAsJsonArray(LINK);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject workflowJson = jsonArray.get(i).getAsJsonObject();
                if (workflowJson.has(ATTRIBUTES)) {
                    JsonArray attrJsonArray = workflowJson.getAsJsonArray(ATTRIBUTES);
                    Workflow workflow = new Workflow();
                    for (int j = 0; j < attrJsonArray.size(); j++) {
                        JsonObject jsonObject = attrJsonArray.get(j).getAsJsonObject();
                        String name = jsonObject.get(NAME).getAsString();
                        String value = null;
                        if (jsonObject.has(VALUE)) {
                            value = jsonObject.get(VALUE).getAsString();
                        }
                        if (NAME.equals(name)) {
                            workflow.setName(value);
                        } else if (ID.equals(name)) {
                            workflow.setId(value);
                        }
                    }
                    workflows.add(workflow);
                }
            }
        }
        return workflows;
    }

    /**
     * Fetch input parameters if any for the given workflow name
     */
    public List<Parameter> fetchWorkflowInputParameters()
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        //TODO Fetch all workflows and match the name to get the workflow Id. For now test with Id from the ui.
        List<Parameter> parameters = new ArrayList<Parameter>();
        if (StringUtils.isNotBlank(buildParam.getWorkflowName())) {
            String requestUrl = String.format(WORKFLOW_SERVICE, buildParam.getServerUrl(), getEncodedString(
                    buildParam.getWorkflowName()));
            String workflowResponse = restClient.httpGet(requestUrl);
            //Parse workflow response to get the input parameters
            JsonObject jsonObject = getJsonObject(workflowResponse);
            if (jsonObject.has(INPUT_PARAMETERS)) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(INPUT_PARAMETERS);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject inputParam = jsonArray.get(i).getAsJsonObject();
                    String name = inputParam.get(NAME).getAsString();
                    String type = inputParam.get(TYPE).getAsString();
                    parameters.add(new Parameter(name, type, ""));
                }
            }
        }
        return parameters;
    }

    /**
     * Executes the workflow with the given input parameters and returns the executed url if successfully accepted by
     * the server.
     */
    public String executeWorkflow()
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        //TODO Fetch all workflows and match the name to get the workflow Id. For now test with Id from the ui.
        String requestUrl = String
                .format(WORKFLOW_EXECUTION_SERVICE, buildParam.getServerUrl(), getEncodedString(
                        buildParam.getWorkflowName()));
        String payLoad = constructRequestPayload(buildParam.getInputParams());
        if (StringUtils.isBlank(payLoad)) {
            payLoad = "{}";
        }
        System.out.println("Execute Payload : " + payLoad);
        return restClient.httpPostForLocationHeader(requestUrl, payLoad);
    }

    /**
     * Return true if workflow is completed, false otherwise
     */
    public ExecutionState fetchWorkflowState(String requestUrl)
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        ExecutionState executionState = new ExecutionState();
        String workflowResponse = restClient.httpGet(requestUrl);
        JsonObject responseJson = getJsonObject(workflowResponse);
        if (responseJson.has(STATE)) {
            String state = responseJson.get(STATE).getAsString();
            executionState.setState(state);
            if (state.equals("completed") || state.equals("canceled") || state.equals("failed")) {
                executionState.setCompleted(true);
                return executionState;
            }
        }
        return executionState;
    }

    /**
     * If workflow is completed, it fetches the output parameters if any
     */
    public ExecutionOutput fetchWorkflowOutputParameters(String requestUrl)
            throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        ExecutionOutput executionOutput = new ExecutionOutput();
        String workflowResponse = restClient.httpGet(requestUrl);
        JsonObject responseJson = getJsonObject(workflowResponse);
        if (responseJson.has(STATE)) {
            String state = responseJson.get(STATE).getAsString();
            executionOutput.setState(state);
        }
        if (responseJson.has(OUTPUT_PARAMETERS)) {
            JsonArray jsonArray = responseJson.getAsJsonArray(OUTPUT_PARAMETERS);
            executionOutput.setParameters(jsonArray.toString());
        }
        if (responseJson.has(CONTENT_EXCEPTION)) {
            String exception = responseJson.get(CONTENT_EXCEPTION).getAsString();
            executionOutput.setException(exception);
        }
        return executionOutput;
    }

    private String constructRequestPayload(List<Parameter> inputParams) {
        if (inputParams != null && inputParams.size() > 0) {
            JsonObject parametersJsonObj = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            for (Parameter parameter : inputParams) {
                JsonObject jsonObject = new JsonObject();
                int index = parameter.getName().indexOf('@');
                String name = parameter.getName().substring(0, index);
                String type = parameter.getName().substring(index + 1, parameter.getName().length());

                jsonObject.addProperty(NAME, name);
                jsonObject.addProperty(TYPE, type);
                jsonObject.add(VALUE, constructValueObjectBasedOnType(type, parameter.getValue()));

                jsonArray.add(jsonObject);
            }
            parametersJsonObj.add(PARAMETERS, jsonArray);
            return parametersJsonObj.toString();
        }
        return null;
    }

    private JsonObject constructValueObjectBasedOnType(String type, String value) {
        if (isPrimitiveType(type)) {
            JsonObject valueObj = new JsonObject();
            valueObj.addProperty(VALUE, value);

            JsonObject valueTypeObj = new JsonObject();
            if (type.equalsIgnoreCase("EncryptedString") || type.equalsIgnoreCase("SecureString")) {
                valueTypeObj.add("string", valueObj);
            } else {
                valueTypeObj.add(type.toLowerCase(), valueObj);
            }
            return valueTypeObj;
        } else {
            //Assuming it is sdk-object type
            JsonObject sdkObjectContent = new JsonObject();
            sdkObjectContent.addProperty(ID, value);
            sdkObjectContent.addProperty(TYPE, type);

            JsonObject sdkObject = new JsonObject();
            sdkObject.add(SDK_OBJECT, sdkObjectContent);
            return sdkObject;
        }
    }

    private boolean isPrimitiveType(String type) {
        //Not supported are properties / array / mime-attachment / regex / composite
        return type.equalsIgnoreCase("string") || type.equalsIgnoreCase("EncryptedString") || type.equalsIgnoreCase(
                "SecureString") || type
                .equalsIgnoreCase("number") || type.equalsIgnoreCase("date") || type.equalsIgnoreCase("boolean");
    }

    private JsonObject getJsonObject(String response) {
        JsonElement responseJson = new JsonParser().parse(response);
        return responseJson.getAsJsonObject();
    }

    private String getEncodedString(String name) throws UnsupportedEncodingException {
        return URLEncoder.encode(name, String.valueOf(StandardCharsets.UTF_8));
    }
}
