package nc.devops.shared.library.seedjob.cron

import groovy.transform.CompileStatic
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@CompileStatic
class CronProviderTest {
    static def pipelineMock
    static CronProvider provider

    @BeforeAll
    static void setUp() {
        pipelineMock = new PipelineMock(null)
        provider = new CronProvider(pipelineMock, 'repoName')
    }

    private String findScmCronExpression(String path) {
        provider.findPollScmCronExpression({ -> this.getClass().getResourceAsStream(path).text })
    }

    @Test
    void returnCronFromPipeline() {
        assert findScmCronExpression("pipelineCron.jenkinsfile") == '* * * * *'
    }

    @Test
    void returnCronFromJenkinsFile() {
        assert findScmCronExpression("jenkinsfileCron.jenkinsfile") == '* * * * *'
    }

    @Test
    void returnNullDueToIncompatibleJenkinsfile() {
        assert findScmCronExpression("random.jenkinsfile") == null
    }

    @Test
    void returnNullBecauseWeDontInvokePipeline() {
        assert findScmCronExpression("noPipeline.jenkinsfile") == null
    }

    @Test
    void returnNullBecausePipelineDoesNotContainPollSCMStrategy() {
        assert findScmCronExpression("pipelineCronDoesNotExist.jenkinsfile") == null
    }

}
