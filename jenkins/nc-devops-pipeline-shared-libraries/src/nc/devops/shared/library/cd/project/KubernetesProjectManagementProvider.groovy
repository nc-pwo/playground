package nc.devops.shared.library.cd.project

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster

abstract class KubernetesProjectManagementProvider implements ProjectManagementProvider {
    protected KubernetesCluster cluster
    protected KubeCtl kubeCtl
    protected String lockableResourceName
    protected def pipelineConfig
    protected String projectName

    protected boolean doesNamespaceExist() {
        return projectName in kubeCtl.getNamespaces()
    }

    @Override
    void useProject(DeploymentProviderParameters deploymentProviderParameters) {
        cluster.withKubeConfig {
            if (doesNamespaceExist()) {
                deploymentProviderParameters.logger.call("Namespace $projectName already exists :).")
            } else {
                handleMissingNamespace()
            }
        }
    }

    abstract protected void handleMissingNamespace()

    @Override
    void deleteProject(DeploymentProviderParameters deploymentProviderParameters) {
        cluster.withKubeConfig {
            boolean exists = doesNamespaceExist()
            if (!exists) {
                deploymentProviderParameters.logger.call("Namespace: $projectName was already deallocated.")
                return
            }
            handleNamespaceDeletion(deploymentProviderParameters)
        }
    }

    abstract protected void handleNamespaceDeletion(DeploymentProviderParameters deploymentProviderParameters)
}
