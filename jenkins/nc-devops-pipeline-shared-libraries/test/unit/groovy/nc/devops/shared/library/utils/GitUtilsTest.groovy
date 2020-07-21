import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class GitUtilsTest {

    private final gitUtils = new gitUtils()

    @ParameterizedTest
    @MethodSource("urlProvider")
    void testParameters(List<String> gitURLs) {
        def gitRepoName = gitUtils.getRepositoryName(gitURLs.get(0))
        assert gitURLs.get(1) == gitRepoName
    }

    private static def urlProvider() {
        return [
                ["git://git.kernel.org/ownername/reponame.git", "reponame"],
                ["https://source.netcompany.com/tfs/Netcompany/STILKOF/_git/stil-devops", "stil-devops"],
                ["ssh://source.netcompany.com:22/tfs/Netcompany/STILKOF/_git/stil-devops", "stil-devops"],
                ["ssh://source.netcompany.com:22/tfs/Netcompany/STILKOF/_git/efteruddannelse.dk", "efteruddannelse.dk"],
                ["https://username@bitbucket.org/teamsinspace/documentation-tests.git", "documentation-tests"],
                ["git@bitbucket.org:teamsinspace/documentation-tests.git", "documentation-tests"]
        ]
    }

    private static Object[][] jobNameProvider() {
        return [
                ["build_nc-devops-hello-world", "nc-devops-hello-world"],
                ["build_nc-devops-hello-world/task%2F41721-expose-prometheus-metrics", "nc-devops-hello-world-task-41721-expose-xxx"],
                ["multibranch_nc-devops-hello-world/task%2F41721-expose-prometheus-metrics", "nc-devops-hello-world-task-41721-expose-xxx"],
                ["local-build_nc-devops-hello-world/task%2F41721-expose-prometheus-metrics", "nc-devops-hello-world-task-41721-expose-xxx"]
        ]
    }

    @Test
    void returnCorrectGitRepoOwner() {
        def gitRepoOwner = gitUtils.getGitRepositoryOwner('https://github.com/test-company/test-repo')
        assert 'test-company' == gitRepoOwner
    }

    @Test
    void returnCorrectPRNumberForBitbucket() {
        def pullRequestNumber = '17'
        assert '17' == gitUtils.getPullRequestNumber(pullRequestNumber, 'refs/pull/35778/merge')
    }

    @Test
    void returnCorrectPRNumberForTFS() {
        def pullRequestNumber = null
        assert '35778' == gitUtils.getPullRequestNumber(pullRequestNumber, 'refs/pull/35778/merge')
    }

    @ParameterizedTest
    @MethodSource("jobNameProvider")
    void returnCorrectProjectName(String jobName, String expectedJobName) {
        assert expectedJobName == gitUtils.prepareProjectName(jobName)
    }


}