package nc.devops.shared.library.cd

import nc.devops.shared.library.cd.kubernetes.KubernetesProvider
import nc.devops.shared.library.cd.kubernetes.KubernetesWithHelmProvider
import nc.devops.shared.library.cd.openshift.OpenshiftProvider
import nc.devops.shared.library.cd.templates.TemplateProcessingToolType
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.assertj.core.api.Assertions.assertThatExceptionOfType

@ExtendWith(MockitoExtension)
class DeploymentProviderFactoryTest {

    @Mock
    PipelineMock cpsScript
    DeploymentProviderFactory factory = new DeploymentProviderFactory()
    DeploymentProviderParameters parameters

    @BeforeEach
    void setup() {
        parameters = new DeploymentProviderParameters(cpsScript: cpsScript)
    }

    private static List<Arguments> illegalStateParameterProvider() {
        [
                Arguments.of(DeploymentProviderType.OPENSHIFT, TemplateProcessingToolType.HELM),
                Arguments.of(DeploymentProviderType.OPENSHIFT, TemplateProcessingToolType.KUBECTL),
                Arguments.of(DeploymentProviderType.KUBERNETES, TemplateProcessingToolType.OC),
                Arguments.of(DeploymentProviderType.KUBERNETES, TemplateProcessingToolType.HELM),
                Arguments.of(DeploymentProviderType.KUBERNETES_WITH_HELM, TemplateProcessingToolType.OC),
                Arguments.of(DeploymentProviderType.KUBERNETES_WITH_HELM, TemplateProcessingToolType.KUBECTL)
        ]
    }

    private static List<Arguments> allowedParameterProvider() {
        [
                Arguments.of(DeploymentProviderType.OPENSHIFT, TemplateProcessingToolType.OC, OpenshiftProvider.class),
                Arguments.of(DeploymentProviderType.KUBERNETES, TemplateProcessingToolType.KUBECTL, KubernetesProvider.class),
                Arguments.of(DeploymentProviderType.KUBERNETES_WITH_HELM, TemplateProcessingToolType.HELM, KubernetesWithHelmProvider.class)
        ]
    }

    @ParameterizedTest
    @MethodSource('illegalStateParameterProvider')
    void illegalStateTest(DeploymentProviderType deploymentProviderType, TemplateProcessingToolType processingToolType) {

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy({ ->
                    factory.createDeploymentProvider(deploymentProviderType, processingToolType, parameters)
                })
    }


    @ParameterizedTest
    @MethodSource('allowedParameterProvider')
    void allowedStateProviderTest(DeploymentProviderType deploymentProviderType, TemplateProcessingToolType processingToolType, Class aClass) {
        DeploymentProvider tool = factory.createDeploymentProvider(deploymentProviderType, processingToolType, parameters)
        assert aClass.isInstance(tool)
    }

}
