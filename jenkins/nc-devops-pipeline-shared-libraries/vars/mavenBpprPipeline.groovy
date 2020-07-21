import nc.devops.shared.library.buildtool.BuildToolType

def call(Closure body) {
    javaBpprPipeline {
        buildToolType = BuildToolType.MAVEN
        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = getDelegate()
        body()
    }
}