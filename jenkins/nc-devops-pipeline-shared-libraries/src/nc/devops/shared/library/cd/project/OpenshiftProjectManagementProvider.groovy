package nc.devops.shared.library.cd.project

import hudson.AbortException
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.openshift.Openshift

abstract class OpenshiftProjectManagementProvider implements ProjectManagementProvider {
    protected Openshift openshift
    protected String lockableResourceName
    protected def pipelineConfig
    protected String projectName

    @Override
    void useProject(DeploymentProviderParameters deploymentProviderParameters) {
        openshift.withCluster(deploymentProviderParameters.deliveryCluster) {
            deploymentProviderParameters.logger.call(projectName)
            def exists = openshift.selector("project", projectName).count()
            if (exists) {
                deploymentProviderParameters.logger.call("The project with name $projectName already exists.")
                return
            }
            try {
                openshift.newProject(projectName)
                openshift.raw('adm', 'policy', 'add-scc-to-user', 'anyuid', "system:serviceaccount:$projectName:default")
            }
            catch (AbortException e) {
                if (!e.message.contains("AlreadyExists")) {
                    throw e
                }
                deploymentProviderParameters.logger.call("The project already exists: ${e.message}")
            }
            deploymentProviderParameters.logger.call("The project with name $projectName is created.")
        }
    }

    @Override
    void deleteProject(DeploymentProviderParameters deploymentProviderParameters) {
        openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            def exists = openshift.selector("project", projectName).count()
            boolean isTerminating = openshift.raw("get", "project", projectName).toString().contains("Terminating")
            if (exists == 0 || isTerminating) {
                deploymentProviderParameters.logger.call("Project is either terminating or non-existing")
                return
            }
            handleProjectCleanUp(deploymentProviderParameters)
        }
    }

    abstract protected void handleProjectCleanUp(DeploymentProviderParameters deploymentProviderParameters)
}