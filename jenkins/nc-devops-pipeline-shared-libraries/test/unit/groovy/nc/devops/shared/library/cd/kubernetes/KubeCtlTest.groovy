package nc.devops.shared.library.cd.kubernetes


import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@ExtendWith(MockitoExtension)
class KubeCtlTest {

    @Mock
    PipelineMock cpsScript

    KubeCtl kubeCtl

    @BeforeEach
    void init() {
        kubeCtl = new KubeCtl(cpsScript)
    }

    private void verifySh(String expectedScript, boolean withOutput) {
        verify(cpsScript).sh(script: "$expectedScript", returnStdout: withOutput)
    }

    @Test
    void getIngressHost() {
        when(cpsScript.sh(anyMap())).thenReturn("some-host")
        assert kubeCtl.getIngressHost("some-name") == "some-host"
        verifySh("kubectl get ingress/some-name -o jsonpath={.spec.rules[0].host}", true)
    }

    @Test
    void deleteAllFromNamespace() {
        kubeCtl.deleteAllFromNamespace("some-name", 10)
        verifySh("kubectl delete --all all --namespace=some-name --timeout=10m", false)
    }

    @Test
    void createNamespace() {
        kubeCtl.createNamespace("some-name")
        verifySh("kubectl create namespace some-name", false)
    }

    @Test
    void deleteNamespace() {
        kubeCtl.deleteNamespace("some-name", 10)
        verifySh("kubectl delete namespace some-name --timeout=10m", false)
    }

    @Test
    void getNamespaces() {
        when(cpsScript.sh(anyMap())).thenReturn("A\nB\nC")
        assert kubeCtl.getNamespaces() == ["A", "B", "C"]
        verifySh("kubectl get namespaces --no-headers -o custom-columns=\":metadata.name\"", true)
    }


}
