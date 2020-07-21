package nc.devops.shared.library.sonar


import hudson.model.Build
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.action.SonarAnalysisAction
import hudson.plugins.sonar.client.HttpClient
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

import static groovy.json.JsonOutput.toJson
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.when

@ExtendWith(MockitoExtension.class)
class QualityGatePollerTest {
    @Mock
    private SonarPluginAdapter plugin
    @Mock
    private HttpClient httpClient
    @Mock
    private Build rawBuild
    private PipelineMock pipeline

    private QualityGatePoller poller

    @BeforeEach
    void setup() {
        poller = new QualityGatePoller(plugin)
        pipeline = new PipelineMock(rawBuild)
        setupMocks()
    }

    private void setupMocks() {
        when(rawBuild.getAction(SonarAnalysisAction.class)).thenReturn(sonarAnalysisAction())
        when(plugin.getSonarInstallation(anyString()))
                .thenReturn(new SonarInstallation('name', 'serverUrl', 'credentialsId',
                        null, null, null, null, null))
        when(plugin.getAuthenticationToken(Mockito.any(), Mockito.any(SonarInstallation.class), eq('credentialsId')))
                .thenReturn('token')
        when(plugin.createHttpClient()).thenReturn(httpClient)
    }

    private SonarAnalysisAction sonarAnalysisAction() {
        def action = new SonarAnalysisAction('installationName', 'credentialId')
        action.serverUrl = 'actionServerUrl'
        action.ceTaskId = 'ceTaskId'
        return action
    }

    @Test
    void returnStatusOkIfAnalysisIdIsPresent() {
        whenHttpClientGetTaskThenReturn([task: [
                status       : 'A',
                componentName: 'B',
                componentKey : 'C',
                analysisId   : 'analysisId']
        ])

        whenHttpClientGetQualityGatesThenReturn('OK')

        assert poller.pollForQualityGate(pipeline) == [status: 'OK']
        Mockito.verifyNoMoreInteractions(plugin, httpClient)
    }

    @Test
    void returnErrorStatusIfAnalysisIdIsAbsent() {

        whenHttpClientGetTaskThenReturn([task: [
                status       : 'A',
                componentName: 'B',
                componentKey : 'C']
        ])

        assert poller.pollForQualityGate(pipeline) == [status: 'ERROR']
        Mockito.verifyNoMoreInteractions(plugin, httpClient)
    }

    private String whenHttpClientGetQualityGatesThenReturn(String status) {
        doReturn(toJson([projectStatus: [status: status]]))
                .when(httpClient)
                .getHttp(('actionServerUrl/api/qualitygates/project_status?analysisId=analysisId'), ('token'))
    }

    private String whenHttpClientGetTaskThenReturn(Map response) {
        doReturn(toJson(response)).when(httpClient).getHttp(('actionServerUrl/api/ce/task?id=ceTaskId'), ('token'))
    }

}
