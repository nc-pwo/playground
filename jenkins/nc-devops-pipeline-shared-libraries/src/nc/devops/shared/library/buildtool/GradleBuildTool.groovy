package nc.devops.shared.library.buildtool

import com.cloudbees.groovy.cps.NonCPS
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.buildtool.logging.LoggingLevel
import nc.devops.shared.library.gradle.Gradle
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

class GradleBuildTool implements BuildTool {
    protected final String TEST_RESULT_PATH = '**/**build/test-results/**/*.xml'
    protected final String LEGACY_INTEGRATION_TEST_RESULT_PATH = '**/build/test-results/integrationTest/*.xml'
    protected final String COMPONENT_TEST_RESULT_PATH = '**/build/test-results/componentTest/*.xml'
    protected final String PUBLIC_API_TEST_RESULT_PATH = '**/build/test-results/publicApiIntegrationTest/*.xml'
    protected final String INTERNAL_API_TEST_RESULT_PATH = '**/build/test-results/internalApiIntegrationTest/*.xml'
    protected final ArtifactsMetadataRepository<?> binaryRepository
    protected final Gradle<?> selectedGradle
    protected final SonarqubeCodeAnalysisTool sonarqubeCodeAnalysisTool
    protected final TestPropertiesConverter propertiesConverter
    BuildToolParameters parameters
    protected final String loggingOptions

    GradleBuildTool(ArtifactsMetadataRepository<?> binaryRepository, Gradle<?> selectedGradle, BuildToolParameters parameters) {
        this.binaryRepository = binaryRepository
        this.selectedGradle = selectedGradle
        this.parameters = parameters
        this.sonarqubeCodeAnalysisTool = new SonarqubeCodeAnalysisTool()
        this.propertiesConverter = new TestPropertiesConverterImpl()
        this.loggingOptions = getLoggingOption(parameters?.loggingLevel)
    }

    @Override
    void clean() {
        gradle 'clean'
    }

    protected def gradle(String tasks) {
        gradle(tasks, loggingOptions)
    }

    protected def gradle(String tasks, String loggingOptions) {
        String command = loggingOptions == null ? tasks : "$loggingOptions $tasks"
        selectedGradle.call command
    }


    @Override
    void unitTest() {
        gradle 'check -x build'
    }

    @Override
    void buildArtifacts() {
        gradle 'build -x check'
    }

    @Override
    void staticCodeAnalysis(StaticCodeAnalysisParams codeAnalysisParams) {
        gradle("sonarqube ${sonarqubeCodeAnalysisTool.convertParams(codeAnalysisParams)}", getLoggingOption(LoggingLevel.INFO))
    }

    @Override
    void publishArtifacts(List<String> additionalTags) {
        String publishAdditionalTags = additionalTags.join(",")
        def buildInfo = gradle "publish -DbinaryRepoUsername=${parameters.binaryRepoUsername} -DbinaryRepoPassword=${parameters.binaryRepoPassword} -PpushRegistryUsername=${parameters.pushRegistryUsername} -PpushRegistryPassword=${parameters.pushRegistryPassword} -PadditionalTags=${publishAdditionalTags}"
        if(buildInfo instanceof BuildInfo) {
            buildInfo.name = "${parameters.prefixBuildInfoName}-${buildInfo.name}"
        }
        binaryRepository.publishBuildInfo(parameters.serverId, (Object) buildInfo) // FIXME incorrect use of generics
    }

    @Override
    void publishImages(String registryURL, String openshiftProjectName) {
        gradle "pushImage -PpushRegistryUsername=${parameters.pushRegistryUsername} " +
                "-PpushRegistryPassword=${parameters.pushRegistryPassword} -PopenshiftProject=${openshiftProjectName} " +
                "-PpushRegistryURL=${registryURL}"
    }

    @Override
    void integrationTest(List<ProcessedTestProperty> properties) {
        String propertiesAsString = propertiesConverter.convert(properties)
        gradle "integrationTest $propertiesAsString -x buildImage"
    }

    @Override
    void integrationTestPublicApi(List<ProcessedTestProperty> properties) {
        String propertiesAsString = propertiesConverter.convert(properties)
        gradle "publicApiIntegrationTest $propertiesAsString -x buildImage"
    }

    @Override
    void integrationTestInternalApi(List<ProcessedTestProperty> properties) {
        String propertiesAsString = propertiesConverter.convert(properties)
        gradle "internalApiIntegrationTest $propertiesAsString -x buildImage"
    }

    @Override
    void componentTest(List<ProcessedTestProperty> properties) {
        String propertiesAsString = propertiesConverter.convert(properties)
        gradle "componentTest $propertiesAsString"
    }

    @Override
    String getTestResultPath() {
        return TEST_RESULT_PATH
    }

    @Override
    String getIntegrationTestResultPath() {
        return LEGACY_INTEGRATION_TEST_RESULT_PATH
    }

    @Override
    String getComponentTestResultPath() {
        return COMPONENT_TEST_RESULT_PATH
    }

    @Override
    String getPublicApiTestResultPath() {
        return PUBLIC_API_TEST_RESULT_PATH
    }

    @Override
    String getInternalApiTestResultPath() {
        return INTERNAL_API_TEST_RESULT_PATH
    }

    @Override
    void setCpsScript(CpsScript script) {
        selectedGradle.setCpsScript(script)
    }

    @NonCPS
    protected String getLoggingOption(LoggingLevel level) {
        switch (level) {
            case LoggingLevel.QUIET:
                return "--quiet"
            case LoggingLevel.WARN:
                return "--warn"
            case LoggingLevel.INFO:
                return "--info"
            case LoggingLevel.DEBUG:
                return "--debug"
            default:
                return null
        }
    }
}
