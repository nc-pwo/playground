package nc.devops.shared.library.maven

import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

class ArtifactoryMaven implements Maven<BuildInfo> {

    def artMaven

    @Override
    BuildInfo call(String arguments) {
        return runMavenPhase(arguments)
    }

    private BuildInfo runMavenPhase(arguments) {
        def buildInfo = artMaven.run pom: 'pom.xml', goals: arguments
        buildInfo
    }

    @Override
    void setCpsScript(CpsScript script) {
        artMaven.setCpsScript(script)
    }
}