package nc.devops.shared.library.xray.runner

import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import nc.devops.shared.library.xray.util.DateUtil
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
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.class)
class XrayClientTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none()

    private XrayClient xrayClient

    private HttpResponseException httpResponseException

    @Mock
    DateUtil dateUtil

    @Mock
    RESTClient restClient

    @Before
    void setUp() {
        xrayClient = new XrayClient({ -> restClient}, 'endpoint', ContentType.URLENC)
        httpResponseException = new HttpResponseException(new HttpResponseDecorator(new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion('HTTP', 3, 0), 401, 'Authentication failed. Invalid client credentials!')), ''))
    }

    @Test
    void authenticationSuccess() {
        Mockito.when(restClient.post(Mockito.anyMap()))
                .thenReturn([data: 'authToken'])
        Assert.assertEquals('authToken', xrayClient.authenticate([loggingEnabled: true, script: this]))
    }

    @Test
    void authenticationInvalidCredentialsFailure() {
        Mockito.when(restClient.post(Mockito.anyMap()))
                .thenThrow(httpResponseException)
        expectedException.expect(HttpResponseException.class)
        xrayClient.authenticate([loggingEnabled: true, script: this])
        Assert.fail('Exception has not been thrown')
    }

    @Test
    void postExecutionResultsSuccess() {
        def responseMock = [data: ["id"  : "10093",
                                   "key" : "ACC-94",
                                   "self": "https://nc-****.atlassian.net/rest/api/2/issue/10093"]]
        Mockito.when(restClient.post(Mockito.anyMap()))
                .thenReturn(responseMock)
        def response = xrayClient.postExecutionResults([loggingEnabled: true, script: this], '')
        Assert.assertEquals(responseMock.data, response.data)
    }

    @Test
    void postExecutionResultsFailure() {
        Mockito.when(restClient.post(Mockito.anyMap()))
                .thenReturn([data: 'authToken'])
                .thenThrow(httpResponseException)
        expectedException.expect(HttpResponseException.class)
        xrayClient.postExecutionResults([loggingEnabled: true, script: this], {})
        Assert.fail('Exception has not been thrown')
    }

    private echo(String message) {
        println message
    }
}