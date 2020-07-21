package dms.devops.shared.library.deployment.manifest

class ApplicationManifest extends Mappable {
    String applicationName
    String imageName
    String imageVersion
    String chartName
    String chartVersion
    String configurationVersion
}
