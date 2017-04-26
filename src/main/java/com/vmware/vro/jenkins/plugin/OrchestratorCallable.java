package com.vmware.vro.jenkins.plugin;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.remoting.RoleChecker;
import com.vmware.vro.jenkins.plugin.model.BuildParam;
import com.vmware.vro.jenkins.plugin.model.ExecutionOutput;
import com.vmware.vro.jenkins.plugin.model.ExecutionState;
import hudson.remoting.Callable;

/**
 * Created by agovindaraju on 1/9/2016.
 */
public class OrchestratorCallable implements Callable<Map<String, String>, IOException>, Serializable {
    private final BuildParam buildParam;
    private static final String ORCHESTRATOR_WORKFLOW_EXECUTION_STATE = "ORCHESTRATOR_WORKFLOW_EXECUTION_STATE";
    private static final String ORCHESTRATOR_WORKFLOW_EXECUTION_OUTPUT = "ORCHESTRATOR_WORKFLOW_EXECUTION_OUTPUT";
    private static final String ORCHESTRATOR_WORKFLOW_EXECUTION_EXCEPTION = "ORCHESTRATOR_WORKFLOW_EXECUTION_EXCEPTION";

    public OrchestratorCallable(BuildParam buildParam) {
        this.buildParam = buildParam;
    }

    @Override
    public Map<String, String> call() throws IOException {
        Map<String, String> data = new HashMap<String, String>();
        try {
            OrchestratorClient client = new OrchestratorClient(buildParam);
            String executeResponseUrl = client.executeWorkflow();
            System.out.println(String.format(
                    "Invoked execute of workflow in the orchestrator server %s resulted in the execution %s",
                    buildParam.getServerUrl(), executeResponseUrl));

            if (buildParam.isWaitExec()) {
                //Now wait till the workflow is completed
                System.out.println(
                        "Build is marked and wait till execution is complete. Hence checking the status of " +
                                "workflow."
                );
                ExecutionState executionState = client.fetchWorkflowState(executeResponseUrl);
                System.out.println(String.format("Currently workflow is in %s state", executionState.getState()));

                while (!executionState.isCompleted()) {
                    Thread.sleep(10 * 1000);
                    executionState = client.fetchWorkflowState(executeResponseUrl);
                    System.out
                            .println(String.format("Currently workflow is in %s state", executionState.getState()));
                }

                //Fetch execution response
                ExecutionOutput executionOutput = client.fetchWorkflowOutputParameters(executeResponseUrl);

                System.out.print(String
                        .format("Workflow completed execution with %s state", executionOutput.getState()));
                data.put(ORCHESTRATOR_WORKFLOW_EXECUTION_STATE, executionOutput.getState());
                data.put(ORCHESTRATOR_WORKFLOW_EXECUTION_OUTPUT, executionOutput.getParameters());

                if (executionState.getState().equalsIgnoreCase("canceled") || executionState.getState()
                        .equalsIgnoreCase("failed")) {
                    data.put(ORCHESTRATOR_WORKFLOW_EXECUTION_EXCEPTION, executionOutput.getException());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage(), e);
        }

        // Return data
        return data;

    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
