package dms.devops.shared.library.buildtool

import dms.devops.shared.library.version.VersionType
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.buildtool.GradleBuildTool
import nc.devops.shared.library.buildtool.StaticCodeAnalysisParams
import nc.devops.shared.library.gradle.Gradle
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

class dmsGradleBuildTool extends GradleBuildTool implements dmsBuildTool {
    dmsGradleBuildTool(ArtifactsMetadataRepository<?> binaryRepository, Gradle<?> selectedGradle, BuildToolParameters parameters) {
        super(binaryRepository, selectedGradle, parameters)
    }

    String testExcludeArgs;
    String dockerExcludeArgs


    void setTestExcludedArgs(String args) {
        this.testExcludeArgs = args
    }

    void setDockerExcludedArgs(String args) {
        this.dockerExcludeArgs = args
    }

    @Override
    void compile() {
        gradle "classes $dockerExcludeArgs"
    }

    @Override
    void unitTest() {
        gradle "check $dockerExcludeArgs $testExcludeArgs"
    }


    @Override
    void integrationTest() {
        gradle "integrationTest $dockerExcludeArgs"
    }

    @Override
    void functionalTest() {
        gradle "functionalTest $dockerExcludeArgs"
    }

    @Override
    void buildArtifacts(String extraArgs) {
        String buildArgs = "-x check $dockerExcludeArgs"
        if (extraArgs != null && !extraArgs.isEmpty()) {
            buildArgs += " ${extraArgs}"
        }
        gradle "build ${buildArgs}"
    }

    @Override
    void buildImage() {

        gradle "buildImage"
    }

    @Override
    String currentVersion(def script) {
        try {
            return gradleOutput(script, "currentVersion -q -Prelease.quiet").trim()
        } catch(Exception ex){
            return gradleOutput(script, "properties -q | grep \"version:\" | awk '{print \$2}'").trim()
        }
    }

    @Override
    void createRelease(VersionType type) {
        gradle "createRelease -Prelease.disableRemoteCheck -Prelease.pushTagsOnly -Prelease.versionIncrementer=${type.incrementPolicy}"
    }

    @Override
    void pushRelease() {
        gradle "pushRelease -Prelease.pushTagsOnly"
    }

    @Override
    void staticCodeAnalysis(StaticCodeAnalysisParams codeAnalysisParams) {
        gradle "--info sonarqube ${sonarqubeCodeAnalysisTool.convertParams(codeAnalysisParams)} -x check $dockerExcludeArgs"
    }

    @Override
    void publishArtifacts() {
        def buildInfo = gradle "publish -DbinaryRepoUsername=${parameters.binaryRepoUsername} -DbinaryRepoPassword=${parameters.binaryRepoPassword} -PpushRegistryUsername=${parameters.pushRegistryUsername} -PpushRegistryPassword=${parameters.pushRegistryPassword}"
        if (buildInfo instanceof BuildInfo) {
            buildInfo.name = "${parameters.prefixBuildInfoName}-${buildInfo.name}"
        }
        binaryRepository.publishBuildInfo(parameters.serverId, (Object)buildInfo)
    }

    void prepareTestExcludeArgs(def script) {
        String gradleTasks = "tasks --group Verification"
        String verificationTasks = gradleOutput(script, gradleTasks)

        String str = ""

        if (verificationTasks.contains("integrationTest")) {
            str += "-x integrationTest "
        }
        if (verificationTasks.contains("functionalTest")) {
            str += "-x functionalTest"
        }

        setTestExcludedArgs(str)
    }

    void prepareDockerExcludeArgs(def script) {
        String gradleTasks = "tasks --group docker"
        String dockerTasks = gradleOutput(script, gradleTasks)

        String str = ""

        if (dockerTasks.contains("buildImage")) {
            str += "-x buildImage"
        }

        setDockerExcludedArgs(str)
    }

    private String gradleOutput(def script, String task) {
        String gradleHome = script.tool name: 'Default', type: 'hudson.plugins.gradle.GradleInstallation'
        return script.sh(script: "$gradleHome/bin/gradle $task 2>/dev/null ", returnStdout: true)
    }
}
