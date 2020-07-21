package nc.devops.shared.library.cd.openshift

import hudson.AbortException
import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.credentials.CredentialsToDeploymentParametersAdapter
import org.jenkinsci.plugins.workflow.cps.CpsScript

class Openshift implements TemplateProcessingTool<OpenshiftProcessedModel> {
    def openshift
    CpsScript cpsScript

    Openshift(def openshift, CpsScript cpsScript) {
        this.openshift = openshift
        this.cpsScript = cpsScript
    }

    void newProject(String projectName) throws AbortException {
        openshift.newProject(projectName)
    }

    def withClusterAndProject(String project, String deliveryCluster, Closure body) {
        openshift.withCluster(deliveryCluster) {
            openshift.withCredentials() {
                if (project) {
                    openshift.withProject(project) {
                        body()
                    }
                } else {
                    body()
                }
            }
        }
    }

    void withCluster(String deliveryCluster, Closure body) {
        openshift.withCluster(deliveryCluster, body)
    }

    def raw(String... args) {
        openshift.raw(args)
    }

    def selector(String key, String value) {
        return openshift.selector(key, value)
    }

    def selector(String key) {
        return openshift.selector(key)
    }

    def apply(def model) throws AbortException {
        return openshift.apply(model)
    }

    def replace(def model) throws AbortException {
        return openshift.replace(model)
    }

    def delete(def model) {
        return openshift.delete(model)
    }

    def create(def model) {
        return openshift.create(model)
    }

    def verifyService(String applicaitonName) {
        return openshift.verifyService(applicaitonName)
    }

    def idle(String applicationName) {
        return openshift.idle(applicationName)
    }

    def process(def option, def models) {
        return openshift.process(option, models)
    }

    @Override
    OpenshiftProcessedModel processTemplate(ApplicationParameters applicationParameters) {
        def models = transformDeploymentParameters(applicationParameters)
        def processedTemplates = process("-f", models) as List<Map>
        return new OpenshiftProcessedModel(
                models: processedTemplates,
                applicationName: getApplicationName(applicationParameters)
        )
    }

    private String getApplicationName(ApplicationParameters applicationParameters) {
        def template = cpsScript.readYaml file: "./${applicationParameters.templatePath}"
        return template.parameters.find { it.name == 'APP_NAME' }?.value
    }

    def transformDeploymentParameters(ApplicationParameters applicationParameters) {
        def result = [applicationParameters.templatePath]
        if (applicationParameters.deploymentParameters) {
            result += applicationParameters.deploymentParameters
        }
        if (applicationParameters.credentials) {
            result += new CredentialsToDeploymentParametersAdapter(cpsScript).convert(applicationParameters.credentials)
        }
        result
    }
}
