package nc.devops.shared.library.tests

import nc.devops.shared.library.tests.model.IntegrationTest
import nc.devops.shared.library.tests.model.IntegrationTests
import nc.devops.shared.library.tests.model.SourceRepository
import nc.devops.shared.library.tests.model.TestProperty

class IntegrationTestsMapper {

    private final String PUBLIC_API = "publicApi"
    private final String INTERNAL_API = "internalApi"
    private final String COMPONENT = "component"

    IntegrationTests createFromLegacySource(Map<String, Object> pipelineConfig) {

        List<Map<String, Object>> integrationTestParams = (pipelineConfig?.integrationTestParams as List<Map<String, Object>>)
        Map<String, String> outerIntegrationTestsParams = pipelineConfig?.outerIntegrationTestsParams as Map<String, String>
        boolean skipIntegrationTest = pipelineConfig.get("skipIntegrationTest", false)
        boolean runWithKubernetes = pipelineConfig.continuousDelivery?.integrationTestWithKubernetes
        String agentServiceAccount = pipelineConfig?.continuousDelivery?.get("agentServiceAccount", null)
        String agentInheritFrom = pipelineConfig?.continuousDelivery?.get("agentInheritFrom", null)

        IntegrationTests tests = new IntegrationTests(
                legacyMode: true,
                component: getProperties(null, COMPONENT)
        )


        if (runWithKubernetes) {
            tests.publicApi = getProperties(null, PUBLIC_API)
            tests.internalApi = getPropertiesLegacySource(integrationTestParams, outerIntegrationTestsParams, skipIntegrationTest)

            if (agentServiceAccount != null) {
                tests.internalApi.agentServiceAccount = agentServiceAccount
            }
            if (agentInheritFrom != null) {
                tests.internalApi.agentInheritFrom = agentInheritFrom
            }

        } else {
            tests.publicApi = getPropertiesLegacySource(integrationTestParams, outerIntegrationTestsParams, skipIntegrationTest)
            tests.internalApi = getProperties(null, INTERNAL_API)
        }

        return tests
    }

    IntegrationTests create(Map<String, Map<String, Object>> testsConfig) {
        IntegrationTests tests = new IntegrationTests(
                publicApi: getProperties(testsConfig, PUBLIC_API),
                internalApi: getProperties(testsConfig, INTERNAL_API),
                component: getProperties(testsConfig, COMPONENT)
        )

        if (testsConfig == null) {
            tests.publicApi.runTest = true
        }

        return tests
    }

    private IntegrationTest getPropertiesLegacySource(List<Map<String, Object>> integrationTestParams, Map<String, String> outerIntegrationTestsParams, boolean skipIntegrationTest) {
        IntegrationTest properties = createDefaultProperties()

        if (outerIntegrationTestsParams != null) {
            properties.externalRepository = new SourceRepository(
                    cloneDirectory: outerIntegrationTestsParams.integrationTestsDir,
                    url: outerIntegrationTestsParams.integrationTestsRepoUrl,
                    credentials: outerIntegrationTestsParams.integrationTestsRepoCredentials,
                    branch: outerIntegrationTestsParams.integrationTestsRepoBranch
            )
        }

        if (integrationTestParams != null) {
            properties.testProperties = integrationTestParams as List<TestProperty>
        }

        properties.runTest = !skipIntegrationTest

        return properties
    }

    private IntegrationTest getProperties(Map<String, Map<String, Object>> testsConfig, String name) {
        Map<String, Object> config = testsConfig?.get(name)
        createProperties(config)
    }

    private IntegrationTest createProperties(Map<String, Object> testConfig) {
        IntegrationTest properties = createDefaultProperties()
        setUserProperties(properties, testConfig)
        return properties
    }


    private IntegrationTest createDefaultProperties() {
        return new IntegrationTest(
                runTest: false,
                testProperties: [],
                agentInheritFrom: "Kubernetes",
                agentServiceAccount: "builder"
        )
    }

    private void setUserProperties(IntegrationTest properties, Map<String, Object> userProperties) {
        if (userProperties == null) {
            return
        }

        properties.runTest = (userProperties.skip != null) ? !userProperties.skip : true

        if (userProperties.agentLabel) {
            properties.agentLabel = userProperties.agentLabel
        }
        if (userProperties.parameters) {
            properties.testProperties = userProperties.parameters as List<TestProperty>
        }
        if (userProperties.externalSource) {
            properties.externalRepository = userProperties.externalSource as SourceRepository
        }
        if (userProperties.agentInheritFrom) {
            properties.agentInheritFrom = userProperties.agentInheritFrom
        }
        if (userProperties.agentServiceAccount) {
            properties.agentServiceAccount = userProperties.agentServiceAccount
        }

    }

}
