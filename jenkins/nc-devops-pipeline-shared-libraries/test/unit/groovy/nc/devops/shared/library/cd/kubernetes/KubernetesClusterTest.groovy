package nc.devops.shared.library.cd.kubernetes


import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.Mockito.verify

@ExtendWith(MockitoExtension)
class KubernetesClusterTest {

    @Mock
    PipelineMock cpsScript

    KubernetesCluster cluster

    @BeforeEach
    void setup() {
        cluster = new KubernetesCluster(cpsScript)
    }

    @Test
    void withKubeConfigTest() {

        def kubeConfig = [
                key      : "value",
                namespace: "some-name"
        ]
        Closure someCode = {}
        cluster.setKubectlConfig(kubeConfig)
        cluster.withKubeConfig someCode
        verify(cpsScript).withKubeConfig(kubeConfig, someCode)
    }

    @Test
    void withKubeConfigAndProjectNameTest() {

        def kubeConfig = [
                key      : "value",
                namespace: "some-name"
        ]
        String otherName = "other-name"
        Closure someCode = {}
        cluster.setKubectlConfig(kubeConfig)
        cluster.withKubeConfig(otherName, someCode)

        def modifiedConfig = [
                key      : "value",
                namespace: otherName
        ]

        verify(cpsScript).withKubeConfig(modifiedConfig, someCode)
    }

}
