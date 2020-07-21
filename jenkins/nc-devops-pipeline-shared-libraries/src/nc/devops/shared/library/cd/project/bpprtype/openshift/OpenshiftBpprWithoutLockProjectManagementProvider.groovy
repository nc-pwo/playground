package nc.devops.shared.library.cd.project.bpprtype.openshift

import hudson.AbortException
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.project.OpenshiftProjectManagementProvider

class OpenshiftBpprWithoutLockProjectManagementProvider extends OpenshiftProjectManagementProvider {

    OpenshiftBpprWithoutLockProjectManagementProvider(def pipelineConfig, Openshift openshift) {
        this.openshift = openshift
        this.pipelineConfig = pipelineConfig
    }

    @Override
    Map getLockableResourcesPluginOptions() {
        return new LinkedHashMap()
    }

    @Override
    String lockResource(DeploymentProviderParameters deploymentProviderParameters) {
        projectName = getBpprProjectName(deploymentProviderParameters.projectName, deploymentProviderParameters.pullRequestId, deploymentProviderParameters.projectNameSuffix, deploymentProviderParameters.projectNameDelimiter)
        return projectName
    }

    @Override
    void unlockResource() {
    }


    private String getBpprProjectName(String baseProjectName, String pullRequestId, String projectNameSuffix, String projectNameDelimiter) {
        def bpprProjectName = []
        bpprProjectName << baseProjectName
        bpprProjectName << pullRequestId
        bpprProjectName << projectNameSuffix
        return bpprProjectName.join(projectNameDelimiter)
    }

    @Override
    protected void handleProjectCleanUp(DeploymentProviderParameters deploymentProviderParameters) {
        try {
            openshift.raw('delete', 'project', projectName)
        }
        catch (AbortException e) {
            if (!e.message.contains("all content is removed")) {
                throw e
            }
            deploymentProviderParameters.logger.call("The project is already terminating: ${e.message}")
        }
        deploymentProviderParameters.timeout.call(deploymentProviderParameters.defaultTimeout) {
            openshift.selector("project", projectName).watch {
                openshift.raw('adm', 'policy', 'remove-scc-from-user', 'anyuid', "system:serviceaccount:$projectName:default")
                return (it.count() == 0)
            }
        }
    }
}