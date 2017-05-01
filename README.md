[![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/vmware-vrealize-orchestrator-plugin/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/vmware-vrealize-orchestrator-plugin)

# Jenkins Orchestrator Plugin
---------------------
This plugin integrates [VMware vRealize Orchestrator][] to [Jenkins][]. With this plugin you can run any vRealize Orchestrator workflows.

[VMware vRealize Orchestrator]: http://www.vmware.com/products/vrealize-orchestrator/
[Jenkins]: https://jenkins-ci.org/


Configuration
-------------

1) Build step : On Job configuration page click on Add build step select “Execute Orchestrator Workflow” option

![Build step](/doc/add-build-step.png)

2) Configure :  Configure Orchestrator workflow like shown in image. Below is the description of each field

  * Server URl -   vRealize Orchestrator Server URL.
  * User Name - Username to connect to Orchestrator server.
  * Password - Password to connect to Orchestrator server.
  * Workflow Name - Name of the workflow which you want to execute.
  * Execute and Wait - If this checkbox is checked the job will wait for workflow to complete its execution.
  * Workflow Input Parameters(Add Parameter) - Once the workflow name is given, it will fetch all the input parameters of the workflow. Now you can click on Add parameter to provide values for the input parameters.

![Configure](/doc/configuration.png)


Jenkins version supported
------------------------
1.580.1 and above. To use lower version use branch version_1_565

Pipeline support
----------------
```
node {
  step([$class: 'OrchestratorBuilder', serverUrl: 'https://vra.url.com', userName: 'test_username', password: 'test_password', tenant: '', workflowName: 'test_workflow', waitExec: true, inputParams: []])
}
```
or
```
node {
  orchestratorBuilder serverUrl: 'https://vra.url.com', userName: 'test_username', password: 'test_password', tenant: '', workflowName: 'test_workflow', waitExec: true, inputParams: []
}
```


Development
===========

Start the local Jenkins instance:

    mvn hpi:run


Installing
----------
Run

	mvn hpi:hpi

to create the plugin .hpi file.

To install:

1. copy the resulting ./target/vmware-vrealize-orchestrator-plugin.hpi file to the $JENKINS_HOME/plugins directory. Don't forget to restart Jenkins afterwards.

2. or use the plugin management console (http://example.com:8080/pluginManager/advanced) to upload the hpi file. You have to restart Jenkins in order to find the plugin in the installed plugins list.

Read More
----------
https://wiki.jenkins-ci.org/display/JENKINS/VMware+vRealize+Orchestrator+Plugin

License
----------
MIT Licensed


Maintainer
----------
Agila Govindaraju <agi.raj@gmail.com>

