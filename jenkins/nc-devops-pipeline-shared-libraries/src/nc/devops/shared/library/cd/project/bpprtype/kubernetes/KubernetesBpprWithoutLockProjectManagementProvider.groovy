package nc.devops.shared.library.cd.project.bpprtype.kubernetes

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster
import nc.devops.shared.library.cd.project.KubernetesProjectManagementProvider

class KubernetesBpprWithoutLockProjectManagementProvider extends KubernetesProjectManagementProvider {

    KubernetesBpprWithoutLockProjectManagementProvider(def pipelineConfig, KubernetesCluster cluster, KubeCtl kubeCtl) {
        this.cluster = cluster
        this.cluster.setKubectlConfig(pipelineConfig.continuousDelivery.kubernetesCluster as Map)
        this.kubeCtl = kubeCtl
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
    protected void handleMissingNamespace() {
        kubeCtl.createNamespace(projectName)
    }

    @Override
    protected void handleNamespaceDeletion(DeploymentProviderParameters deploymentProviderParameters) {
        kubeCtl.deleteNamespace(projectName, deploymentProviderParameters.defaultTimeout)
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
}
