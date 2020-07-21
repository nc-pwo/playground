package dms.devops.shared.library.deployment.manifest

import java.time.LocalDateTime

class EnvironmentManifest extends Mappable {
    String namespace
    String configurationType
    String replicas
    LocalDateTime timestamp

    EnvironmentManifest(String namespace, String configurationType, String replicas, LocalDateTime timestamp) {
        this.namespace = namespace
        this.configurationType = configurationType
        this.replicas = replicas
        this.timestamp = timestamp
    }
}
