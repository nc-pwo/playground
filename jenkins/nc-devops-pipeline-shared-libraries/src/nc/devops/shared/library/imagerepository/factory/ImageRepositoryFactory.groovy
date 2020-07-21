package nc.devops.shared.library.imagerepository.factory

import nc.devops.shared.library.imagerepository.model.ImageRepository
import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters
import nc.devops.shared.library.imagerepository.type.ArtifactoryImageRepository
import nc.devops.shared.library.imagerepository.type.ImageRepoType
import nc.devops.shared.library.imagerepository.type.OpenshiftImageRepository

class ImageRepositoryFactory {
    static ImageRepository createImageRepoObject(ImageRepoType type, ImageRepositoryParameters parameters, def script) {
        if (type == ImageRepoType.ARTIFACTORY) {
            return new ArtifactoryImageRepository(parameters, script)
        } else {
            return new OpenshiftImageRepository(parameters, script)
        }
    }
}