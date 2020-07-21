package nc.devops.shared.library.buildtool

class BuildToolPipelineMock {
    def Artifactory = new Artifactory()

    def tool(def map) {
    }

    def sh(String str) {
    }

    def withMaven(def configs, Closure cl) {
    }

    def echo(String str) {
        println str
    }

    class Artifactory {

        def newGradleBuild() {
            return new GradleBuild()
        }

        def newMavenBuild() {
            return new MavenBuild()
        }

        def server = { String str -> str}
    }

    class GradleBuild {
        def tool = 'Gradle_Name'
        boolean usesPlugin
        boolean useWrapper
    }

    class MavenBuild {
        def tool = 'MAVEN_NAME'
        def deployer = {Map config ->
            return 'config'
        }
        def deployArtifacts
    }
}