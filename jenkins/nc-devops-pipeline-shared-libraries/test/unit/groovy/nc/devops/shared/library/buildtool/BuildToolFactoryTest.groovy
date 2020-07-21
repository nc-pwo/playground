package nc.devops.shared.library.buildtool

import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BuildToolFactoryTest {

    @ParameterizedTest
    @MethodSource("buildToolConfigs")
    void createBuildToolBaseOnBuildToolType(BuildToolType buildToolType, BuildToolParameters buildToolParameters, Class<BuildTool> expectedBuildToolClass) {
        ArtifactsMetadataRepository repository = null
        def selectedBuildObject = null

        BuildToolFactory buildToolFactory = new BuildToolFactory()
        BuildTool buildTool = buildToolFactory.createBuildTool(buildToolType, selectedBuildObject, repository, buildToolParameters)

        assert buildTool.class == expectedBuildToolClass
    }

    @Test
    void createCustomBuildToolAndThrowException() {
        BuildToolFactory buildToolFactory = new BuildToolFactory()

        Assertions.assertThrows(IllegalArgumentException.class, { ->
                buildToolFactory.createBuildTool(BuildToolType.CUSTOM, null, null,
                        new BuildToolParameters(buildToolCustomClass: 'invalid_custom_class_name'))
            }
        ).message.equals("Invalid build tool custom class provided: invalid_custom_class_name")
    }

    private static Object[][] buildToolConfigs() {
        return [
                [BuildToolType.GRADLE, null, GradleBuildTool],
                [BuildToolType.MAVEN, null, MavenBuildTool],
                [BuildToolType.CUSTOM, new BuildToolParameters(buildToolCustomClass: CustomBuildToolMock.name), CustomBuildToolMock]
        ]
    }
}