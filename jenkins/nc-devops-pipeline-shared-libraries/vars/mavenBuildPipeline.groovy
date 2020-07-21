import groovy.transform.Field
import nc.devops.shared.library.buildtool.BuildToolType

@Field static final pollSCMStrategy = new javaBuildPipeline().pollSCMStrategy
def call(Closure body) {
    javaBuildPipeline {
        buildToolType = BuildToolType.MAVEN
        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = getDelegate()
        body()
    }
}