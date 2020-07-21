package nc.devops.shared.library.maven

import nc.devops.shared.library.buildtool.BuildToolPipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.*
import static org.mockito.BDDMockito.then

@ExtendWith(MockitoExtension)
class GenericMavenTest {

    @Mock BuildToolPipelineMock pipeline
    GenericMaven genericMaven


    @BeforeEach
    void setup() {
        genericMaven = new GenericMaven(script: pipeline, mavenName: '', mavenSettingsConfig: '')
    }

    @Test
    void testGenericMavenCall() {
        genericMaven.call('phase')
        then(pipeline).should().withMaven(anyMap(), any())
    }
}