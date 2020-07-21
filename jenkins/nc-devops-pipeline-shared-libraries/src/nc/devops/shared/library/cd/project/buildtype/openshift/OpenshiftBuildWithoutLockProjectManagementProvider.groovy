package nc.devops.shared.library.cd.project.buildtype.openshift

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.project.OpenshiftProjectManagementProvider

class OpenshiftBuildWithoutLockProjectManagementProvider extends OpenshiftProjectManagementProvider {

    OpenshiftBuildWithoutLockProjectManagementProvider(def pipelineConfig, Openshift openshift) {
        this.openshift = openshift
        this.pipelineConfig = pipelineConfig
    }

    @Override
    Map getLockableResourcesPluginOptions() {
        return new LinkedHashMap()
    }

    @Override
    String lockResource(DeploymentProviderParameters deploymentProviderParameters) {
        this.projectName = deploymentProviderParameters.projectName
        return projectName
    }

    @Override
    void unlockResource() {

    }

    @Override
    protected void handleProjectCleanUp(DeploymentProviderParameters deploymentProviderParameters) {
        openshift.raw('delete', 'project', projectName)
        deploymentProviderParameters.timeout.call(deploymentProviderParameters.defaultTimeout) {
            openshift.selector("project", projectName).watch {
                openshift.raw('adm', 'policy', 'remove-scc-from-user', 'anyuid', "system:serviceaccount:$projectName:default")
                return (it.count() == 0)
            }
        }
    }
}