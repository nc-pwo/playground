import com.cloudbees.groovy.cps.NonCPS
import nc.devops.shared.library.credentials.FileCredentials
import nc.devops.shared.library.credentials.StringCredentials
import nc.devops.shared.library.credentials.UsernamePasswordCredentials

@NonCPS
UsernamePasswordCredentials usernamePassword(String credentialsId, String userParam, String passParam) {
    new UsernamePasswordCredentials(credentialsId: credentialsId, usernameParameter: userParam, passwordParameter: passParam)
}

@NonCPS
StringCredentials string(String credentialsId, String stringParam) {
    new StringCredentials(credentialsId: credentialsId, stringParameter: stringParam)
}

@NonCPS
FileCredentials file(String credentialsId, String fileParam) {
    return new FileCredentials(credentialsId: credentialsId, fileParameter: fileParam)
}