package dms.devops.shared.library.deployment.helm


import java.time.LocalDateTime

class DeploymentConfiguration {
    String applicationName
    String imageName
    String imageVersion
    String chartName
    String chartVersion
    String namespace
    String configurationType
    String configurationVersion
    String appValuesFile
    String replicas
    LocalDateTime timestamp

    DeploymentConfiguration(String applicationName, String imageName, String imageVersion, String chartName, String chartVersion, String namespace, String configurationType, String configurationVersion, String replicas) {
        this.applicationName = applicationName
        this.imageName = imageName
        this.imageVersion = imageVersion
        this.chartName = chartName
        this.chartVersion = chartVersion
        this.namespace = namespace
        this.configurationType = configurationType
        this.configurationVersion = configurationVersion
        this.replicas = replicas
        this.timestamp = LocalDateTime.now()
        this.appValuesFile = "${configurationType}/${applicationName}.yaml"
    }
}
