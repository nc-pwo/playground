package dms.devops.shared.library.xray.runner

import groovy.json.JsonOutput
import groovyx.net.http.ContentType
import nc.devops.shared.library.xray.runner.XrayClient

class dmsXrayClient extends XrayClient {

    dmsXrayClient(Closure restClientFactory, String restEndmsoint, ContentType requestContentType) {
        super(restClientFactory, restEndmsoint, requestContentType)
    }

    @Override
    def postExecutionResults(Map vars, def body) {
        try {
            this.authToken = this.authToken ?: authenticate(vars)
            def script = vars.script
            log(vars.loggingEnabled, script, "ProjectKey:\n$vars.testExecutionProjectKey\nRequest body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(body))}")
            def response = restClientFactory().post([
                    path              : "${vars.baseXrayUrlSuffix}$restEndmsoint",
                    body              : body,
                    headers           : [authorization: "Bearer $authToken"],
                    requestContentType: requestContentType]
            )
            response?.data.toString()
            log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        }
        catch (Exception e) {
            vars.script.echo e.toString()
            vars.script.echo "Connection to Xray fails to establish. This publishing is skip."
        }
    }
}