package nc.devops.shared.library.cd

import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty

interface DeploymentProvider extends Serializable {
    String useProject(ProjectManagementProvider projectManagementProvider)

    def deployApplication()

    void verifyReadiness()

    List<ProcessedTestProperty> processIntegrationTestParams(List<TestProperty> integrationTestParams)

    List<ProcessedTestProperty> processIntegrationTestParamsWithoutCluster(List<TestProperty> integrationTestParams)

    void idleApplication()
}