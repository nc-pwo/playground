package nc.devops.shared.library.cd.project.buildtype.kubernetes

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
class KubernetesBuildWithoutLockProjectManagementProviderTest {
    @Mock
    private KubernetesCluster cluster

    @Mock
    KubeCtl kubeCtl

    private DeploymentProviderParameters deploymentProviderParameters
    private KubernetesBuildWithoutLockProjectManagementProvider projectManagementProvider
    private final String PROJECT_NAME = "test-project"
    private final List<String> NAMESPACE_LIST = ["BPPR-1", "BPPR-2", "BPPR-3"]

    @BeforeEach
    void setup() {
        deploymentProviderParameters = new DeploymentProviderParameters(
                logger: { String message -> println(message) },
                sourceBranch: "refs/pull/60946/merge",
                projectNameSuffix: "bppr",
                projectNameDelimiter: "-",
                deliveryCluster: "test-cluster",
                projectName: PROJECT_NAME,
                pullRequestId: "60946",
                defaultTimeout: 5,
                timeout: { int timeoutInMins, Closure timeoutBody -> timeoutBody() }
        )
        projectManagementProvider = new KubernetesBuildWithoutLockProjectManagementProvider([continuousDelivery: [projectName: PROJECT_NAME, kubernetesCluster: []]], cluster, kubeCtl)
        projectManagementProvider.lockResource(deploymentProviderParameters)
        when(cluster.withKubeConfig(any())).then({ i -> i.getArgument(0).call() })
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
    }

    @Test
    void testUseProjectWhenNamespaceExists() {
        when(kubeCtl.getNamespaces()).thenReturn([PROJECT_NAME])
        projectManagementProvider.useProject(deploymentProviderParameters)

        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verifyNoMoreInteractions(cluster)

        verify(kubeCtl).getNamespaces()
        verifyNoMoreInteractions(kubeCtl)
    }

    @Test
    void testUseProjectWhenNamespaceDoesNotExist() {
        when(kubeCtl.getNamespaces()).thenReturn(NAMESPACE_LIST)
        projectManagementProvider.useProject(deploymentProviderParameters)

        verify(cluster).setKubectlConfig(any())
        verify(cluster).withKubeConfig(any())
        verifyNoMoreInteractions(cluster)

        verify(kubeCtl).getNamespaces()
        verify(kubeCtl).createNamespace(PROJECT_NAME)
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
        when(kubeCtl.getNamespaces()).thenReturn([PROJECT_NAME])
        projectManagementProvider.deleteProject(deploymentProviderParameters)

        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(any())
        verifyNoMoreInteractions(cluster)

        verify(kubeCtl).getNamespaces()
        verify(kubeCtl).deleteAllFromNamespace(PROJECT_NAME, 5)
        verifyNoMoreInteractions(kubeCtl)
    }
}
