package nc.devops.shared.library.sonar

import com.cloudbees.groovy.cps.NonCPS
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.client.HttpClient
import hudson.plugins.sonar.utils.SonarUtils

import javax.annotation.Nullable

class SonarPluginAdapter {
    @NonCPS
    SonarInstallation getSonarInstallation(String installationName) {
        return SonarInstallation.get(installationName)
    }

    @NonCPS
    HttpClient createHttpClient() {
        return new HttpClient()
    }

    @NonCPS
    String getAuthenticationToken(def build, SonarInstallation inst, @Nullable String credentialsId) {
        return SonarUtils.getAuthenticationToken(build, inst, credentialsId)
    }
}
