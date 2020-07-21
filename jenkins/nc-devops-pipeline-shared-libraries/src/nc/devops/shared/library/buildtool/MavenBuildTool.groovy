package nc.devops.shared.library.buildtool

import com.cloudbees.groovy.cps.NonCPS
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.buildtool.logging.LoggingLevel
import nc.devops.shared.library.maven.ArtifactoryMaven
import nc.devops.shared.library.maven.Maven
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

class MavenBuildTool implements BuildTool {
    protected final String TEST_RESULT_PATH = '**/*-reports/*.xml'
    protected final ArtifactsMetadataRepository<?> binaryRepository
    protected Maven<?> selectedMaven
    protected BuildToolParameters parameters
    protected final SonarqubeCodeAnalysisTool sonarqubeCodeAnalysisTool
    protected final TestPropertiesConverter propertiesConverter
    protected final String loggingOptions

    MavenBuildTool(ArtifactsMetadataRepository<?> binaryRepository, Maven<?> selectedMaven, BuildToolParameters parameters) {
        this.binaryRepository = binaryRepository
        this.selectedMaven = selectedMaven
        this.parameters = parameters
        this.sonarqubeCodeAnalysisTool = new SonarqubeCodeAnalysisTool()
        this.propertiesConverter = new TestPropertiesConverterImpl()
        this.loggingOptions = getLoggingOption(parameters?.loggingLevel)
    }

    protected mvn(String args) {
        String command = loggingOptions == null ? args : "$loggingOptions $args"
        selectedMaven.call(command)
    }

    @Override
    void clean() {
        mvn'clean'
    }

    @Override
    void unitTest() {
        mvn 'test'
    }

    @Override
    void buildArtifacts() {
        mvn 'package -DskipTests'
    }

    @Override
    void staticCodeAnalysis(StaticCodeAnalysisParams codeAnalysisParams) {
        String sonarProfile = parameters.sonarProfile ?: 'sonar-profile'
        mvn "sonar:sonar -P $sonarProfile ${sonarqubeCodeAnalysisTool.convertParams(codeAnalysisParams)}"
    }

    @Override
    void publishArtifacts(List<String> additionalTags) {
        def buildInfo
        String phase = "deploy"
        if(selectedMaven instanceof ArtifactoryMaven) {
            phase = "install"
        }
        buildInfo = mvn "$phase -DskipTests -Dregistry.username=${parameters.pushRegistryUsername} -Dregistry.password=${parameters.pushRegistryPassword}"
        if(buildInfo instanceof BuildInfo) {
            buildInfo.name = "${parameters.prefixBuildInfoName}-${buildInfo.name}"
        }
        binaryRepository.publishBuildInfo(parameters.getServerId(), (Object) buildInfo)
    }

    @Override
    void publishImages(String registryURL, String openshiftProjectName) {
        mvn "package -DskipTests docker:push -Dregistry.url=${registryURL} -DopenshitProject=${openshiftProjectName} " +
                "-Dregistry.username=${parameters.pushRegistryUsername} -Dregistry.password=${parameters.pushRegistryPassword}"
    }

    @Override
    void integrationTest(List<ProcessedTestProperty> properties) {
        String propertiesAsString = propertiesConverter.convert(properties)
        mvn "integration-test $propertiesAsString"
    }

    @Override
    void integrationTestPublicApi(List<ProcessedTestProperty> properties) {
        String propertiesAsString = propertiesConverter.convert(properties)
        mvn "integration-test $propertiesAsString"
    }

    @Override
    void integrationTestInternalApi(List<ProcessedTestProperty> properties) {
        String parametersAsString = propertiesConverter.convert(properties)
        mvn "integration-test $parametersAsString"
    }

    @Override
    void componentTest(List<ProcessedTestProperty> properties) {
    }

    @Override
    String getTestResultPath() {
        return TEST_RESULT_PATH
    }

    @Override
    String getIntegrationTestResultPath() {
        return getTestResultPath()
    }

    @Override
    String getComponentTestResultPath() {
        return getTestResultPath()
    }

    @Override
    String getPublicApiTestResultPath() {
        return getTestResultPath()
    }

    @Override
    String getInternalApiTestResultPath() {
        return getTestResultPath()
    }

    @Override
    void setCpsScript(CpsScript cpsScript) {
        selectedMaven.setCpsScript(cpsScript)
    }

    @NonCPS
    private String getLoggingOption(LoggingLevel level) {
        switch (level) {
            case LoggingLevel.QUIET:
                return "--quiet"
            case LoggingLevel.DEBUG:
                return "--debug"
            default:
                return null
        }
    }
}
