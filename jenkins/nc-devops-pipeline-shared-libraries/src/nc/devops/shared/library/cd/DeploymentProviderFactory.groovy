package nc.devops.shared.library.cd

import nc.devops.shared.library.cd.kubernetes.*
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.openshift.OpenshiftProvider
import nc.devops.shared.library.cd.templates.TemplateProcessingFactory
import nc.devops.shared.library.cd.templates.TemplateProcessingToolType
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DeploymentProviderFactory {
    private final Map<DeploymentProviderType, Closure<DeploymentProvider>> deploymentProviders = new HashMap<>()
    private final TemplateProcessingFactory templateProcessingFactory

    DeploymentProviderFactory() {
        templateProcessingFactory = new TemplateProcessingFactory()

        deploymentProviders.put(DeploymentProviderType.OPENSHIFT, { DeploymentProviderParameters parameters ->
            new OpenshiftProvider(new Openshift(parameters.cpsScript.openshift, parameters.cpsScript), parameters)
        })

        deploymentProviders.put(DeploymentProviderType.KUBERNETES, { DeploymentProviderParameters parameters ->
            return new KubernetesProvider(
                    new KubernetesCluster(parameters.cpsScript),
                    new KubeCtl(parameters.cpsScript),
                    parameters
            )
        })

        deploymentProviders.put(DeploymentProviderType.KUBERNETES_WITH_HELM, { DeploymentProviderParameters parameters ->
            return new KubernetesWithHelmProvider(
                    new KubernetesCluster(parameters.cpsScript),
                    new KubeCtl(parameters.cpsScript),
                    new Helm(parameters.cpsScript),
                    parameters
            )
        })
    }

    DeploymentProvider createDeploymentProvider(DeploymentProviderType deploymentProviderType, TemplateProcessingToolType processingToolType, DeploymentProviderParameters parameters) {
        parameters.templateProcessingTool = getProcessingTool(deploymentProviderType, processingToolType, parameters.cpsScript)
        return deploymentProviders.get(deploymentProviderType).call(parameters)
    }

    private def getProcessingTool(DeploymentProviderType deploymentProviderType, TemplateProcessingToolType processingToolType, CpsScript cpsScript) {
        if (deploymentProviderType.getAvailableProcessingTools().contains(processingToolType)) {
            return templateProcessingFactory.createTemplateProcessingTool(processingToolType, cpsScript)
        } else {
            throw new IllegalStateException("Processing tool ${processingToolType.name()} is not allowed for deployment provider of type ${deploymentProviderType.name()}")
        }
    }
}