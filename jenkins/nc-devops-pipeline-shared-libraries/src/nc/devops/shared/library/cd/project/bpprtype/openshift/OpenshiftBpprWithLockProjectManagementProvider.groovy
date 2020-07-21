package nc.devops.shared.library.cd.project.bpprtype.openshift

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.project.OpenshiftProjectManagementProvider

class OpenshiftBpprWithLockProjectManagementProvider extends OpenshiftProjectManagementProvider {

    OpenshiftBpprWithLockProjectManagementProvider(def pipelineConfig, Openshift openshift) {
        this.openshift = openshift
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
        this.projectName = deploymentProviderParameters.cpsScript.env[lockableResourceName]
        return projectName
    }

    @Override
    void unlockResource() {
    }

    @Override
    protected void handleProjectCleanUp(DeploymentProviderParameters deploymentProviderParameters) {
        openshift.raw('delete', 'all', '--all',"--timeout=${deploymentProviderParameters.defaultTimeout}m")
    }
}