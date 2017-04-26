package com.vmware.vro.jenkins.plugin;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.vmware.vro.jenkins.plugin.model.BuildParam;
import com.vmware.vro.jenkins.plugin.model.Parameter;
import com.vmware.vro.jenkins.plugin.model.Workflow;
import com.vmware.vro.jenkins.plugin.util.EnvVariableResolver;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

/**
 * Sample {@link Builder}.
 * <p/>
 * <p/>
 * When the user configures the project and enables this builder, {@link DescriptorImpl#newInstance(StaplerRequest)} is
 * invoked and a new {@link OrchestratorBuilder} is created. The created instance is persisted to the project
 * configuration XML by using XStream, so this allows you to use instance fields (like {@link #serverUrl}) to remember
 * the configuration.
 * <p/>
 * <p/>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author Agila Govindaraju
 */
public class OrchestratorBuilder extends Builder implements Serializable {

    private final String serverUrl;
    private final String userName;
    private final String password;
    private final String tenant;
    private final String workflowName;
    private final boolean waitExec;
    private final List<Parameter> inputParams;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public OrchestratorBuilder(String serverUrl, String userName, String password, String tenant,
                               String workflowName,
                               boolean waitExec, List<Parameter> inputParams) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.password = password;
        this.tenant = tenant;
        this.workflowName = workflowName;
        this.waitExec = waitExec;
        this.inputParams = inputParams;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException,
            IOException {
        PrintStream logger = listener.getLogger();
        EnvVariableResolver resolver = new EnvVariableResolver(build, listener);

        //Resolve the workflowName to workflowId
        BuildParam buildParam = new BuildParam(serverUrl, userName, password, tenant, null);
        OrchestratorClient client = new OrchestratorClient(buildParam);
        String workflowId = null;
        try {
            String resolvedWfName = resolver.getValueForBuildParameter(workflowName);

            List<Workflow> workflows = client.fetchWorkflows();
            for (Workflow workflow : workflows) {
                if (workflow.getName().equals(resolvedWfName)) {
                    workflowId = workflow.getId();
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }

        if (workflowId == null) {
            throw new IOException("Workflow doesn't exist in the server.");
        }

        BuildParam param = new BuildParam(resolver.getValueForBuildParameter(
                serverUrl), resolver.getValueForBuildParameter(userName),
                resolver.getValueForBuildParameter(password),
                resolver.getValueForBuildParameter(tenant),
                workflowId,
                waitExec, resolver.getValueForBuildParameter(inputParams)
        );
        logger.println("Starting Orchestrator workflow execution : " + param.getWorkflowName());
        param.validate();

        //Set build success bool
        boolean success = true;

        OrchestratorCallable callable = new OrchestratorCallable(param);
        Map<String, String> outputParameters = launcher.getChannel().call(callable);

        if (outputParameters != null && outputParameters.size() > 0) {
            logger.println("Output Parameters from the workflow execution");
            for (Map.Entry entry : outputParameters.entrySet()) {
                logger.println(entry.getKey() + " : " + entry.getValue());
            }
        }

        String state = outputParameters.get("ORCHESTRATOR_WORKFLOW_EXECUTION_STATE");

        // If the workflow run fails set the appropriate result, otherwise continue with success
        if (state.equalsIgnoreCase("canceled") || state.equalsIgnoreCase("failed")) {
            build.setResult(Result.FAILURE);
            success = false;
        } else {
            OrchestratorEnvAction orchestratorAction = new OrchestratorEnvAction(outputParameters);
            build.addAction(orchestratorAction);
        }

        return success;
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

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Execute Orchestrator Workflow";
        }

        public FormValidation doCheckServerUrl(
                @QueryParameter final String value) {

            String url = Util.fixEmptyAndTrim(value);
            if (url == null) {
                return FormValidation.error("Please enter Orchestrator server URL.");
            }

            if (url.indexOf('$') >= 0) {
                // set by variable, can't validate
                return FormValidation.ok();
            }

            try {
                new URL(value).toURI();
            } catch (MalformedURLException e) {
                return FormValidation.error("This is not a valid URI");
            } catch (URISyntaxException e) {
                return FormValidation.error("This is not a valid URI");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUserName(
                @QueryParameter final String value) {

            String username = Util.fixEmptyAndTrim(value);
            if (username == null) {
                return FormValidation.error("Please enter user name.");
            }

            if (username.indexOf('$') >= 0) {
                // set by variable, can't validate
                return FormValidation.ok();
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter final String value) {

            String password = Util.fixEmptyAndTrim(value);
            if (password == null) {
                return FormValidation.error("Please enter password.");
            }

            if (password.indexOf('$') >= 0) {
                // set by variable, can't validate
                return FormValidation.error("Environment variable cannot be used in password.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckWorkflowName(@QueryParameter final String serverUrl,
                                                  @QueryParameter final String userName,
                                                  @QueryParameter final String password,
                                                  @QueryParameter final String tenant,
                                                  @QueryParameter final String workflowName)
                throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException,
                URISyntaxException {

            String workflowNameValue = Util.fixEmptyAndTrim(workflowName);
            if (workflowNameValue == null) {
                return FormValidation.error("Please enter workflow name.");
            }

            if (workflowNameValue.indexOf('$') >= 0) {
                // set by variable, can't validate
                return FormValidation.ok();
            }
            //Call server and validate
            BuildParam buildParam = new BuildParam(serverUrl, userName, password, tenant, null);
            OrchestratorClient client = new OrchestratorClient(buildParam);
            List<Workflow> workflows = client.fetchWorkflows();

            boolean isWorkflowFound = false;
            for (Workflow workflow : workflows) {
                if (workflow.getName().equals(workflowName)) {
                    isWorkflowFound = true;
                }
            }
            if (!isWorkflowFound) {
                return FormValidation.error("Workflow with the given name doesn't exist in the server.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTenant(@QueryParameter final boolean ssoEnabled,
                                            @QueryParameter final String tenant) {

            if (ssoEnabled) {
                String tenantVal = Util.fixEmptyAndTrim(tenant);
                if (tenantVal == null) {
                    return FormValidation.error("Please enter tenant.");
                }

                if (tenantVal.indexOf('$') >= 0) {
                    // set by variable, can't validate
                    return FormValidation.ok();
                }
            }

            return FormValidation.ok();
        }
    }

    public static class OrchestratorEnvAction implements EnvironmentContributingAction {
        private transient Map<String, String> data = new HashMap<String, String>();

        public OrchestratorEnvAction() {
        }

        public OrchestratorEnvAction(Map<String, String> data) {
            this.data = data;
        }

        private void add(String key, String val) {
            if (data == null) {
                return;
            }
            data.put(key, val);
        }

        private void addAll(Map<String, String> map) {
            data.putAll(map);
        }

        @Override
        public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
            if (data != null) {
                env.putAll(data);
            }
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return null;
        }

        public Map<String, String> getData() {
            return data;
        }
    }
}

