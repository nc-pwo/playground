package nc.devops.shared.library.imagerepository.model

class ImageRepositoryParameters {
    String pushCredentialsId,
           deleteCredentialsId,
           projectName
    URL stagingRepositoryUrl

    ImageRepositoryParameters(Map imageRepoConfig) {
        this.pushCredentialsId = imageRepoConfig.pushCredentialsId
        this.deleteCredentialsId = imageRepoConfig.deleteCredentialsId
        this.projectName = imageRepoConfig.projectName
        this.stagingRepositoryUrl = new URL(imageRepoConfig.stagingRepositoryUrl as String)
    }

    @Override
    String toString() {
        return """\
ImageRepositoryParameters {
    stagingRepositoryUrl = '${stagingRepositoryUrl as String}',
    projectName = '$projectName'
}"""
    }
}
