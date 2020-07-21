package nc.devops.shared.library.tests

import nc.devops.shared.library.tests.model.IntegrationTests
import nc.devops.shared.library.tests.model.SourceRepository
import nc.devops.shared.library.tests.model.TestProperty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class IntegrationTestMapperTest {

    private IntegrationTestsMapper mapper = new IntegrationTestsMapper()
    private def parameterList = [
            [
                    propertyType : 't',
                    propertyName : 'n',
                    propertyValue: 'v'
            ]
    ]

    private def externalSource = [
            cloneDirectory: 'dir',
            url           : 'some_url',
            credentials   : 'some_credentials',
            branch        : 'some_branch'
    ]

    @Test
    void withNoConfigPublicApiTestsAreRun() {
        IntegrationTests testsProperties = mapper.create(null)
        assert !testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert !testsProperties.internalApi.runTest
        assert testsProperties.publicApi.runTest
        assert testsProperties.publicApi.testProperties.isEmpty()
    }

    @Test
    void publicApiConfigWithSkipAndExternalSourceTest() {
        def config = [
                publicApi: [
                        skip          : true,
                        parameters    : parameterList,
                        externalSource: externalSource
                ]
        ]
        IntegrationTests testsProperties = mapper.create(config)
        assert !testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert !testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.publicApi.testProperties == parameterList as List<TestProperty>

        assert testsProperties.publicApi.externalRepository.properties == (externalSource as SourceRepository).properties
    }

    @Test
    void internalApiConfigTest() {
        def config = [
                internalApi: [:]
        ]
        IntegrationTests testsProperties = mapper.create(config)
        assert !testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.publicApi.testProperties.isEmpty()
        assert testsProperties.internalApi.testProperties.isEmpty()
        assert testsProperties.internalApi.agentInheritFrom == "Kubernetes"
        assert testsProperties.internalApi.agentServiceAccount == "builder"
    }

    @Test
    void internalApiConfigWithAgentTest() {
        def config = [
                internalApi: [
                        agentInheritFrom   : "Agent",
                        agentServiceAccount: "Account"
                ]
        ]
        IntegrationTests testsProperties = mapper.create(config)
        assert !testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.publicApi.testProperties.isEmpty()
        assert testsProperties.internalApi.testProperties.isEmpty()
        assert testsProperties.internalApi.agentInheritFrom == "Agent"
        assert testsProperties.internalApi.agentServiceAccount == "Account"
    }

    @Test
    void componentConfigTest() {
        def config = [
                component: [
                        agentLabel: 'agent',
                        parameters: parameterList
                ]
        ]
        IntegrationTests testsProperties = mapper.create(config)
        assert !testsProperties.legacyMode
        assert testsProperties.component.runTest
        assert !testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.component.testProperties == parameterList as List<TestProperty>
        assert testsProperties.component.agentLabel == 'agent'

    }

    @Test
    void legacySourcePublicApiTest() {
        def pipelineConfig = [
                integrationTestParams: parameterList
        ]

        IntegrationTests testsProperties = mapper.createFromLegacySource(pipelineConfig)
        assert testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert !testsProperties.internalApi.runTest
        assert testsProperties.publicApi.runTest
        assert testsProperties.publicApi.testProperties == parameterList as List<TestProperty>

    }

    @Test
    void legacySourceInternalApiTest() {
        def pipelineConfig = [
                integrationTestParams: parameterList,
                continuousDelivery   : [
                        integrationTestWithKubernetes: true
                ]
        ]

        IntegrationTests testsProperties = mapper.createFromLegacySource(pipelineConfig)
        assert testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.internalApi.testProperties == parameterList as List<TestProperty>
        assert testsProperties.internalApi.agentInheritFrom == "Kubernetes"
        assert testsProperties.internalApi.agentServiceAccount == "builder"

    }

    @Test
    void legacySourceInternalApiWithChangedInheritAgentTest() {
        def pipelineConfig = [
                integrationTestParams: parameterList,
                continuousDelivery   : [
                        integrationTestWithKubernetes: true,
                        agentInheritFrom             : "Agent",
                        agentServiceAccount          : "Account"
                ]
        ]

        IntegrationTests testsProperties = mapper.createFromLegacySource(pipelineConfig)
        assert testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.internalApi.testProperties == parameterList as List<TestProperty>
        assert testsProperties.internalApi.agentInheritFrom == "Agent"
        assert testsProperties.internalApi.agentServiceAccount == "Account"

    }

    @Test
    void legacySourceSkipTest() {
        def pipelineConfig = [
                skipIntegrationTest  : true,
                integrationTestParams: parameterList
        ]

        IntegrationTests testsProperties = mapper.createFromLegacySource(pipelineConfig)
        assert testsProperties.legacyMode
        assert !testsProperties.component.runTest
        assert !testsProperties.internalApi.runTest
        assert !testsProperties.publicApi.runTest
        assert testsProperties.publicApi.testProperties == parameterList as List<TestProperty>

    }
}
