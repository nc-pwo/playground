package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository

class BuildToolFactory {
    private final Map<BuildToolType, Closure<BuildTool>> buildTools = new HashMap<>()

    BuildToolFactory() {
        buildTools.put(BuildToolType.GRADLE, { def buildObject, ArtifactsMetadataRepository repository,
                                               BuildToolParameters buildToolParameters ->
            new GradleBuildTool(repository, buildObject, buildToolParameters)
        })

        buildTools.put(BuildToolType.MAVEN, { def buildObject, ArtifactsMetadataRepository repository,
                                              BuildToolParameters buildToolParameters ->
            new MavenBuildTool(repository, buildObject, buildToolParameters)
        })

        buildTools.put(BuildToolType.CUSTOM, { def buildObject, ArtifactsMetadataRepository repository,
                                               BuildToolParameters buildToolParameters ->
            try {
                return (buildToolParameters.buildToolCustomClass as Class<BuildTool>).newInstance()
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid build tool custom class provided: $buildToolParameters.buildToolCustomClass")
            }
        })
    }

    BuildTool createBuildTool(BuildToolType buildToolType, def buildObject, ArtifactsMetadataRepository repository,
                              BuildToolParameters buildToolParameters) {
        return buildTools.get(buildToolType).call(buildObject, repository, buildToolParameters)
    }
}