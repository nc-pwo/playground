package nc.devops.shared.library.imagerepository.type

import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.imagerepository.model.ImageRepository
import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters

class OpenshiftImageRepository implements ImageRepository {
    private def script
    private ImageRepositoryParameters parameters

    OpenshiftImageRepository(ImageRepositoryParameters parameters, def script) {
        this.script = script
        this.parameters = parameters
    }

    @Override
    void delete() {
    }

    @Override
    void setProjectName(String projectName) {
        parameters.projectName = projectName
    }

    @Override
    void setPushCredentials(BuildToolParameters buildToolParameters) {
        withCredentials(parameters.pushCredentialsId) {
            buildToolParameters.setPushRegistryUsername("unused")
            buildToolParameters.setPushRegistryPassword(script.env.TOKEN as String)
        }
    }

    @Override
    void withCredentials(String credentialsId, Closure closure) {
        script.withCredentials([script.string(credentialsId: credentialsId, variable: 'TOKEN')]) {
            closure.call()
        }
    }

    @Override
    String getDockerRepositoryName() {
        return parameters.getStagingRepositoryUrl().host
    }
}
