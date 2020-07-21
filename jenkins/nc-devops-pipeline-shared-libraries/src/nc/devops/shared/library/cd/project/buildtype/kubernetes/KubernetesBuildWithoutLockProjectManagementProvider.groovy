package nc.devops.shared.library.cd.project.buildtype.kubernetes

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster
import nc.devops.shared.library.cd.project.KubernetesProjectManagementProvider

class KubernetesBuildWithoutLockProjectManagementProvider extends KubernetesProjectManagementProvider {

    KubernetesBuildWithoutLockProjectManagementProvider(def pipelineConfig, KubernetesCluster cluster, KubeCtl kubeCtl) {
        this.cluster = cluster
        this.cluster.setKubectlConfig(pipelineConfig.continuousDelivery.kubernetesCluster as Map)
        this.kubeCtl = kubeCtl
        this.pipelineConfig = pipelineConfig
        this.projectName = pipelineConfig.continuousDelivery.projectName
    }

    @Override
    Map getLockableResourcesPluginOptions() {
        return new LinkedHashMap()
    }

    @Override
    String lockResource(DeploymentProviderParameters deploymentProviderParameters) {
        return deploymentProviderParameters.projectName
    }

    @Override
    protected void handleMissingNamespace() {
        kubeCtl.createNamespace(projectName)
    }

    @Override
    protected void handleNamespaceDeletion(DeploymentProviderParameters deploymentProviderParameters) {
        kubeCtl.deleteAllFromNamespace(projectName, deploymentProviderParameters.defaultTimeout)
    }

    @Override
    void unlockResource() {

    }
}
