package nc.devops.shared.library.xray.runner

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import groovyx.net.http.ContentType

import static groovyx.net.http.ContentType.JSON

class XrayClient {
    protected final Closure restClientFactory
    protected final String restEndpoint
    protected final ContentType requestContentType
    protected String authToken

    XrayClient(Closure restClientFactory, String restEndpoint, ContentType requestContentType) {
        this.restClientFactory = restClientFactory
        this.restEndpoint = restEndpoint
        this.requestContentType = requestContentType
    }

    String authenticate(Map vars) {
        def script = vars.script
        script.echo 'Xray: authenticating...'
        def response = restClientFactory().post([
                path              : "${vars.baseXrayUrlSuffix}/authenticate",
                body              : [client_id    : vars.xrayClientId,
                                     client_secret: vars.xrayClientSecret.toString()],
                requestContentType: JSON]
        )

        script.echo 'Xray: authentication success'
        return response?.data
    }

    @NonCPS
    def postExecutionResults(Map vars, def body) {
        this.authToken = this.authToken ?: authenticate(vars)
        def script = vars.script
        log(vars.loggingEnabled, script, "ProjectKey:\n$vars.jiraProjectKey\nRequest body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(body))}")
        def response = restClientFactory().post([
                path              : "${vars.baseXrayUrlSuffix}$restEndpoint",
                query             : [projectKey: vars.jiraProjectKey],
                body              : body,
                headers           : [authorization: "Bearer $authToken"],
                requestContentType: requestContentType]
        )
        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    void log(boolean loggingEnabled, def script, String message) {
        if (loggingEnabled)
            script.echo message
    }

}