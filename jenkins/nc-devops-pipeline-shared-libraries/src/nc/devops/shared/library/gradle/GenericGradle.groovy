package nc.devops.shared.library.gradle

import org.jenkinsci.plugins.workflow.cps.CpsScript

class GenericGradle implements Gradle<Void> {

    private static final GRADLE_TYPE_CLASS = 'hudson.plugins.gradle.GradleInstallation'
    private script
    private final String gradleName
    private def callMethod

    GenericGradle(def script, String gradleName, boolean useWrapper) {
        this.script = script
        this.gradleName = gradleName
        this.callMethod = useWrapper ? this.&wrapper : this.&globalTool
    }

    @Override
    Void call(String tasks) {
        callMethod(tasks)
    }

    private void globalTool(String tasks) {
        String gradleHome = script.tool name: gradleName, type: GRADLE_TYPE_CLASS
        script.sh("$gradleHome/bin/gradle $tasks")
    }

    private void wrapper(String tasks) {
        script.sh("./gradlew $tasks")
    }

    @Override
    void setCpsScript(CpsScript script) {
        this.script = script
    }
}
