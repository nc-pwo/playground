package nc.devops.shared.library.cd.kubernetes


import nc.devops.shared.library.cd.AbstractDeploymentProvider
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty

class KubernetesProvider extends AbstractDeploymentProvider implements Serializable {
    protected final DeploymentProviderParameters deploymentProviderParameters
    protected final TemplateProcessingTool processingTool
    protected final KubernetesCluster cluster
    protected final KubeCtl kubeCtl
    protected String projectName
    protected final Closure logger, parallel

    KubernetesProvider(KubernetesCluster cluster, KubeCtl kubectl, DeploymentProviderParameters parameters) {
        this.deploymentProviderParameters = parameters
        this.processingTool = parameters.templateProcessingTool
        this.projectName = null
        this.logger = parameters.logger
        this.parallel = parameters.parallel
        this.kubeCtl = kubectl
        this.cluster = cluster
        this.cluster.setKubectlConfig(parameters.kubernetesCluster)
    }

    @Override
    def deployApplication() {
        cluster.withKubeConfig(projectName) {
            throw new UnsupportedOperationException()
        }
    }

    @Override
    void verifyReadiness() {
        throw new UnsupportedOperationException()
    }

    @Override
    List<ProcessedTestProperty> processIntegrationTestParams(List<TestProperty> integrationTestParams) {
        return cluster.withKubeConfig(projectName) {
            return convertIntegrationTestParams(integrationTestParams,
                    {
                        it.appHostForValue ? "http://" + kubeCtl.getIngressHost(it.propertyValue) : it.propertyValue
                    })
        } as List<ProcessedTestProperty>
    }

    @Override
    List<ProcessedTestProperty> processIntegrationTestParamsWithoutCluster(List<TestProperty> integrationTestParams) {
        return convertIntegrationTestParams(integrationTestParams, { it.propertyValue })
    }

    @Override
    String useProject(ProjectManagementProvider projectManagementProvider) {
        this.projectName = projectManagementProvider.lockResource(deploymentProviderParameters)
        projectManagementProvider.useProject(deploymentProviderParameters)
        return projectName
    }

    @Override
    void idleApplication() {
        throw new UnsupportedOperationException()
    }

}