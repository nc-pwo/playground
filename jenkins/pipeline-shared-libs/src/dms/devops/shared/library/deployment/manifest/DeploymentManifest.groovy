package dms.devops.shared.library.deployment.manifest

import dms.devops.shared.library.deployment.helm.DeploymentConfiguration

class DeploymentManifest extends Mappable {
    String status
    ApplicationManifest applicationConfiguration
    EnvironmentManifest environmentConfiguration

    DeploymentManifest(DeploymentConfiguration config, String status) {
        this.status = status
        this.applicationConfiguration = [
                applicationName     : config.applicationName,
                imageName           : config.imageName,
                imageVersion        : config.imageVersion,
                chartName           : config.chartName,
                chartVersion        : config.chartVersion,
                configurationVersion: config.configurationVersion
        ] as ApplicationManifest

        this.environmentConfiguration = new EnvironmentManifest(
                config.namespace,
                config.configurationType,
                config.replicas,
                config.timestamp
        )
    }
}
