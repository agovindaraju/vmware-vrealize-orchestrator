package com.vmware.vro.jenkins.plugin.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vmware.vro.jenkins.plugin.model.Parameter;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import static hudson.Util.fixEmptyAndTrim;

/**
 * Created by agovindaraju on 1/9/2016.
 */
public class EnvVariableResolver {
    private EnvVars environment;

    public EnvVariableResolver(AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException, InterruptedException {
        environment = build.getEnvironment(listener);
        environment.overrideAll(build.getBuildVariables());
    }

    public String getValueForBuildParameter(String buildParam) {
        return fixEmptyAndTrim(environment.expand(buildParam));
    }

    public List<Parameter> getValueForBuildParameter(List<Parameter> workflowInputParams) {
        List<Parameter> inputParams = new ArrayList<Parameter>();
        if (workflowInputParams != null) {
            for (Parameter parameter : workflowInputParams) {
                try {
                    Parameter clonedInputParam = parameter.clone();
                    clonedInputParam.setName(getValueForBuildParameter(clonedInputParam.getName()));
                    clonedInputParam.setValue(getValueForBuildParameter(clonedInputParam.getValue()));
                    clonedInputParam.setType(getValueForBuildParameter(clonedInputParam.getType()));
                    inputParams.add(clonedInputParam);
                } catch (CloneNotSupportedException e) {
                    new IOException("Not able to clone input param");
                }
            }
        }
        return inputParams;
    }

    public EnvVars getEnvironment() {
        return environment;
    }
}
