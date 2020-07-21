package nc.devops.shared.library.cd.project.bpprtype.kubernetes

import hudson.AbortException
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
@MockitoSettings(strictness = Strictness.LENIENT)
class KubernetesBpprWithLockProjectManagementProviderTest {

    @Mock
    private KubernetesCluster cluster
    @Mock
    private KubeCtl kubeCtl

    @Mock
    private CpsScript cpsScript

    private DeploymentProviderParameters deploymentProviderParameters
    private KubernetesBpprWithLockProjectManagementProvider projectManagementProvider
    private final String BASE_PROJECT_NAME = "test-project"
    private final List<String> NAMESPACE_LIST = ["BPPR-1", "BPPR-2", "BPPR-3"]

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
        when(cluster.withKubeConfig(any())).then({ i -> i.getArgument(0).call() })
        projectManagementProvider = new KubernetesBpprWithLockProjectManagementProvider([continuousDelivery: [projectName: BASE_PROJECT_NAME, kubernetesCluster: []]], cluster, kubeCtl)
    }

    private setUpEnvironmentForSpecificTests() {
        projectManagementProvider.getLockableResourcesPluginOptions()
        projectManagementProvider.lockResource(deploymentProviderParameters)
    }

    @Test
    void testGetLockableResourcesReturnsValidMap() {
        assert [label: 'BPPR_POOL', quantity: 1, variable: 'LOCKED_PROJECT_NAME'] == projectManagementProvider.getLockableResourcesPluginOptions()
    }

    @Test
    void testLockResources() {
        projectManagementProvider.getLockableResourcesPluginOptions()
        assert "BPPR-1" == projectManagementProvider.lockResource(deploymentProviderParameters)
    }

    @Test
    void testUseProjectWhenNamespaceExists() {
        setUpEnvironmentForSpecificTests()
        when(kubeCtl.getNamespaces()).thenReturn(NAMESPACE_LIST)
        projectManagementProvider.useProject(deploymentProviderParameters)
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verify(kubeCtl).getNamespaces()
        verifyNoMoreInteractions(kubeCtl)
        verifyNoMoreInteractions(cluster)
    }

    @Test
    void testUseProjectWhenNamespaceDoesNotExist() {
        setUpEnvironmentForSpecificTests()
        when(kubeCtl.getNamespaces()).thenReturn([])
        Assertions.assertThrows(AbortException.class) {
            projectManagementProvider.useProject(deploymentProviderParameters)
        }
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verify(kubeCtl).getNamespaces()
        verifyNoMoreInteractions(kubeCtl)
        verifyNoMoreInteractions(cluster)
    }

    @Test
    void testDeleteProjectWhenNamespaceDoesNotExist() {
        setUpEnvironmentForSpecificTests()
        when(kubeCtl.getNamespaces()).thenReturn([])
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verify(kubeCtl).getNamespaces()
        verifyNoMoreInteractions(kubeCtl)
        verifyNoMoreInteractions(cluster)
    }

    @Test
    void testDeleteProjectWhenNamespaceExists() {
        setUpEnvironmentForSpecificTests()
        when(kubeCtl.getNamespaces()).thenReturn(NAMESPACE_LIST)
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verify(kubeCtl).getNamespaces()
        verify(kubeCtl).deleteAllFromNamespace("BPPR-1", 5 as int)
        verifyNoMoreInteractions(kubeCtl)
        verifyNoMoreInteractions(cluster)
    }
}
