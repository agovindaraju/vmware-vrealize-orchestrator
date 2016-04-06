package com.vmware.vro.jenkins.plugin.model;

import java.io.Serializable;

/**
 * Created by agovindaraju on 1/10/2016.
 */
public class ExecutionState implements Serializable {

    private String state;
    private boolean completed;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
