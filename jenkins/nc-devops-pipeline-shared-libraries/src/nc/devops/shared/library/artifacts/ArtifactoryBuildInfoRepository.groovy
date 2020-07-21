package nc.devops.shared.library.artifacts


import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

class ArtifactoryBuildInfoRepository implements ArtifactsMetadataRepository<BuildInfo> {

    private final script

    ArtifactoryBuildInfoRepository(script) {
        this.script = script
    }

    @Override
    void publishBuildInfo(String serverInstance, BuildInfo buildInfo) {
        script.Artifactory.server(serverInstance).publishBuildInfo buildInfo
    }

}