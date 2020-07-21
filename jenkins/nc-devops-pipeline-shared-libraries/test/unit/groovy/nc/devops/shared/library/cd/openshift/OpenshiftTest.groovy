package nc.devops.shared.library.cd.openshift


import nc.devops.shared.library.cd.mock.OpenshiftMock
import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
class OpenshiftTest {

    private Openshift openshift

    @Mock
    OpenshiftMock openshiftMock

    @Mock
    PipelineMock cpsScript

    private final Map APP = [templatePath: 'openshift/bppr.yml', deploymentParameters: ["PROJECT_NAME=poc-config-server-35778-bppr", "COMMIT_ID=manually_created"], hasBpprImage: true]

    @BeforeEach
    void setup() {
        openshift = new Openshift(openshiftMock, cpsScript)
    }

    @Test
    void transformDeploymentParametersTest() {
        assert openshift.transformDeploymentParameters(new ApplicationParameters(APP)) == ['openshift/bppr.yml', "PROJECT_NAME=poc-config-server-35778-bppr", "COMMIT_ID=manually_created"]
    }

    @Test
    void testProcessApplicationCalled() {
        when(openshiftMock.process(anyString(), anyList())).thenReturn([])
        when(cpsScript.readYaml(anyMap())).thenReturn([parameters: [[name: "APP_NAME", value: "some_value"]]])
        openshift.processTemplate(new ApplicationParameters(APP))
        verify(openshiftMock, times(1)).process(anyString(), any())
    }
}
