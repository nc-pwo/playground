import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import groovyx.net.http.RESTClient
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
import nc.devops.shared.library.xray.runner.TestRunnerStrategyFactory
import nc.devops.shared.library.xray.runner.TestRunnerType

def call(def stepConfig) {
    if (stepConfig) {
        def defaultTests = [
                [resultsPath: 'build/test-results/test/spock-report',
                 runnerType : 'SPOCK'],
                [resultsPath: 'build/test-results/test',
                 runnerType : 'JUNIT']
        ]
        UsernamePasswordCredentialsImpl xrayCreds = CredentialsProvider.findCredentialById(stepConfig?.xrayCredentialsId, StandardUsernamePasswordCredentials.class, this.$build(), [])
        try {
            stepConfig += [script          : stepConfig.script ?: this,
                           xrayClientId    : xrayCreds.username,
                           xrayClientSecret: xrayCreds.password,
                           workspace       : env.WORKSPACE,
                           tests           : stepConfig.tests ?: defaultTests]
        }
        catch (NullPointerException e) {
            throw new IllegalArgumentException('Credentials with given id not found!', e)
        }
        return stepConfig.tests.collect {
            Closure restClientFactory = { -> new RESTClient(stepConfig.baseXrayUrl) }
            new TestRunnerStrategyFactory()
                    .create(TestRunnerType.valueOf(it.runnerType),
                    stepConfig.restClientFactory ?: restClientFactory,
                    it.resultsPath)
                    .importExecutionResults(stepConfig)
        }.flatten()
    }
}