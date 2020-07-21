package nc.devops.shared.library.imagerepository.type

import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class OpenshiftImageRepositoryTest {
    private PipelineMock pipelineMock
    private ImageRepositoryParameters parameters
    private OpenshiftImageRepository openshiftImageRepository
    private BuildToolParameters buildToolParameters

    @BeforeEach
    void setUp() {
        pipelineMock = new PipelineMock()
        parameters = new ImageRepositoryParameters([deleteCredentialsId: "delete-credentials", stagingRepositoryUrl: "https://docker-registry-default.40.113.68.97.nip.io", projectName: "test-project-name"])
        buildToolParameters = new BuildToolParameters()
        openshiftImageRepository = new OpenshiftImageRepository(parameters, pipelineMock)
    }

    @Test
    void testOpenshiftCredentialsSetWereCorrectlySet() {
        openshiftImageRepository.setPushCredentials(buildToolParameters)
        assert buildToolParameters.getPushRegistryUsername() == "unused"
        assert buildToolParameters.getPushRegistryPassword() == "actual string"
    }

    @Test
    void testParsingUrlWithDockerRepositoryName() {
        assert "docker-registry-default.40.113.68.97.nip.io" == openshiftImageRepository.getDockerRepositoryName()
    }
}
