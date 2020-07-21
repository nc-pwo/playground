package dms.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.buildtool.BuildToolType

class dmsBuildToolFactory {
    private final Map<BuildToolType, Closure<dmsBuildTool>> buildTools = new HashMap<>()

    dmsBuildToolFactory() {
        buildTools.put(BuildToolType.GRADLE, { def buildObject, ArtifactsMetadataRepository repository,
                                               BuildToolParameters buildToolParameters ->
            new dmsGradleBuildTool(repository, buildObject, buildToolParameters)
        })
    }

    dmsBuildTool createBuildTool(BuildToolType buildToolType, def buildObject, ArtifactsMetadataRepository repository,
                              BuildToolParameters buildToolParameters) {
        return buildTools.get(buildToolType).call(buildObject, repository, buildToolParameters)
    }
}
