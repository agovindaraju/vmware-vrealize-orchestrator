package com.vmware.vro.jenkins.plugin.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Created by agovindaraju on 1/9/2016.
 */
public class BuildParam implements Serializable {
    private final String serverUrl;
    private final String userName;
    private final String password;
    private final String tenant;
    private final String workflowName;
    private final boolean waitExec;
    private final List<Parameter> inputParams;

    public BuildParam(String serverUrl, String userName, String password, String tenant,
                      String workflowName,
                      boolean waitExec,
                      List<Parameter> inputParams) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.password = password;
        this.tenant = tenant;
        this.workflowName = workflowName;
        this.waitExec = waitExec;
        this.inputParams = inputParams;
    }

    public BuildParam(String serverUrl, String userName, String password, String tenant,
                      String workflowName) {
        this(serverUrl, userName, password, tenant, workflowName, false, null);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public boolean isWaitExec() {
        return waitExec;
    }

    public List<Parameter> getInputParams() {
        return inputParams;
    }

    public String getTenant() {
        return tenant;
    }

    public Boolean validate() throws IOException {
        if (StringUtils.isBlank(this.getServerUrl())) {
            throw new IOException("Orchestrator server url cannot be empty");
        }

        if (StringUtils.isBlank(this.getUserName())) {
            throw new IOException("Orchestrator server username cannot be empty");
        }

        if (StringUtils.isBlank(this.getPassword())) {
            throw new IOException("Orchestrator server password cannot be empty");
        }

        if (StringUtils.isBlank(this.getWorkflowName())) {
            throw new IOException("Orchestrator workflow name cannot be empty");
        }

        return true;
    }
}
