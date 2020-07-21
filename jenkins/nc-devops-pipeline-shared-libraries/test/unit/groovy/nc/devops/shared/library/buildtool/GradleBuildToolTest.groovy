package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.buildtool.GradleBuildTool
import nc.devops.shared.library.buildtool.PullRequestCodeAnalysisParams
import nc.devops.shared.library.buildtool.StaticCodeAnalysisParams
import nc.devops.shared.library.buildtool.logging.LoggingLevel
import nc.devops.shared.library.gradle.Gradle
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.BDDMockito.then
import static org.mockito.Mockito.verifyNoMoreInteractions

@ExtendWith(MockitoExtension)
class GradleBuildToolTest {

    @Mock
    private ArtifactsMetadataRepository artifactsMetadataRepository
    @Mock
    private Gradle gradle

    private final String TEST_RESULT_PATH = '**/**build/test-results/**/*.xml'
    private final String COMPONENT_RESULT_PATH = '**/build/test-results/componentTest/*.xml'
    private final String PUBLIC_RESULT_PATH = '**/build/test-results/publicApiIntegrationTest/*.xml'
    private final String INTERNAL_RESULT_PATH = '**/build/test-results/internalApiIntegrationTest/*.xml'

    private GradleBuildTool buildTool

    private final String branchName = 'branch_name'
    private final String pullRequestNumber = '123456'
    private final String targetBranch = 'target_branch_name'
    private final String repoUrl = 'ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-hello-world'
    private final StaticCodeAnalysisParams sonarParams = new StaticCodeAnalysisParams(branchName: branchName)
    private final PullRequestCodeAnalysisParams sonarParamsWithPullRequestNumber = new PullRequestCodeAnalysisParams(pullRequestSourceBranchName: branchName, pullRequestNumber: pullRequestNumber, pullRequestBaseBranchName: targetBranch, repoUrl: repoUrl)
    private final String registryURL = 'mock_url'
    private final String projectName = 'mock_project_name'

    @BeforeEach
    void setup() {
        buildTool = new GradleBuildTool(artifactsMetadataRepository, gradle, new BuildToolParameters())
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        if (testInfo.getTags().contains("skipAfterEachMethod")) {
            return
        }
        verifyNoMoreInteractions(gradle, artifactsMetadataRepository)
    }

    @Test
    void cleanCallsGradleClean() {
        buildTool.clean()
        then(gradle).should().call('clean')
    }

    @Test
    void unitTestCallGradleTest() {
        buildTool.unitTest()
        then(gradle).should().call('check -x build')
    }

    @Test
    void buildArtifactsCallGradleBuild() {
        buildTool.buildArtifacts()
        then(gradle).should().call('build -x check')
    }

    @Test
    void staticCodeAnalysisCallGradleSonarQube() {
        buildTool.staticCodeAnalysis(sonarParams)
        then(gradle).should().call("--info sonarqube -Dsonar.branch.name=${branchName}")
    }

    @Test
    void staticCodeAnalysisCallGradleSonarQubeWithPullRequestNumber() {
        buildTool.staticCodeAnalysis(sonarParamsWithPullRequestNumber)
        then(gradle).should().call("--info sonarqube -Dsonar.pullrequest.branch=${branchName} -Dsonar.pullrequest.key=${pullRequestNumber} -Dsonar.pullrequest.base=${targetBranch} -Dsonar.pullrequest.provider=vsts -Dsonar.pullrequest.vsts.instanceUrl=https://source.netcompany.com/tfs/Netcompany -Dsonar.pullrequest.vsts.project=NCCGV001 -Dsonar.pullrequest.vsts.repository=nc-devops-hello-world")
    }

    @Test
    @Tag("skipAfterEachMethod")
    void publishArtifactsCallGradlePublish() {
        List<String> additionalTags = ["test1"]
        buildTool.publishArtifacts(additionalTags)
        String publishAdditionalTags = additionalTags.join(",")
        then(gradle).should().call("publish -DbinaryRepoUsername=null -DbinaryRepoPassword=null -PpushRegistryUsername=null -PpushRegistryPassword=null -PadditionalTags=$publishAdditionalTags")
    }

    @Test
    void publishImageCallGradlePushImage() {
        buildTool.publishImages(registryURL, projectName)
        then(gradle).should().call("pushImage -PpushRegistryUsername=null -PpushRegistryPassword=null " +
                "-PopenshiftProject=${projectName} -PpushRegistryURL=${registryURL}")
    }

    @Test
    void integrationTestPublicApiCallGradleIntegrationTest() {
        buildTool.integrationTestPublicApi([])
        then(gradle).should().call('publicApiIntegrationTest  -x buildImage')
    }

    @Test
    void integrationTestInternalCallGradleIntegrationTest() {
        buildTool.integrationTestInternalApi([])
        then(gradle).should().call('internalApiIntegrationTest  -x buildImage')
    }

    @Test
    void getTestResultPathsForGradle() {
        assert TEST_RESULT_PATH == buildTool.getTestResultPath()
        assert COMPONENT_RESULT_PATH == buildTool.getComponentTestResultPath()
        assert PUBLIC_RESULT_PATH == buildTool.getPublicApiTestResultPath()
        assert INTERNAL_RESULT_PATH == buildTool.getInternalApiTestResultPath()
    }

    @ParameterizedTest
    @MethodSource("loggingLevelParams")
    void loggingLevelInfo(LoggingLevel level, String loggingArg) {
        buildTool = new GradleBuildTool(artifactsMetadataRepository, gradle, new BuildToolParameters(loggingLevel: level))

        buildTool.clean()
        String cmd = loggingArg == null ? "clean" : "$loggingArg clean"
        then(gradle).should().call(cmd)

        buildTool.staticCodeAnalysis(sonarParams)
        then(gradle).should().call("--info sonarqube -Dsonar.branch.name=${branchName}")
    }

    private static Object[][] loggingLevelParams() {
        return [
                [LoggingLevel.DEFAULT, null],
                [LoggingLevel.INFO, "--info"],
                [LoggingLevel.DEBUG, "--debug"],
                [LoggingLevel.QUIET, "--quiet"],
                [LoggingLevel.WARN, "--warn"]
        ]
    }


}
