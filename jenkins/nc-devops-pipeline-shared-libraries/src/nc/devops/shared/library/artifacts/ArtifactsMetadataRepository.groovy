package nc.devops.shared.library.artifacts

interface ArtifactsMetadataRepository<T> {
    void publishBuildInfo(String serverInstance, T buildInfo)
}