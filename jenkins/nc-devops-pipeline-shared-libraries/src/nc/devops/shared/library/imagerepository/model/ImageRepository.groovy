package nc.devops.shared.library.imagerepository.model

import nc.devops.shared.library.buildtool.BuildToolParameters

interface ImageRepository {
    void delete()
    void setProjectName(String projectName)
    void setPushCredentials(BuildToolParameters buildToolParameters)
    String getDockerRepositoryName()
    void withCredentials(String credentialsId, Closure closure)
}