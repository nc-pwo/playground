package nc.devops.shared.library.gradle

import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.GradleBuild
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.*
import static org.mockito.BDDMockito.then

@ExtendWith(MockitoExtension)
class ArtifactoryGradleTest {

    @Mock GradleBuild gradleBuild
    ArtifactoryGradle artifactoryGradle

    @BeforeEach
    void setup() {
        artifactoryGradle = new ArtifactoryGradle(artGradle: gradleBuild)
    }

    @Test
    void testArtifactoryGradleCall() {
        artifactoryGradle.call(anyString())
        then(gradleBuild).should().run(anyMap())
    }
}