package nc.devops.shared.library.cd.kubernetes


import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.templates.ApplicationParameters

class KubernetesWithHelmProvider extends KubernetesProvider {

    private List<ApplicationParameters> applicationConfigurationList
    private Helm helm

    KubernetesWithHelmProvider(KubernetesCluster cluster, KubeCtl kubectl, Helm helm, DeploymentProviderParameters parameters) {
        super(cluster, kubectl, parameters)
        this.helm = helm
        this.applicationConfigurationList = getApplicationsParameterList(parameters)
    }

    @Override
    def deployApplication() {

        List<HelmProcessedModel> processedApplicationParams = processCharts()

        cluster.withKubeConfig(projectName) {
            Map parallelActions = processedApplicationParams.collectEntries() { applicationParameters ->
                [(applicationParameters.applicationName): { deployAndWait(applicationParameters) }]
            }
            parallel.call(parallelActions)
        }
    }

    private def deployAndWait(HelmProcessedModel applicationParameters) {
        helm.upgrade(
                applicationParameters.applicationName,
                applicationParameters.templatePath,
                applicationParameters.additionalParameters,
                deploymentProviderParameters.defaultTimeout
        )
    }

    @Override
    void verifyReadiness() {
        //do nothing, helm --wait --timeout does the job during deployment
    }

    @Override
    void idleApplication() {
    }

    private List<HelmProcessedModel> processCharts() {
        applicationConfigurationList.collect {
            processingTool.processTemplate(it) as HelmProcessedModel
        }
    }
}