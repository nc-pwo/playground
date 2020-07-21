package nc.devops.shared.library.imagerepository.type

import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class ArtifactoryImageRepositoryTest {
    private PipelineMock pipelineMock
    private ImageRepositoryParameters parameters
    private ArtifactoryImageRepository artifactoryImageRepository
    private BuildToolParameters buildToolParameters

    @BeforeEach
    void setUp() {
        pipelineMock = new PipelineMock()
        parameters = new ImageRepositoryParameters([deleteCredentialsId: "delete-credentials", stagingRepositoryUrl: "https://nc-hosting/nc-dvo-docker-staging-local", projectName: "test-project-name"])
        buildToolParameters = new BuildToolParameters()
        artifactoryImageRepository = new ArtifactoryImageRepository(parameters, pipelineMock)
    }

    @Test
    void testDeleteCredentialsSetsUsernameAndPassword() {
        artifactoryImageRepository.delete()
        assert pipelineMock.env.USERNAME == "username"
        assert pipelineMock.env.PASSWORD == "password"
    }

    @Test
    void testArtifactoryCredentialsSetWereCorrectlySet() {
        artifactoryImageRepository.setPushCredentials(buildToolParameters)
        assert buildToolParameters.getPushRegistryUsername() == "username"
        assert buildToolParameters.getPushRegistryPassword() == "password"
    }

    @Test
    void testParsingUrlWithDockerRepositoryName() {
        assert "nc-dvo-docker-staging-local.nc-hosting" == artifactoryImageRepository.getDockerRepositoryName()
    }
}
