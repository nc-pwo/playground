package nc.devops.shared.library.seedjob

import groovy.transform.CompileStatic
import nc.devops.shared.library.seedjob.cron.CronProvider
import nc.devops.shared.library.seedjob.dsl.JobDslExecutor
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
@CompileStatic
class JobCreatorTest {
    private static final String DSL_SCRIPTS_PATH = '/dslScripts'

    @Mock
    JobDslExecutor executor
    @Mock
    CronProvider cronProvider

    JobCreator jobCreator

    @BeforeEach
    void setUp() {
        PipelineMock pipelineMock = new PipelineMock(null)
        jobCreator = new JobCreator(pipelineMock, false,
                DSL_SCRIPTS_PATH,
                getResourcePathOnDisk(this.getClass(), "customdsl.jenkinsfile"),
                { -> executor },
                { a, b -> cronProvider })
    }

    private static List<Arguments> paramsProvider() {
        [
                testCase('create job using bitBucketBPPRJob.groovy built-in template for bppr.jenkinsfile',
                        'https://bitbucket.org/some/repo',
                        'bppr',
                        "${DSL_SCRIPTS_PATH}/bitBucketBPPRJob.groovy"),
                testCase('create job using bitBucketBPPRJob.groovy built-in template for mavenBppr.jenkinsfile',
                        'https://bitbucket.org/some/repo', 'mavenBppr',
                        "${DSL_SCRIPTS_PATH}/bitBucketBPPRJob.groovy"),
                testCase('create job using gitHubBPPRJob.groovy built-in template for mavenBppr.jenkinsfile',
                        'https://github.com/test-company/test-repo',
                        'mavenBppr',
                        "${DSL_SCRIPTS_PATH}/gitHubBPPRJob.groovy", 'test-company'),
                testCase('create job using tfsBPPRJob.groovy built-in template for bppr.jenkinsfile',
                        'testRepo',
                        'bppr',
                        "${DSL_SCRIPTS_PATH}/tfsBPPRJob.groovy"),
                testCase('create job using tfsBPPRJob.groovy built-in template for mavenBppr.jenkinsfile',
                        'testRepo',
                        'mavenBppr',
                        "${DSL_SCRIPTS_PATH}/tfsBPPRJob.groovy"),
                testCase('create job using gitMultibranchPipelineJob.groovy built-in template for multibranch-build.jenkinsfile',
                        'testRepoo',
                        'multibranch-build',
                        "${DSL_SCRIPTS_PATH}/gitMultibranchPipelineJob.groovy"),
                testCase('create job using localModeDisabled.groovy built-in template for build.jenkinsfile',
                        'testRepoo',
                        'build',
                        "${DSL_SCRIPTS_PATH}/localModeDisabled.groovy"),
                testCase('create job using localModeDisabled.groovy built-in template for non-bppr non-build jenkinsfile',
                        'testRepoo',
                        'jenkinsfileCron',
                        "${DSL_SCRIPTS_PATH}/localModeDisabled.groovy"),
                testCase('create job using customdsl.jobdsl for customdsl.jenkinsfile',
                        'testRepo',
                        'customdsl',
                        'customdsl.jobdsl')
        ]
    }

    private static Arguments testCase(String name, String repoUrl, String taskName, String expectedJobDslPath, String expectedRepositoryOwner = null) {
        def jobData = new JobData(
                repository: repoUrl,
                taskName: taskName,
                repositoryName: '.',
                pathToJenkinsfile: '.'
        )
        def expectedTemplateParams = new JobTemplateParameters(jobData, false, expectedRepositoryOwner , '* * * * *')
        return Arguments.of(name, jobData, expectedJobDslPath, expectedTemplateParams)
    }

    private static String getResourcePathOnDisk(Class clazz, String resourceName) {
        Paths.get(clazz.getResource(resourceName).toURI()).toFile().getParent()
    }

    @ParameterizedTest(name = '{0}')
    @MethodSource('paramsProvider')
    void getTextFromScripts(String testName, JobData jobData, String dslFilePath, JobTemplateParameters expectedJobParameters) {
        when(executor.createJob(anyString(), anyMap())).thenReturn(true)
        when(cronProvider.findPollScmCronExpression(any(Closure))).thenReturn('* * * * *')

        jobCreator.create(jobData)
        verify(executor).createJob(eq(getResourceText(dslFilePath)), eq(expectedJobParameters.asMapForJobTemplate()))
        verify(cronProvider).findPollScmCronExpression(any(Closure))
        verifyNoMoreInteractions(executor, cronProvider)
    }

    private String getResourceText(String dslFilePath) {
        this.getClass().getResourceAsStream(dslFilePath).text
    }
}
