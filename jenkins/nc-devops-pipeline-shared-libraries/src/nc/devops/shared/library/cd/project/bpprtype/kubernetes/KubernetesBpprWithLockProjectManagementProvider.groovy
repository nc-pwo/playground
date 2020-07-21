package nc.devops.shared.library.cd.project.bpprtype.kubernetes

import hudson.AbortException
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster
import nc.devops.shared.library.cd.project.KubernetesProjectManagementProvider

class KubernetesBpprWithLockProjectManagementProvider extends KubernetesProjectManagementProvider {

    KubernetesBpprWithLockProjectManagementProvider(def pipelineConfig, KubernetesCluster cluster, KubeCtl kubectl) {
        this.cluster = cluster
        this.cluster.setKubectlConfig(pipelineConfig.continuousDelivery.kubernetesCluster as Map)
        this.kubeCtl = kubectl
        this.pipelineConfig = pipelineConfig
    }

    @Override
    LinkedHashMap getLockableResourcesPluginOptions() {
        lockableResourceName = pipelineConfig.lockConfiguration?.availableResouceName ?: 'LOCKED_PROJECT_NAME'
        return [label: "${pipelineConfig.lockConfiguration?.bpprNamespacePoolLabel ?: 'BPPR_POOL'}", quantity: pipelineConfig.lockConfiguration?.quantity ?: 1, variable: lockableResourceName]
    }

    @Override
    String lockResource(DeploymentProviderParameters deploymentProviderParameters) {
        deploymentProviderParameters.logger.call("Lock on resource: ${deploymentProviderParameters.cpsScript.env[lockableResourceName]} performed and handled by Jenkins Lockable Resource Plugin")
        projectName = deploymentProviderParameters.cpsScript.env[lockableResourceName] as String
        return projectName
    }

    @Override
    void unlockResource() {
    }

    @Override
    protected void handleMissingNamespace() {
        throw new AbortException("Namespace: $projectName does not exist and cannot be created when lock is being used (not sufficient priviledges).")
    }

    @Override
    protected void handleNamespaceDeletion(DeploymentProviderParameters deploymentProviderParameters) {
        kubeCtl.deleteAllFromNamespace(projectName, deploymentProviderParameters.defaultTimeout)
    }
}


