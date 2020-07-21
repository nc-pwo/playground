package nc.devops.shared.library.gradle

import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

class ArtifactoryGradle implements Gradle<BuildInfo> {

    private artGradle

    @Override
    BuildInfo call(String tasks) {
        return runGradleTask(tasks)
    }

    private BuildInfo runGradleTask(tasks) {
        def buildInfo = artGradle.run buildFile: 'build.gradle', tasks: tasks
        buildInfo
    }

    @Override
    void setCpsScript(CpsScript script) {
        artGradle.setCpsScript(script)
    }
}