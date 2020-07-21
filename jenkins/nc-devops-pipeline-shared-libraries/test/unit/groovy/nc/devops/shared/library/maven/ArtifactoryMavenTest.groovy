package nc.devops.shared.library.maven

import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.MavenBuild
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.*
import static org.mockito.BDDMockito.then

@ExtendWith(MockitoExtension)
class ArtifactoryMavenTest {

    @Mock MavenBuild mavenBuild
    ArtifactoryMaven artifactoryMaven

    @BeforeEach
    void setup() {
        artifactoryMaven = new ArtifactoryMaven(artMaven: mavenBuild)
    }

    @Test
    void testArtifactoryMavenCall() {
        artifactoryMaven.call(anyString())
        then(mavenBuild).should().run(anyMap())
    }
}