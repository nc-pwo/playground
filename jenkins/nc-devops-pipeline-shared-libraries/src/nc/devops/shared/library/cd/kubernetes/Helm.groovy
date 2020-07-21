package nc.devops.shared.library.cd.kubernetes

import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.credentials.CredentialsToDeploymentParametersAdapter
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml
import org.jenkinsci.plugins.workflow.cps.CpsScript

class Helm implements TemplateProcessingTool<HelmProcessedModel>, Serializable {

    private final CpsScript cpsScript

    Helm(CpsScript cpsScript) {
        this.cpsScript = cpsScript
    }

    def upgrade(String applicationName, String templatePath, Map overriddenValues, int timeoutInMinutes) {
        def values = createValuesOverrideYaml(overriddenValues)
        helm("upgrade --install $applicationName --wait --timeout=${timeoutInMinutes}m $templatePath $values")
    }

    void lint(String templatePath, Map overriddenValues) {
        def values = createValuesOverrideYaml(overriddenValues)
        helm("lint $templatePath $values")
    }

    private def helm(String parameters, boolean withStdOutput = false) {
        this.cpsScript.sh script: "helm $parameters", returnStdout: withStdOutput
    }

    @Override
    HelmProcessedModel processTemplate(ApplicationParameters applicationParameters) {

        Map overrideParams = convertTemplateParametersToMap(applicationParameters)
        lint(applicationParameters.templatePath, overrideParams)

        return new HelmProcessedModel(
                applicationName: getApplicationName(applicationParameters),
                templatePath: applicationParameters.templatePath,
                additionalParameters: overrideParams
        )
    }

    private String getApplicationName(ApplicationParameters applicationParameters) {
        def chart = cpsScript.readYaml file: "./${applicationParameters.templatePath}/Chart.yaml"
        return chart.name
    }

    private Map convertTemplateParametersToMap(ApplicationParameters applicationParameters) {

        Map deploymentParams = applicationParameters.deploymentParameters?.
                collectEntries { splitToKeyAndValue(it) } ?: [:]

        Map credentialsParams = applicationParameters.credentials ? new CredentialsToDeploymentParametersAdapter(cpsScript)
                .convert(applicationParameters.credentials)
                .collectEntries() { splitToKeyAndValue(it) } : [:]

        return deploymentParams + credentialsParams
    }

    private List splitToKeyAndValue(String i) {
        def equalsSignIndex = i.indexOf('=')
        [i.substring(0, equalsSignIndex), i.substring(equalsSignIndex + 1)]
    }

    private String convertMapToYaml(Map processedTemplateParameters) {
        def yaml = new Yaml()
        return yaml.dump(processedTemplateParameters)
    }

    /**
     * Due to a bug (https://github.com/helm/helm/issues/7002) we cannot use stdin to provide values to helm.
     */
    private String createValuesOverrideYaml(Map overrideParameters) {
        if (overrideParameters.isEmpty()) {
            return ""
        } else {
            this.cpsScript.writeFile file: "override_values.yaml", text: convertMapToYaml(overrideParameters)
            return "-f override_values.yaml"
        }
    }
}
