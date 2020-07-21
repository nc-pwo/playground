import nc.devops.shared.library.buildtool.BuildToolType

def call(Closure body) {
    javaBpprPipeline {
        buildToolType = BuildToolType.GRADLE
        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = getDelegate()
        body()
    }
}