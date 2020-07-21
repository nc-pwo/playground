package nc.devops.shared.library.cd.project

import nc.devops.shared.library.cd.DeploymentProviderParameters

interface ProjectManagementProvider extends Serializable {
    Map getLockableResourcesPluginOptions()
    void unlockResource()
    String lockResource(DeploymentProviderParameters deploymentProviderParameters)
    void useProject(DeploymentProviderParameters deploymentProviderParameters)
    void deleteProject(DeploymentProviderParameters deploymentProviderParameters)
}