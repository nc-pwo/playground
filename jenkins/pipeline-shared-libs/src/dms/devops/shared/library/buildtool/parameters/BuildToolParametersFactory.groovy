package dms.devops.shared.library.buildtool.parameters

import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.buildtool.logging.LoggingLevel

class BuildToolParametersFactory {
    def script

    BuildToolParametersFactory(def script) {
        this.script = script
    }

    BuildToolParameters create(Map pipelineConfig) {

        BuildToolParameters buildToolParameters = new BuildToolParameters(
                mavenSettingsConfig: pipelineConfig.mavenSettingsConfig,
                sonarProfile: pipelineConfig.sonarProfile,
                buildToolCustomClass: pipelineConfig.buildToolCustomClass,
                useWrapper: pipelineConfig.useWrapper as boolean,
                loggingLevel: pipelineConfig.buildToolLoggingLevel != null ? pipelineConfig.buildToolLoggingLevel as LoggingLevel : LoggingLevel.DEFAULT
        )
        return pipelineConfig.skipPublishing == false ? withPublishingConfig(buildToolParameters, pipelineConfig?.publishingConfig as Map) : buildToolParameters
    }

    private BuildToolParameters withPublishingConfig(BuildToolParameters params, Map config) {
        params.serverId = config?.serverId
        params.prefixBuildInfoName = config?.prefixBuildInfoName
        params.releaseRepo = config?.releaseRepo
        params.snapshotRepo = config?.snapshotRepo

        script.withCredentials([script.usernamePassword(credentialsId: config?.credentials,
                usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            params.binaryRepoUsername = script.env.USERNAME
            params.binaryRepoPassword = script.env.PASSWORD
            params.pushRegistryUsername = script.env.USERNAME
            params.pushRegistryPassword = script.env.PASSWORD
        }
        return params
    }


}
