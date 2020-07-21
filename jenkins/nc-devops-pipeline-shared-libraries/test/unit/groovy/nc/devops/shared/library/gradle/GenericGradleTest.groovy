package nc.devops.shared.library.gradle

import nc.devops.shared.library.buildtool.BuildToolPipelineMock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import static org.mockito.ArgumentMatchers.*
import static org.mockito.BDDMockito.then

@ExtendWith(MockitoExtension)
class GenericGradleTest {

    @Mock
    BuildToolPipelineMock pipeline
    GenericGradle genericGradle

    @Test
    void genericGradleCallGlobalTool() {
        genericGradle = new GenericGradle(pipeline, null, false)
        genericGradle.call(anyString())
        then(pipeline).should().tool(anyMap())
    }

    @Test
    void genericGradleCallWrapper() {
        String task = 'clean'
        genericGradle = new GenericGradle(pipeline, null, true)
        genericGradle.call(task)
        then(pipeline).should().sh("./gradlew ${task}")
    }
}