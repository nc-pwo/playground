import groovy.transform.Field
import nc.devops.shared.library.buildtool.BuildToolType

@Field static final pollSCMStrategy = new javaBuildPipeline().pollSCMStrategy
def call(Closure body) {
    javaBuildPipeline {
        buildToolType = BuildToolType.GRADLE
        // FIXME we assume only one GRADLE tool type for now, but I can image multiple different building strategies using gradle
        // ie with different tasks mapped to different stages, etc. However, this should not be implemented now
        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = getDelegate()
        body()
    }
}