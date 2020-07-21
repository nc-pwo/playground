package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactoryBuildInfoRepository
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.NoMetadataRepository
import nc.devops.shared.library.artifacts.RepoType

class RepositoryFactory {

    static ArtifactsMetadataRepository<?> createMetadataRepo(RepoType repoType, def script) {
        switch (repoType) {
            case RepoType.ARTIFACTORY:
                return new ArtifactoryBuildInfoRepository(script)
            default:
                return new NoMetadataRepository()
        }
    }
}
