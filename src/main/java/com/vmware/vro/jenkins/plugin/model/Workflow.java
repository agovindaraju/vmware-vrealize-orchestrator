package com.vmware.vro.jenkins.plugin.model;

import java.io.Serializable;

/**
 * Created by agovindaraju on 1/11/2016.
 */
public class Workflow implements Serializable {
    private String name;
    private String id;

    public Workflow() {
    }

    public Workflow(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
