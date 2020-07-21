import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.lesfurets.jenkins.unit.BasePipelineTest
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import hudson.model.FreeStyleBuild
import hudson.model.Run
import nc.devops.shared.library.test.PipelineMock
import nc.devops.shared.library.xray.runner.TestRunnerStrategyFactory
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import groovyx.net.http.RESTClient

@RunWith(PowerMockRunner.class)
@PrepareForTest(CredentialsProvider.class)
@PowerMockIgnore(['javax.net.ssl.*'])
class PublishXrayReportTest extends BasePipelineTest {
    private def params

    private def publishXrayReport

    private PipelineMock scriptMock

    @Mock
    private UsernamePasswordCredentialsImpl credentials

    @Mock
    private FreeStyleBuild fsBuild

    @Mock
    private RESTClient xrayRestClient

    @Mock
    private TestRunnerStrategyFactory testRunnerStrategyFactory

    @Rule
    public ExpectedException expectedException = ExpectedException.none()

    private HttpResponseException httpResponseException

    private static final def mockXrayResponse = [data: ["id" : "10093",
                                                        "key": "ACC-94"]]
    private static final def mockJiraResponse = [data: ["issues":
                                                                [["id"    : "10094",
                                                                  "key"   : "AC-1",
                                                                  "fields": ["summary": "Successful test"]]]]]
    private static final def mockEmptyData = [data: []]
    private static final def spockTestsParams = [[resultsPath: 'spock/TEST-spock-report',
                                                  runnerType : 'SPOCK']]
    private static final def junitTestsParams = [[resultsPath: 'junit',
                                                  runnerType : 'JUNIT']]

    @Before
    void setUp() {
        super.setUp()
        scriptMock = new PipelineMock(null)
        httpResponseException = new HttpResponseException(new HttpResponseDecorator(new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion('HTTP', 3, 0), 401, 'Authentication failed. Invalid client credentials!')), ''))
        publishXrayReport = loadScript('vars/publishXrayReport.groovy')
        publishXrayReport.metaClass.$build = { return fsBuild }
        publishXrayReport.metaClass.env = [WORKSPACE: 'test/resources']
        params = [
                jiraProjectKey   : '',
                xrayCredentialsId: '',
                baseXrayUrl      : '',
                baseJiraUrl      : '',
                script           : '',
                restClientFactory   : { -> xrayRestClient}
        ]
        PowerMockito.mockStatic(CredentialsProvider.class)
        PowerMockito.when(CredentialsProvider.findCredentialById(Mockito.any(String.class), Mockito.any(Class.class), Mockito.any(Run.class), Mockito.anyList()))
                .thenReturn(credentials)

        helper.registerAllowedMethod("jiraLinkIssues", [Map.class], {_ -> return mockEmptyData})
        helper.registerAllowedMethod("jiraNewIssue", [Map.class], {_ -> return mockJiraResponse})
    }

    @Test
    void authenticationFailure() {
        Mockito.when(xrayRestClient.post(Mockito.anyMap()))
                .thenThrow(httpResponseException)
        def tmpParams = params + [loggingEnabled: true,
                                  tests         : junitTestsParams]
        expectedException.expect(HttpResponseException.class)
        publishXrayReport.call(tmpParams)
        Assert.fail('Exception has not been thrown')
    }

    @Test
    void junitTestsExecutionsImportSuccess() {
        Mockito.when(xrayRestClient.post(Mockito.anyMap()))
                .thenReturn([data: 'authToken'])
                .thenReturn(mockXrayResponse)
        def tmpParams = params + [loggingEnabled: false,
                                  tests         : junitTestsParams]
        def responses = publishXrayReport.call(tmpParams)

        assert responses.size() > 0
    }

    @Test
    void spockTestsExecutionsImportSuccess() {
        helper.registerAllowedMethod("jiraJqlSearch", [Map.class], {_ -> return mockJiraResponse})
        helper.registerAllowedMethod("readFile", [String.class], {_ -> return scriptMock.readFile("test/resources/spock/TEST-spock-report")})
        Mockito.when(xrayRestClient.post(Mockito.anyMap()))
                .thenReturn([data: 'authToken'])
                .thenReturn(mockXrayResponse)
        def tmpParams = params + [loggingEnabled: false,
                                  tests         : spockTestsParams]
        def responses = publishXrayReport.call(tmpParams)

        assert responses.size() > 0
    }

    @Test
    void testsExecutionsImportFailure() {
        Mockito.when(xrayRestClient.post(Mockito.anyMap()))
                .thenReturn([data: 'authToken'])
                .thenThrow(httpResponseException)
        def tmpParams = params + [loggingEnabled: false,
                                  tests         : junitTestsParams]
        expectedException.expect(HttpResponseException.class)
        publishXrayReport.call(tmpParams)
        Assert.fail('Exception has not been thrown')
    }

    @Test
    void spockCreateTestcaseSuccess() {
        helper.registerAllowedMethod("jiraJqlSearch", [Map.class], {_ -> return mockEmptyData})
        helper.registerAllowedMethod("readFile", [String.class], {_ -> return scriptMock.readFile("test/resources/spock/TEST-spock-report")})
        def tmpParams = params + [loggingEnabled: false,
                                  tests         : spockTestsParams]
        def responses = publishXrayReport.call(tmpParams)

        assert responses.size() > 0
    }

    @Test
    void spockLinkTestToStorySuccess() {
        helper.registerAllowedMethod("jiraJqlSearch", [Map.class], {_ -> return mockJiraResponse})
        helper.registerAllowedMethod("readFile", [String.class], {_ -> return scriptMock.readFile("test/resources/spock/TEST-spock-report")})
        def tmpParams = params + [loggingEnabled: false,
                                  tests         : spockTestsParams]
        def responses = publishXrayReport.call(tmpParams)

        assert responses.size() > 0
    }

    private void echo(message) {
        println message
    }
}
