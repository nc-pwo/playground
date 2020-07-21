package nc.devops.shared.library.cd.project.bpprtype.openshift

import hudson.AbortException
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.openshift.SelectorMock
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenshiftBpprWithLockProjectManagementProviderTest {
    @Mock
    private Openshift openshift

    @Mock
    private CpsScript cpsScript

    private DeploymentProviderParameters deploymentProviderParameters
    private OpenshiftBpprWithLockProjectManagementProvider projectManagementProvider
    private final String BASE_PROJECT_NAME = "test-project"
    private final String EXPECTED_BPPR_NAME = "BPPR-1"

    @BeforeEach
    void setup() {
        deploymentProviderParameters = new DeploymentProviderParameters(
                logger: { String message -> println(message) },
                sourceBranch: "refs/pull/60946/merge",
                projectNameSuffix: "bppr",
                projectNameDelimiter: "-",
                deliveryCluster: "test-cluster",
                projectName: BASE_PROJECT_NAME,
                pullRequestId: "60946",
                defaultTimeout: 5,
                timeout: { int timeoutInMins, Closure timeoutBody -> timeoutBody() },
                cpsScript: cpsScript
        )
        when(cpsScript.env).thenReturn([LOCKED_PROJECT_NAME: "BPPR-1"])
        when(openshift.withCluster(anyString(), any())).then({ i -> i.getArgument(1).call() })
        when(openshift.withClusterAndProject(anyString(), anyString(), any())).then({ i -> i.getArgument(2).call() })
        projectManagementProvider = new OpenshiftBpprWithLockProjectManagementProvider([], openshift)
        projectManagementProvider.lockResource(deploymentProviderParameters)
    }

    @Test
    void testLockableResourcesPluginOptionsIsEmpty() {
        assert [label: 'BPPR_POOL', quantity: 1, variable: 'LOCKED_PROJECT_NAME'] == projectManagementProvider.getLockableResourcesPluginOptions()
    }

    @Test
    void testUnlockResourcesDidNotFail() {
        projectManagementProvider.unlockResource()
        verifyNoMoreInteractions(openshift)
    }

    private setUpEnvironmentForSpecificTests() {
        projectManagementProvider.getLockableResourcesPluginOptions()
        projectManagementProvider.lockResource(deploymentProviderParameters)
    }

    @Test
    void testUseProjectWhenProjectExists() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(true))
        projectManagementProvider.useProject(deploymentProviderParameters)
        verify(openshift).withCluster(any(), any())
        verify(openshift).selector("project", EXPECTED_BPPR_NAME)
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testUseProjectWhenProjectDoesNotExistAndNotExceptionIsThrown() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(false))
        projectManagementProvider.useProject(deploymentProviderParameters)
        verify(openshift).withCluster(any(), any())
        verify(openshift).selector("project", EXPECTED_BPPR_NAME)
        verify(openshift).newProject(EXPECTED_BPPR_NAME)
        verify(openshift).raw('adm', 'policy', 'add-scc-to-user', 'anyuid', "system:serviceaccount:$EXPECTED_BPPR_NAME:default")
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testUseProjectWhenProjectDoesNotExistAndAlreadyExistExceptionIsThrown() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(false))
        when(openshift.newProject(EXPECTED_BPPR_NAME)).thenThrow(new AbortException("AlreadyExists"))
        projectManagementProvider.useProject(deploymentProviderParameters)
        verify(openshift).withCluster(any(), any())
        verify(openshift).selector("project", EXPECTED_BPPR_NAME)
        verify(openshift).newProject(EXPECTED_BPPR_NAME)
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testUseProjectWhenProjectDoesNotExistAndUnhandledExceptionIsThrown() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(false))
        when(openshift.newProject(EXPECTED_BPPR_NAME)).thenThrow(new AbortException("Unhandled message"))
        Assertions.assertThrows(AbortException.class) {
            projectManagementProvider.useProject(deploymentProviderParameters)
        }
        verify(openshift).withCluster(any(), any())
        verify(openshift).selector("project", EXPECTED_BPPR_NAME)
        verify(openshift).newProject(EXPECTED_BPPR_NAME)
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeleteProjectWhenProjectNoLongerExists() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(0))
        when(openshift.raw("get", "project", EXPECTED_BPPR_NAME)).thenReturn("get project $EXPECTED_BPPR_NAME")
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(openshift).withClusterAndProject(eq(EXPECTED_BPPR_NAME), eq(deploymentProviderParameters.deliveryCluster), any())
        verify(openshift).selector(eq("project"), eq(EXPECTED_BPPR_NAME))
        verify(openshift).raw(eq("get"), eq("project"), eq(EXPECTED_BPPR_NAME))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeleteProjectWhenProjectIsTerminating() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(1))
        when(openshift.raw("get", "project", EXPECTED_BPPR_NAME)).thenReturn("Terminating get project $EXPECTED_BPPR_NAME")
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(openshift).withClusterAndProject(eq(EXPECTED_BPPR_NAME), eq(deploymentProviderParameters.deliveryCluster), any())
        verify(openshift).selector(eq("project"), eq(EXPECTED_BPPR_NAME))
        verify(openshift).raw(eq("get"), eq("project"), eq(EXPECTED_BPPR_NAME))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeleteProjectWhenProjectExistsAndIsTerminating() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(0))
        when(openshift.raw("get", "project", EXPECTED_BPPR_NAME)).thenReturn("Terminating get project $EXPECTED_BPPR_NAME")
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(openshift).withClusterAndProject(eq(EXPECTED_BPPR_NAME), eq(deploymentProviderParameters.deliveryCluster), any())
        verify(openshift).selector(eq("project"), eq(EXPECTED_BPPR_NAME))
        verify(openshift).raw(eq("get"), eq("project"), eq(EXPECTED_BPPR_NAME))
        verifyNoMoreInteractions(openshift)
    }

    @Test
    void testDeleteProjectWhenProjectExists() {
        setUpEnvironmentForSpecificTests()
        when(openshift.selector(any(), any())).thenReturn(new SelectorMock(1))
        when(openshift.raw("get", "project", EXPECTED_BPPR_NAME)).thenReturn("get project $EXPECTED_BPPR_NAME")
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(openshift).withClusterAndProject(eq(EXPECTED_BPPR_NAME), eq(deploymentProviderParameters.deliveryCluster), any())
        verify(openshift, times(1)).selector(eq("project"), eq(EXPECTED_BPPR_NAME))
        verify(openshift).raw(eq("get"), eq("project"), eq(EXPECTED_BPPR_NAME))
        verify(openshift).raw(eq("delete"), eq("all"), eq("--all"), eq("--timeout=5m"))
        verifyNoMoreInteractions(openshift)
    }
}
