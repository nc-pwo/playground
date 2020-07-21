package nc.devops.shared.library.cd

import com.cloudbees.groovy.cps.NonCPS
import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty

abstract class AbstractDeploymentProvider implements DeploymentProvider {


    protected List<ProcessedTestProperty> convertIntegrationTestParams(List<TestProperty> integrationTestParams, Closure propValueTransform) {
        return integrationTestParams.collect { testProperty ->
            new ProcessedTestProperty(
                    type: testProperty.propertyType,
                    name: testProperty.propertyName,
                    value: propValueTransform(testProperty)
            )
        }
    }

    @NonCPS
    protected List<ApplicationParameters> getApplicationsParameterList(DeploymentProviderParameters deploymentProviderParameters) {

        List<ApplicationParameters> applicationList = []

        if (deploymentProviderParameters.getRequiredComponents()) {
            applicationList.addAll(
                    deploymentProviderParameters
                            .getRequiredComponents()
                            .collect { new ApplicationParameters(it) }
            )
        }

        if (deploymentProviderParameters.getTemplatePath()) {

            applicationList.add(
                    new ApplicationParameters(
                            deploymentProviderParameters.templatePath,
                            deploymentProviderParameters.deploymentParameters,
                            deploymentProviderParameters.credentialParameters,
                            true
                    )
            )
        }
        return applicationList
    }
}
