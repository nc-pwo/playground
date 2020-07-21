package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactoryBuildInfoRepository
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.NoMetadataRepository
import nc.devops.shared.library.artifacts.RepoType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RepositoryFactoryTest {

    @ParameterizedTest
    @MethodSource("repoConfigs")
    void createMetadataRepositoryBasedOnRepoType(RepoType repoType, Class expectedRepoClass) {
        ArtifactsMetadataRepository repository = RepositoryFactory.createMetadataRepo(repoType, null)
        assert repository.class == expectedRepoClass
    }

    private static Object[][] repoConfigs() {
        return [
                [RepoType.GENERIC, NoMetadataRepository],
                [RepoType.ARTIFACTORY, ArtifactoryBuildInfoRepository]
        ]
    }
}