package nc.devops.shared.library.imagerepository.factory

import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters
import nc.devops.shared.library.imagerepository.type.ArtifactoryImageRepository
import nc.devops.shared.library.imagerepository.type.ImageRepoType
import nc.devops.shared.library.imagerepository.type.OpenshiftImageRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class ImageRepositoryFactoryTest {

    @Mock
    private ImageRepositoryParameters parameters

    @Mock
    private def script

    @Test
    void testArtifactoryTypeRepositoryCreation() {
        assert ArtifactoryImageRepository.class == ImageRepositoryFactory.createImageRepoObject(ImageRepoType.ARTIFACTORY, parameters, script).class
    }


    @Test
    void testOpenshiftTypeRepositoryCreation() {
        assert OpenshiftImageRepository.class == ImageRepositoryFactory.createImageRepoObject(ImageRepoType.OPENSHIFT, parameters, script).class
    }
}
