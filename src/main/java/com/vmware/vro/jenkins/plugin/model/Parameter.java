package com.vmware.vro.jenkins.plugin.model;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.ExportedBean;
import com.vmware.vro.jenkins.plugin.OrchestratorClient;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

/**
 * Created by agovindaraju on 1/8/2016.
 */
@ExportedBean
public class Parameter extends AbstractDescribableImpl<Parameter> implements Serializable, Cloneable {

    private String name;
    private String type;
    private String value;

    @DataBoundConstructor
    public Parameter(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Parameter> {

        @Override
        public String getDisplayName() {
            return "Parameters for workflow";
        }

        public ListBoxModel doFillNameItems(@QueryParameter @RelativePath("..") final String serverUrl,
                                            @QueryParameter @RelativePath("..") final String userName,
                                            @QueryParameter @RelativePath("..") final String password,
                                            @QueryParameter @RelativePath("..") final String tenant,
                                            @QueryParameter @RelativePath("..") final String workflowName,
                                            @QueryParameter final String name)
                throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException,
                URISyntaxException {

            BuildParam listWorkflowBuildParam = new BuildParam(serverUrl, userName, password, tenant, null);
            OrchestratorClient listWfClient = new OrchestratorClient(listWorkflowBuildParam);
            List<Workflow> workflows = listWfClient.fetchWorkflows();
            String workflowId = null;
            for (Workflow workflow : workflows) {
                if (workflow.getName().equals(workflowName)) {
                    workflowId = workflow.getId();
                    break;
                }
            }

            BuildParam buildParam = new BuildParam(serverUrl, userName, password, tenant, workflowId);
            OrchestratorClient client = new OrchestratorClient(buildParam);
            List<Parameter> parameters = client.fetchWorkflowInputParameters();
            List<ListBoxModel.Option> options = new ArrayList<ListBoxModel.Option>(parameters.size());
            for (Parameter parameter : parameters) {
                String paramDisplayName = String.format("%s@%s", parameter.getName(), parameter.getType());
                options.add(new ListBoxModel.Option(paramDisplayName, paramDisplayName,
                        paramDisplayName.matches(name)));
            }
            ListBoxModel listBoxModel = new ListBoxModel(options);
            return listBoxModel;
        }
    }

    @Override
    public Parameter clone() throws CloneNotSupportedException {
        return (Parameter) super.clone();
    }
}
