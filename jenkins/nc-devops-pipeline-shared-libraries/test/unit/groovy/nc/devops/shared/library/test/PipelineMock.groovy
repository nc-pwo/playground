package nc.devops.shared.library.test

import hudson.model.Build
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.jenkinsci.plugins.workflow.cps.CpsScript

class PipelineMock extends CpsScript {
    private final Build rawBuildMock

    public Map env = [:]

    PipelineMock(Build rawBuildMock) {
        this.rawBuildMock = rawBuildMock
    }

    boolean waitUntil(Closure<Boolean> c) {
        return c.call()
    }

    void echo(String dataToPrint){
        DefaultGroovyMethods.println(dataToPrint)
    }

    def getCurrentBuild() {
        return new Object() {
            Build getRawBuild() {
                return rawBuildMock
            }
        }
    }

    def usernamePassword(Map inputs) {
        inputs
    }

    def string(Map inputs) {
        inputs
    }

    def file(Map inputs) {
        inputs
    }

    def withCredentials(List args, Closure closure) {
        args.each {
            if (!it.variable) {
                env[it.usernameVariable] = 'username'
                env[it.passwordVariable] = 'password'
            } else {
                env[it.variable] = 'actual string'
            }
        }
        closure()
    }

    String commandCalled

    def sh(String command) {
        commandCalled = command
    }

    def sh(Map args) {
        return sh(args.script as String)
    }

    String libraryResource(String path) {
        return getClass().getResourceAsStream(path).text
    }

    def getGitUtils() {
        return new GroovyScriptEngine('vars').loadScriptByName('gitUtils.groovy').newInstance()
    }

    def getGradleBuildPipeline() {
        return new Object() {
            String getPollSCMStrategy() {
                return '* * * * *'
            }
        }
    }

    boolean fileExists(String path) {
        return new File(path).exists()
    }

    String readFile(String path) {
        return new File(path).text
    }

    def withKubeConfig(Map map, Closure c) {
        c.call()
    }

    def readYaml(Map<String, String> attrs) {
        return attrs
    }

    void writeFile(Map<String, String> attrs) {
        new File(attrs['file']).write(attrs['text'])
    }

    @Override
    Object run() {
        return null
    }
}
