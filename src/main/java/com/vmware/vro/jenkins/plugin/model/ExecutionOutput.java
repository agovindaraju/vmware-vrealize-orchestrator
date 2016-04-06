package com.vmware.vro.jenkins.plugin.model;

import java.io.Serializable;

/**
 * Created by agovindaraju on 1/10/2016.
 */
public class ExecutionOutput implements Serializable {

    private String state;
    private String parameters;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
