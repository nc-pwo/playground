package nc.devops.shared.library.maven

import org.jenkinsci.plugins.workflow.cps.CpsScript

class GenericMaven implements Maven<Void> {

    private script
    private String mavenName
    private String mavenSettingsConfig

    @Override
    Void call(String arguments) {
        script.withMaven(maven: mavenName, mavenSettingsConfig: mavenSettingsConfig) {
            script.sh "mvn $arguments"
        }
    }

    @Override
    void setCpsScript(CpsScript script) {
        this.script = script
    }
}
