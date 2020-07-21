package nc.devops.shared.library.sonar

import com.cloudbees.groovy.cps.NonCPS
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.action.SonarAnalysisAction
import hudson.plugins.sonar.client.WsClient

class QualityGatePoller {
    private final SonarPluginAdapter pluginAdapter

    QualityGatePoller(SonarPluginAdapter pluginAdapter) {
        this.pluginAdapter = pluginAdapter
    }

    def pollForQualityGate(def pipeline) {
        String projectStatus = "ERROR"
        def (String serverUrl, String ceTaskId, String token) = getSonarQubeParameters(pipeline.currentBuild.getRawBuild())
        pipeline.echo "Contacting SonarQube server: $serverUrl with taskId: $ceTaskId"
        pipeline.waitUntil {
            String intermediateStatus = checkAnalysisAndQualityGateStatus(pipeline, serverUrl, token, ceTaskId)
            if (intermediateStatus) {
                projectStatus = intermediateStatus
                return true
            }
            return false
        }
        return [status: projectStatus]
    }

    private String checkAnalysisAndQualityGateStatus(pipeline, String serverUrl, String token, String ceTaskId) {
        String projectStatus
        WsClient client = new WsClient(pluginAdapter.createHttpClient(), serverUrl, token)
        WsClient.CETask ceTask = client.getCETask(ceTaskId)
        pipeline.echo "SonarQube task ${ceTaskId} status is ${ceTask.getStatus()}"
        if (!ceTask.analysisId) {
            return null
        }
        projectStatus = client.requestQualityGateStatus(ceTask.analysisId)
        pipeline.echo "SonarQube task ${ceTaskId} completed. Quality gate is ${projectStatus}"
        return projectStatus
    }


    @NonCPS
    private List getSonarQubeParameters(def build) {
        final SonarAnalysisAction action = build.getAction(SonarAnalysisAction.class)
        final SonarInstallation sonarInstallation = pluginAdapter.getSonarInstallation(action.getInstallationName())
        final String token = pluginAdapter.getAuthenticationToken(build, sonarInstallation, sonarInstallation.getCredentialsId())
        return [action.getServerUrl(),
                action.getCeTaskId(),
                token]
    }
}
