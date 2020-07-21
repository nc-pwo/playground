package nc.devops.shared.library.artifacts

class NoMetadataRepository implements ArtifactsMetadataRepository<Void> {
    @Override
    void publishBuildInfo(String serverInstance, Void buildInfo) {
    }
}
