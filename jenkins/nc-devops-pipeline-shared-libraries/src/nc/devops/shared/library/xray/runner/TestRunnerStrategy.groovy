package nc.devops.shared.library.xray.runner

interface TestRunnerStrategy {
    def importExecutionResults(Map vars)
}
