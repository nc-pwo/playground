package nc.devops.shared.library.cd.openshift

import hudson.AbortException
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.test.util.ReflectionTestUtils

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenshiftProviderTest {

    @Mock
    private TemplateProcessingTool templateProcessingTool

    @Mock
    private Openshift openshift

    @Mock
    private ProjectManagementProvider projectManagementProvider

    private OpenshiftProvider openshiftProvider
    private DeploymentProviderParameters parameters
    def processedDC = new OpenshiftProcessedModel(
            models: [[metadata: [name: "nc-artifactory"], kind: "DeploymentConfig"], [metadata: [name: "pvc"], kind: "PersistentVolumeClaim"], [metadata: [name: "test-import-image"], kind: "ImageStream", spec: [tags: [name: "id-of-image", from: [name: "[szyb-test]"]]]]],
            applicationName: "nc-artifactory"
    )
    def processedJob = new OpenshiftProcessedModel(
            models: [[metadata: [name: "nc-artifactory-job"], kind: "Job"]],
            applicationName: "nc-artifactory-job"
    )
    List<OpenshiftProcessedModel> processedTemplates = [processedDC, processedJob]
    private final String PROJECT_NAME = "test-project"

    @BeforeEach
    void setup() {
        parameters = new DeploymentProviderParameters(
                logger: { String message -> println(message) },
                sourceBranch: "refs/pull/60946/merge",
                projectNameSuffix: "bppr",
                projectNameDelimiter: "-",
                deliveryCluster: "test-cluster",
                projectName: PROJECT_NAME,
                pullRequestId: "60946",
                defaultTimeout: 5,
                templatePath: 'openshift/bppr.yml',
                gitCommitId: "manually_created",
                bppr: false,
                parallel: { Map m ->
                    m.each { k, v -> println("Calling $k"); v() }
                },
                timeout: { int timeoutInMins, Closure timeoutBody -> timeoutBody() },
                sleep: { sleepInMins -> },
                templateProcessingTool: templateProcessingTool
        )
        when(openshift.withCluster(anyString(), any())).then({ i -> i.getArgument(1).call() })
        when(openshift.withClusterAndProject(anyString(), anyString(), any())).then({ i -> i.getArgument(2).call() })
        openshiftProvider = new OpenshiftProvider(openshift, parameters)
        ReflectionTestUtils.setField(openshiftProvider, "processedTemplates", processedTemplates)
    }

    @Test
    void testDeployApplicationWithoutAbortException() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC).thenReturn(processedJob)
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true))
        openshiftProvider.deployApplication()
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(2)).selector(eq('dc'), any())
        verify(openshift, times(3)).apply(any())
        verify(openshift, times(2)).raw(eq("import-image -n test-project test-import-image:id-of-image --from=szyb-test --confirm"))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeployApplicationDidNotFailWhenRolloutIsInProgress() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.apply(any())).thenThrow(new AbortException("apply returned an error"))
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true)).thenReturn(new SelectorMock(true))
        when(openshift.selector('dc', 'nc-artifactory')).thenReturn(new SelectorMock(new AbortException("already in progress")))
        openshiftProvider.deployApplication()
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(2)).selector(eq('dc'), any())
        verify(openshift, times(3)).apply(any())
        verify(openshift, times(2)).replace(any())
        verify(openshift, times(2)).raw(eq("import-image -n test-project test-import-image:id-of-image --from=szyb-test --confirm"))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeployApplicationFailWhenUnhandledAbortExceptionIsThrown() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.apply(any())).thenThrow(new AbortException("apply returned an error"))
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true)).thenReturn(new SelectorMock(true))
        when(openshift.selector('dc', 'nc-artifactory')).thenReturn(new SelectorMock(new AbortException("unhandled exception")))
        Assertions.assertThrows(AbortException.class) {
            openshiftProvider.deployApplication()
        }
    }

    @Test
    void testDeployApplicationWhenAbortExceptionWithSpecialMessageIsThrownAndReplaceSucceeds() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.apply(any())).thenThrow(new AbortException("apply returned an error"))
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true))
        openshiftProvider.deployApplication()
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(2)).selector(eq('dc'), any())
        verify(openshift, times(3)).apply(any())
        verify(openshift, times(2)).replace(any())
        verify(openshift, times(0)).delete(any())
        verify(openshift, times(0)).create(any())
        verify(openshift, times(2)).raw(eq("import-image -n test-project test-import-image:id-of-image --from=szyb-test --confirm"))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeployApplicationWhenAbortExceptionWithoutSpecialMessageIsThrownAndReplaceSucceeds() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.apply(any())).thenThrow(new AbortException("incorrect message"))
        Assertions.assertThrows(AbortException.class) {
            openshiftProvider.deployApplication()
        }
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(1)).apply(any())
        verify(openshift, times(0)).replace(any())
        verify(openshift, times(0)).delete(any())
        verify(openshift, times(0)).create(any())
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeployApplicationWhenApplyAndReplaceFailed() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.apply(any())).thenThrow(new AbortException("apply returned an error"))
        when(openshift.replace(any())).thenThrow(new AbortException("replace returned an error"))
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true))
        openshiftProvider.deployApplication()
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(2)).selector(eq('dc'), any())
        verify(openshift, times(3)).apply(any())
        verify(openshift, times(2)).replace(any())
        verify(openshift, times(2)).delete(any())
        verify(openshift, times(2)).create(any())
        verify(openshift, times(2)).raw(eq("import-image -n test-project test-import-image:id-of-image --from=szyb-test --confirm"))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeployApplicationWhenApplyAndReplaceFailedWithIncorrectMessage() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.apply(any())).thenThrow(new AbortException("apply returned an error"))
        when(openshift.replace(any())).thenThrow(new AbortException("incorrect message"))
        Assertions.assertThrows(AbortException.class) {
            openshiftProvider.deployApplication()
        }
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(1)).apply(any())
        verify(openshift, times(1)).replace(any())
        verify(openshift, times(0)).delete(any())
        verify(openshift, times(0)).create(any())
        verifyNoMoreInteractions(openshift)
    }


    @Test
    void testVerifyAppAndJobReadiness() {
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true))
                .thenReturn(new SelectorMock(true))
        when(openshift.selector(anyString())).thenReturn(new SelectorMock(true))
        openshiftProvider.verifyReadiness()
        verify(openshift, times(2)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(1)).selector('dc', "nc-artifactory")
        verify(openshift, times(1)).selector('rc', "nc-artifactory-true")
        verify(openshift, times(1)).selector("jobs/nc-artifactory-job")
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testIdleApplication() {
        openshiftProvider.idleApplication()
        verify(openshift, times(1)).withClusterAndProject(eq(parameters.projectName), eq(parameters.deliveryCluster), any())
        verify(openshift, times(1)).idle(eq("nc-artifactory"))
        verify(openshift, times(1)).idle(eq("nc-artifactory-job"))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void prepareIntegrationTestParamsWithoutCluster() {
        def processedProperties = openshiftProvider.processIntegrationTestParamsWithoutCluster([
                [propertyType : 'P',
                 propertyName : 'appUrl',
                 propertyValue: 'localhost',
                ],
                [propertyType : 'D',
                 propertyName : 'prop',
                 propertyValue: 'value',
                ]
        ] as List<TestProperty>)

        assert processedProperties == [
                [type : 'P',
                 name : 'appUrl',
                 value: 'localhost'
                ] as ProcessedTestProperty,
                [type : 'D',
                 name : 'prop',
                 value: 'value'
                ] as ProcessedTestProperty
        ]
    }

    @Test
    void prepareIntegrationTestParams() {
        when(openshift.selector(anyString())).thenReturn(new SelectorMock("host_for_spec_and_routes/routeName"))
        def processedProperties = openshiftProvider.processIntegrationTestParams([
                [propertyType   : 'P',
                 propertyName   : 'appUrl',
                 propertyValue  : 'routeName',
                 appHostForValue: true],
                [propertyType : 'D',
                 propertyName : 'prop',
                 propertyValue: 'value',
                ]
        ] as List<TestProperty>)

        assert processedProperties == [
                [type : 'P',
                 name : 'appUrl',
                 value: 'http://host_for_spec_and_routes/routeName'
                ] as ProcessedTestProperty,
                [type : 'D',
                 name : 'prop',
                 value: 'value'
                ] as ProcessedTestProperty

        ]
    }

    @Test
    void testOnlyRolloutDeployConfig() {
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedDC)
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true))
        openshiftProvider.deployApplication()
        verify(openshift, times(2)).selector(eq('dc'), any())

        when(templateProcessingTool.processTemplate(any())).thenReturn(processedJob)
        when(openshift.selector(anyString(), anyString())).thenReturn(new SelectorMock(true))
        verify(openshift, times(0)).selector(eq('dc'))

    }

}