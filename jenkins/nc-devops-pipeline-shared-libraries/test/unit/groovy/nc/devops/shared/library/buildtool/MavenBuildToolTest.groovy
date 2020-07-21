package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.buildtool.MavenBuildTool
import nc.devops.shared.library.buildtool.PullRequestCodeAnalysisParams
import nc.devops.shared.library.buildtool.StaticCodeAnalysisParams
import nc.devops.shared.library.buildtool.logging.LoggingLevel
import nc.devops.shared.library.maven.Maven
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.BDDMockito.then
import static org.mockito.Mockito.verifyNoMoreInteractions

@ExtendWith(MockitoExtension)
class MavenBuildToolTest {

    @Mock
    private ArtifactsMetadataRepository artifactsMetadataRepository
    @Mock
    private Maven maven

    private MavenBuildTool buildTool

    private final String TEST_RESULT_PATH = '**/*-reports/*.xml'

    private String branchName = 'branch_name'
    private final String pullRequestNumber = '123456'
    private final String targetBranch = 'target_branch_name'
    private final String repoUrl = 'ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-pipeline-shared-libraries'
    private final StaticCodeAnalysisParams sonarParams = new StaticCodeAnalysisParams(branchName: branchName)
    private final PullRequestCodeAnalysisParams sonarParamsWithPullRequestNumber = new PullRequestCodeAnalysisParams(pullRequestSourceBranchName: branchName, pullRequestNumber: pullRequestNumber, pullRequestBaseBranchName: targetBranch, repoUrl: repoUrl)
    private String registryURL = 'mock_url'
    private String projectName = 'mock_project_name'

    @BeforeEach
    void setup() {
        buildTool = new MavenBuildTool(artifactsMetadataRepository, maven, new BuildToolParameters())
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        if (testInfo.getTags().contains("skipAfterEachMethod")) {
            return;
        }
        verifyNoMoreInteractions(maven, artifactsMetadataRepository)
    }

    @Test
    void cleanCallsMavenClean() {
        buildTool.clean()
        then(maven).should().call('clean')
    }


    @Test
    void unitTestCallMavenTest() {
        buildTool.unitTest()
        then(maven).should().call('test')
    }

    @Test
    void buildArtifactsCallMavenPackage() {
        buildTool.buildArtifacts()
        then(maven).should().call('package -DskipTests')
    }

    @Test
    void staticCodeAnalysisCallMavenSonar() {
        buildTool.staticCodeAnalysis(sonarParams)
        then(maven).should().call("sonar:sonar -P sonar-profile -Dsonar.branch.name=${branchName}")
    }

    @Test
    void staticCodeAnalysisCallMavenSonarQubeWithPullRequestNumber() {
        buildTool.staticCodeAnalysis(sonarParamsWithPullRequestNumber)
        then(maven).should().call("sonar:sonar -P sonar-profile -Dsonar.pullrequest.branch=${branchName} -Dsonar.pullrequest.key=${pullRequestNumber} -Dsonar.pullrequest.base=${targetBranch} -Dsonar.pullrequest.provider=vsts -Dsonar.pullrequest.vsts.instanceUrl=https://source.netcompany.com/tfs/Netcompany -Dsonar.pullrequest.vsts.project=NCCGV001 -Dsonar.pullrequest.vsts.repository=nc-devops-pipeline-shared-libraries")
    }

    @Test
    @Tag("skipAfterEachMethod")
    void publishArtifactsCallMavenDeploy() {
        buildTool.publishArtifacts()
        then(maven).should().call('deploy -DskipTests -Dregistry.username=null -Dregistry.password=null')
    }

    @Test
    void publishImageCallMavenPushImage() {
        buildTool.publishImages(registryURL, projectName)
        then(maven).should().call("package -DskipTests docker:push -Dregistry.url=${registryURL} " +
                "-DopenshitProject=${projectName} -Dregistry.username=null -Dregistry.password=null")
    }

    @Test
    void integrationTestPublicCallMavenIntegrationTest() {
        buildTool.integrationTestPublicApi([])
        then(maven).should().call('integration-test ')
    }

    @Test
    void integrationTestInternalCallMavenIntegrationTest() {
        buildTool.integrationTestPublicApi([])
        then(maven).should().call('integration-test ')
    }

    @Test
    void getTestResultPathsForGradle() {
        assert TEST_RESULT_PATH == buildTool.getTestResultPath()
        assert TEST_RESULT_PATH == buildTool.getComponentTestResultPath()
        assert TEST_RESULT_PATH == buildTool.getPublicApiTestResultPath()
        assert TEST_RESULT_PATH == buildTool.getInternalApiTestResultPath()
    }

    @ParameterizedTest
    @MethodSource("loggingLevelParams")
    void loggingLevelInfo(LoggingLevel level, String loggingArg) {
        buildTool = new MavenBuildTool(artifactsMetadataRepository, maven, new BuildToolParameters(loggingLevel: level))

        buildTool.clean()
        String cmd = loggingArg == null ? "clean" : "$loggingArg clean"
        then(maven).should().call(cmd)
    }

    private static Object[][] loggingLevelParams() {
        return [
                [LoggingLevel.DEFAULT, null],
                [LoggingLevel.INFO, null],
                [LoggingLevel.DEBUG, "--debug"],
                [LoggingLevel.QUIET, "--quiet"],
                [LoggingLevel.WARN, null]
        ]
    }

}