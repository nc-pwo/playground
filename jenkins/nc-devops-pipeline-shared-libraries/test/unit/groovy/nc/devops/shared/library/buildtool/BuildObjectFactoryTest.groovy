package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.gradle.ArtifactoryGradle
import nc.devops.shared.library.gradle.GenericGradle
import nc.devops.shared.library.maven.ArtifactoryMaven
import nc.devops.shared.library.maven.GenericMaven
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BuildObjectFactoryTest {

    BuildToolPipelineMock pipeline = new BuildToolPipelineMock()

    @ParameterizedTest
    @MethodSource("buildObjectConfigs")
    void createBuildObjectBaseOnRepoTypeAndBuildToolType(RepoType repoType, BuildToolType buildToolType, Class expectedBuildObjectClass, Boolean useWrapper) {
        BuildObject buildObject = new BuildObjectFactory(pipeline).createBuildObject(repoType, buildToolType, 'Default', new BuildToolParameters(useWrapper: useWrapper))
        assert buildObject.class == expectedBuildObjectClass
    }

    @ParameterizedTest
    @MethodSource("buildObjectConfigs")
    void createBuildObjectWithoutName(RepoType repoType, BuildToolType buildToolType,
                                      Class expectedBuildObjectClass, Boolean useWrapper) {
        BuildObject buildObject = new BuildObjectFactory(pipeline).createBuildObject(repoType, buildToolType, null,
                new BuildToolParameters(useWrapper: useWrapper))
        assert buildObject.class == expectedBuildObjectClass
    }

    @Test
    void createBuildObjectWithCustomBuildToolType() {
        def buildObject = new BuildObjectFactory(pipeline).createBuildObject(null, BuildToolType.CUSTOM, null, null)
        assert buildObject == null
    }

    private static Object[][] buildObjectConfigs() {
        return [
                [
                        RepoType.GENERIC,
                        BuildToolType.GRADLE,
                        GenericGradle,
                        false
                ],
                [
                        RepoType.GENERIC,
                        BuildToolType.GRADLE,
                        GenericGradle,
                        true
                ],
                [
                        RepoType.ARTIFACTORY,
                        BuildToolType.GRADLE,
                        ArtifactoryGradle,
                        true
                ],
                [
                        RepoType.ARTIFACTORY,
                        BuildToolType.GRADLE,
                        ArtifactoryGradle,
                        false
                ],
                [
                        RepoType.GENERIC,
                        BuildToolType.MAVEN,
                        GenericMaven,
                        true
                ],
                [
                        RepoType.ARTIFACTORY,
                        BuildToolType.MAVEN,
                        ArtifactoryMaven,
                        true
                ]
        ]
    }

}