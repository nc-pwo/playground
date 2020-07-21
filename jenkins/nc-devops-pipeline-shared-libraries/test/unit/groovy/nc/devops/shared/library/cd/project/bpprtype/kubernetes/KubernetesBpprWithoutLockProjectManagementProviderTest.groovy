package nc.devops.shared.library.cd.project.bpprtype.kubernetes

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster
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
class KubernetesBpprWithoutLockProjectManagementProviderTest {
    @Mock
    private KubernetesCluster cluster
    @Mock
    private KubeCtl kubeCtl

    private DeploymentProviderParameters deploymentProviderParameters
    private KubernetesBpprWithoutLockProjectManagementProvider projectManagementProvider
    private final String BASE_PROJECT_NAME = "test-project"
    private final String EXPECTED_BPPR_NAME = "$BASE_PROJECT_NAME-60946-bppr"
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
                timeout: { int timeoutInMins, Closure timeoutBody -> timeoutBody() }
        )
        projectManagementProvider = new KubernetesBpprWithoutLockProjectManagementProvider([continuousDelivery: [projectName: BASE_PROJECT_NAME, kubernetesCluster: []]], cluster, kubeCtl)
        projectManagementProvider.lockResource(deploymentProviderParameters)
        when(cluster.withKubeConfig(any())).then({ i -> i.getArgument(0).call() })
    }

    @Test
    void testLockResourceProjectName() {
        assert EXPECTED_BPPR_NAME == projectManagementProvider.lockResource(deploymentProviderParameters)
    }

    @Test
    void testLockableResourcesPluginOptionsIsEmpty() {
        assert [:] == projectManagementProvider.getLockableResourcesPluginOptions()
    }

    @Test
    void testUnlockResourcesDidNotFail() {
        projectManagementProvider.unlockResource()
        verify(cluster).setKubectlConfig(anyMap())
        verifyNoMoreInteractions(cluster)
        verifyNoMoreInteractions(kubeCtl)
    }

    @Test
    void testUseProjectWhenNamespaceExists() {
        when(kubeCtl.getNamespaces()).thenReturn([EXPECTED_BPPR_NAME])
        projectManagementProvider.useProject(deploymentProviderParameters)

        verify(kubeCtl).getNamespaces()
        verifyNoMoreInteractions(kubeCtl)

        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verifyNoMoreInteractions(cluster)

    }

    @Test
    void testUseProjectWhenNamespaceDoesNotExist() {
        when(kubeCtl.getNamespaces()).thenReturn(NAMESPACE_LIST)
        projectManagementProvider.useProject(deploymentProviderParameters)

        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verifyNoMoreInteractions(cluster)

        verify(kubeCtl).getNamespaces()
        verify(kubeCtl).createNamespace(EXPECTED_BPPR_NAME)
        verifyNoMoreInteractions(kubeCtl)

    }

    @Test
    void testDeleteProjectWhenNamespaceDoesNotExist() {
        when(kubeCtl.getNamespaces()).thenReturn(NAMESPACE_LIST)
        projectManagementProvider.deleteProject(deploymentProviderParameters)

        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verifyNoMoreInteractions(cluster)

        verify(kubeCtl).getNamespaces()
        verifyNoMoreInteractions(kubeCtl)
    }

    @Test
    void testDeleteProjectWhenNamespaceExists() {
        when(kubeCtl.getNamespaces()).thenReturn([EXPECTED_BPPR_NAME])
        projectManagementProvider.deleteProject(deploymentProviderParameters)
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())

        verify(kubeCtl).getNamespaces()
        verify(kubeCtl).deleteNamespace(EXPECTED_BPPR_NAME, 5)
        verifyNoMoreInteractions(kubeCtl)
    }
}
