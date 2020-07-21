package nc.devops.shared.library.imagerepository.type

import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.imagerepository.model.ImageRepository
import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters

class ArtifactoryImageRepository implements ImageRepository {
    private def script
    private ImageRepositoryParameters parameters

    ArtifactoryImageRepository(ImageRepositoryParameters parameters, def script) {
        this.script = script
        this.parameters = parameters
    }

    @Override
    void delete() {
        withCredentials(parameters.deleteCredentialsId) {
            script.sh "curl -u${script.env.USERNAME}:${script.env.PASSWORD} --show-error --fail -X DELETE ${parameters.stagingRepositoryUrl}/${parameters.projectName}"
        }
    }

    @Override
    void setProjectName(String projectName) {
        parameters.projectName = projectName
    }

    void setPushCredentials(BuildToolParameters buildToolParameters) {
        withCredentials(parameters.pushCredentialsId) {
            buildToolParameters.setPushRegistryUsername(script.env.USERNAME as String)
            buildToolParameters.setPushRegistryPassword(script.env.PASSWORD as String)
        }
    }

    @Override
    void withCredentials(String credentialsId, Closure closure) {
        script.withCredentials([script.usernamePassword(credentialsId: credentialsId,
                usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            closure.call()
        }
    }

    @Override
    String getDockerRepositoryName() {
        def repositoryURL = parameters.stagingRepositoryUrl
        def repositoryName = repositoryURL.path.split("/")[1]
        return "$repositoryName.${repositoryURL.host}"
    }
}
