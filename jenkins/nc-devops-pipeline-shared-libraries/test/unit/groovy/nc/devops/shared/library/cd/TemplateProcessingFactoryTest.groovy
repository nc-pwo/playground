package nc.devops.shared.library.cd

import nc.devops.shared.library.cd.kubernetes.Helm
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.templates.TemplateProcessingFactory
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.cd.templates.TemplateProcessingToolType
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class TemplateProcessingFactoryTest {

    @Mock
    PipelineMock cpsScript
    TemplateProcessingFactory factory = new TemplateProcessingFactory()

    private static List<Arguments> parameterProvider() {
        [
                Arguments.of(TemplateProcessingToolType.HELM, Helm.class),
                Arguments.of(TemplateProcessingToolType.KUBECTL, KubeCtl.class),
                Arguments.of(TemplateProcessingToolType.OC, Openshift.class)
        ]
    }

    @ParameterizedTest
    @MethodSource('parameterProvider')
    void returnTypeTest(TemplateProcessingToolType toolType, Class aClass) {
        TemplateProcessingTool tool = factory.createTemplateProcessingTool(toolType, cpsScript)
        assert aClass.isInstance(tool)
    }

}
