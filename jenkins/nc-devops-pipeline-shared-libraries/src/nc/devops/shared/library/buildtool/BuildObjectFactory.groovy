package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.gradle.ArtifactoryGradle
import nc.devops.shared.library.gradle.GenericGradle
import nc.devops.shared.library.gradle.Gradle
import nc.devops.shared.library.maven.ArtifactoryMaven
import nc.devops.shared.library.maven.GenericMaven
import nc.devops.shared.library.maven.Maven

class BuildObjectFactory {

    def script

    BuildObjectFactory(def script) {
        this.script = script
    }

    BuildObject createBuildObject(RepoType repoType, BuildToolType buildToolType, String toolName, BuildToolParameters parameters) {
        if (BuildToolType.GRADLE == buildToolType) {
            return createGradleInstance(repoType, toolName, parameters.useWrapper)
        } else if (BuildToolType.MAVEN == buildToolType) {
            return createMavenInstance(repoType, toolName, parameters)
        } else {
            // may have npm or customs in the future
            return null
        }
    }

    protected Gradle createGradleInstance(RepoType repoType, String gradleName, boolean useWrapper) {
        Gradle gradleObj
        String gradleNameOrDefault = gradleName ?: 'Default'
        if (RepoType.ARTIFACTORY == repoType) {
            def artGradle = script.Artifactory.newGradleBuild()
            artGradle.tool = gradleNameOrDefault
            artGradle.usesPlugin = true
            artGradle.useWrapper = useWrapper
            gradleObj = new ArtifactoryGradle(artGradle: artGradle)
            script.echo "Created artifactory gradle instance with toolName=${artGradle.tool}."
        } else {
            gradleObj = new GenericGradle(script, gradleNameOrDefault, useWrapper)
            script.echo "Created generic gradle instance with toolName=${gradleNameOrDefault}."
        }
        gradleObj
    }

    protected Maven createMavenInstance(RepoType repoType, String mavenName, BuildToolParameters parameters) {
        Maven mavenObj
        if (RepoType.ARTIFACTORY == repoType) {
            def artMaven = script.Artifactory.newMavenBuild()
            artMaven.tool = mavenName ?: 'Default'
            def server = script.Artifactory.server(parameters.serverId)
            artMaven.deployer releaseRepo: parameters.releaseRepo, snapshotRepo: parameters.snapshotRepo, server: server
            artMaven.deployer.deployArtifacts = true
            mavenObj = new ArtifactoryMaven(artMaven: artMaven)
        } else {
            mavenObj = new GenericMaven(mavenName: mavenName ?: 'Default', mavenSettingsConfig: parameters.mavenSettingsConfig, script: script)
        }
        mavenObj
    }

}